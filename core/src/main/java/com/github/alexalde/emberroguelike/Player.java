package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2; // Neu importiert!

import java.util.EnumMap;
import java.util.List;

public class Player {
    // Wir ersetzen die losen floats für x und y durch LibGDX Vektoren
    private Vector2 position;
    private Vector2 direction; // Speichert die reine Richtung (-1, 0, 1)

    private float speed;

    // Alle Animations-Frames haben dieselbe feste Pixelgröße - es gibt keine einzelne "die"
    // Textur mehr, von der man Breite/Höhe abfragen könnte (siehe animationTextures unten).
    // Bewusst größer als TILE_SIZE (32, siehe DualGridTerrainRenderer) - Sprite- und
    // Kachelgröße sind vollständig entkoppelt (kein Codepfad verknüpft beide).
    private static final int SPRITE_WIDTH = 48;
    private static final int SPRITE_HEIGHT = 48;

    // Eigene Texturen (Platzhalter, laufzeit-generiert) - werden in dispose() aufgeräumt
    private Texture[] animationTextures;
    // Echte, von Aseprite exportierte Kunst (siehe Quest 7.10) - Tags, die darin noch nicht
    // existieren, liefern null, dann bleibt der Pixmap-Platzhalter für diesen Zustand aktiv
    // (siehe Konstruktor). So kann Kunst nach und nach eintrudeln, ohne dass der Rest wartet.
    private AsepriteSpriteSheet playerSpriteSheet;
    private EnumMap<AnimationState, Animation> animations;
    private AnimationState currentAnimationState;
    // Frei laufender Zeit-Akkumulator für looping Animationen (Idle/Walk) - wird NIE
    // zurückgesetzt, siehe getAnimationProgress()
    private float animationTime;
    private static final float WALK_ANIMATION_DURATION = 0.3f;
    // Bleibt bei reiner Vertikalbewegung/Stillstand erhalten (siehe updateFacing()) - kein Reset
    // pro Frame wie bei "direction", sonst würde der Charakter beim Stehenbleiben "zurückschauen"
    private boolean facingLeft;
    // Analog zu facingLeft, aber für IDLE/WALK nach Norden vs. Süden - deckt zusammen mit der
    // horizontalen Spiegelung alle 8 Bewegungsrichtungen mit nur 2 gezeichneten Varianten ab
    // (Süden inkl. Ost/West/Südost/Südwest, Norden inkl. Nordost/Nordwest)
    private boolean facingUp;
    private Animation idleUpAnimation;
    private Animation walkUpAnimation;
    // Countdown-Timer wie bei Sword.attackVisualTimeRemaining - werden in takeDamage() gesetzt,
    // ticken in updateAnimation() (nicht update()!) herunter, siehe dortiger Kommentar
    private float hurtTimeRemaining;
    private float deathTimeRemaining;
    private static final float HURT_ANIMATION_DURATION = 0.2f;
    private static final float DEATH_ANIMATION_DURATION = 0.6f;

    private enum PlayerState{
        NORMAL,
        DASHING
    };
    private PlayerState state;
    private Vector2 dashDirection;
    private float dashTimeRemaining;
    private float dashCooldownRemaining;

    private static final float DASH_DISTANCE = 150f;
    private static final float DASH_DURATION = 0.15f;
    private static final float DASH_COOLDOWN = 0.5f;
    private static final float DASH_SPEED = DASH_DISTANCE / DASH_DURATION;

    private Sword sword;
    // Bow testweise auf der rechten Maustaste - echtes Waffen-Slot-System (Wechsel zwischen
    // Waffen) kommt erst später, siehe GDD 2 / ROADMAP Quest 3
    private Bow bow;
    private Vector2 aimDirection;

    private float health;
    private float maxHealth;

    public Player(float startX, float startY) {
        this.position = new Vector2(startX, startY);
        this.direction = new Vector2(0, 0);
        this.speed = 300f;

        // Platzhalter-Frames: Idle 1, Walk 2, Attack 2, Hurt 1, Death 3, plus Idle-Norden 1 und
        // Walk-Norden 2 (unterschiedlich hell/farbig, damit der Frame-Wechsel sichtbar ist) -
        // echte Pixelart ersetzt das später (Quest 7.10). Death durchläuft bewusst 3 statt 2
        // Frames (Treffer -> Fallen -> Ruhen), auch als reiner Platzhalter schon klar lesbar.
        this.animationTextures = new Texture[] {
            createFrameTexture(new Color(0.2f, 0.4f, 0.9f, 1f)),
            createFrameTexture(new Color(0.2f, 0.4f, 0.9f, 1f)),
            createFrameTexture(new Color(0.5f, 0.7f, 1f, 1f)),
            createFrameTexture(new Color(0.9f, 0.7f, 0.1f, 1f)),
            createFrameTexture(new Color(1f, 0.9f, 0.3f, 1f)),
            createFrameTexture(new Color(1f, 0.2f, 0.2f, 1f)),
            createFrameTexture(new Color(0.8f, 0.1f, 0.1f, 1f)),
            createFrameTexture(new Color(0.5f, 0.15f, 0.15f, 1f)),
            createFrameTexture(new Color(0.25f, 0.25f, 0.25f, 1f)),
            createFrameTexture(new Color(0.3f, 0.1f, 0.6f, 1f)),
            createFrameTexture(new Color(0.3f, 0.1f, 0.6f, 1f)),
            createFrameTexture(new Color(0.6f, 0.4f, 0.9f, 1f))
        };
        // IDLE/WALK sind jetzt echtzeit-basiert (siehe Animation.getFrameByElapsedTime()), auch
        // als Platzhalter - deshalb brauchen sie eine Frame-Dauern-Liste. Bei 1 Frame (Idle) ist
        // der genaue Wert egal (Loop landet ohnehin sofort wieder auf demselben Frame), bei Walk
        // ergeben 2 gleich lange Frames dieselbe Gesamtdauer wie bisher WALK_ANIMATION_DURATION.
        Animation placeholderIdle = new Animation(new TextureRegion[] {
            new TextureRegion(animationTextures[0])
        }, new float[] { 1f });
        Animation placeholderWalk = new Animation(new TextureRegion[] {
            new TextureRegion(animationTextures[1]),
            new TextureRegion(animationTextures[2])
        }, new float[] { WALK_ANIMATION_DURATION / 2f, WALK_ANIMATION_DURATION / 2f });
        Animation placeholderIdleUp = new Animation(new TextureRegion[] {
            new TextureRegion(animationTextures[9])
        }, new float[] { 1f });
        Animation placeholderWalkUp = new Animation(new TextureRegion[] {
            new TextureRegion(animationTextures[10]),
            new TextureRegion(animationTextures[11])
        }, new float[] { WALK_ANIMATION_DURATION / 2f, WALK_ANIMATION_DURATION / 2f });

        // ATTACK/HURT/DEATH bleiben progress-basiert (siehe Animation.getFrame()), da an
        // Gameplay-Timer gekoppelt - keine Frame-Dauern-Liste nötig
        Animation placeholderAttack = new Animation(new TextureRegion[] {
            new TextureRegion(animationTextures[3]),
            new TextureRegion(animationTextures[4])
        });
        Animation placeholderHurt = new Animation(new TextureRegion[] {
            new TextureRegion(animationTextures[5])
        });
        Animation placeholderDeath = new Animation(new TextureRegion[] {
            new TextureRegion(animationTextures[6]),
            new TextureRegion(animationTextures[7]),
            new TextureRegion(animationTextures[8])
        });

        this.playerSpriteSheet = new AsepriteSpriteSheet("player_spritesheet.png", "player_spritesheet.json");

        this.animations = new EnumMap<>(AnimationState.class);
        animations.put(AnimationState.IDLE, orPlaceholder(playerSpriteSheet.getAnimation("Idle-South"), placeholderIdle));
        animations.put(AnimationState.WALK, orPlaceholder(playerSpriteSheet.getAnimation("Walk-South"), placeholderWalk));
        animations.put(AnimationState.ATTACK, orPlaceholder(playerSpriteSheet.getAnimation("Attack"), placeholderAttack));
        animations.put(AnimationState.HURT, orPlaceholder(playerSpriteSheet.getAnimation("Hurt"), placeholderHurt));
        animations.put(AnimationState.DEATH, orPlaceholder(playerSpriteSheet.getAnimation("Death"), placeholderDeath));
        this.idleUpAnimation = orPlaceholder(playerSpriteSheet.getAnimation("Idle-North"), placeholderIdleUp);
        this.walkUpAnimation = orPlaceholder(playerSpriteSheet.getAnimation("Walk-North"), placeholderWalkUp);

        this.currentAnimationState = AnimationState.IDLE;
        this.animationTime = 0f;

        this.state = PlayerState.NORMAL;
        this.sword = new Sword();
        this.bow = new Bow();
        this.aimDirection = new Vector2(1, 0);
        this.maxHealth = 100f;
        this.health = maxHealth;
    }

    // "mouseWorldPosition" kommt bereits fertig umgerechnet von Main (Viewport-aware unproject).
    // "roomWidth"/"roomHeight" sind die PIXEL-Maße des AKTUELLEN Raums (nicht der festen Viewport-
    // Größe) - Räume können größer als der Bildschirm sein, siehe updateCamera() in Main
    public void update(float deltaTime, List<Enemy> targets, Vector2 mouseWorldPosition, float roomWidth, float roomHeight) {
        sword.update(deltaTime, targets);
        bow.update(deltaTime, targets);

        if (state == PlayerState.DASHING) {
            // Während des Dashs: nur mit der eingefrorenen Richtung bewegen, kein Input lesen
            position.x += dashDirection.x * DASH_SPEED * deltaTime;
            position.y += dashDirection.y * DASH_SPEED * deltaTime;

            dashTimeRemaining -= deltaTime;
            if (dashTimeRemaining <= 0) {
                state = PlayerState.NORMAL;
                dashCooldownRemaining = DASH_COOLDOWN;
            }
        } else {
            // Jeden Frame die Richtung zurücksetzen
            direction.set(0, 0);

            // Richtung basierend auf Input bestimmen
            if (Gdx.input.isKeyPressed(GameSettings.moveUpKeyPrimary) || Gdx.input.isKeyPressed(GameSettings.moveUpKeySecondary)) {
                direction.y += 1;
            }
            if (Gdx.input.isKeyPressed(GameSettings.moveDownKeyPrimary) || Gdx.input.isKeyPressed(GameSettings.moveDownKeySecondary)) {
                direction.y -= 1;
            }
            if (Gdx.input.isKeyPressed(GameSettings.moveLeftKeyPrimary) || Gdx.input.isKeyPressed(GameSettings.moveLeftKeySecondary)) {
                direction.x -= 1;
            }
            if (Gdx.input.isKeyPressed(GameSettings.moveRightKeyPrimary) || Gdx.input.isKeyPressed(GameSettings.moveRightKeySecondary)) {
                direction.x += 1;
            }

            // Normalisieren
            // nor() macht die Länge des Vektors zu 1, wenn er nicht 0 ist (wichtig, sonst Crash durch Division durch 0)
            if (direction.len() > 0) {
                direction.nor();
            }

            // Bewegung auf die Position anrechnen
            // position = position + direction * speed * deltaTime
            position.x += direction.x * speed * deltaTime;
            position.y += direction.y * speed * deltaTime;

            // Cooldown läuft nur ab, während wir NICHT dashen
            if (dashCooldownRemaining > 0) {
                dashCooldownRemaining -= deltaTime;
            }

            // Dash auslösen: frisch gedrückt, kein Cooldown mehr, und wir bewegen uns überhaupt
            if (Gdx.input.isKeyJustPressed(GameSettings.dashKey)
                    && dashCooldownRemaining <= 0
                    && direction.len() > 0) {
                state = PlayerState.DASHING;
                dashDirection = direction.cpy();
                dashTimeRemaining = DASH_DURATION;
            }
        }

        // Raumbegrenzung (Clamping) - gegen die tatsächliche Größe des AKTUELLEN Raums, nicht
        // gegen die feste Viewport-Größe (Räume können größer als der Bildschirm sein)
        // Grenzen für X (0 bis Raumbreite minus Bildbreite)
        if (position.x < 0) {
            position.x = 0;
        } else if (position.x > roomWidth - SPRITE_WIDTH) {
            position.x = roomWidth - SPRITE_WIDTH;
        }

        // Grenzen für Y (0 bis Raumhöhe minus Bildhöhe)
        if (position.y < 0) {
            position.y = 0;
        } else if (position.y > roomHeight - SPRITE_HEIGHT) {
            position.y = roomHeight - SPRITE_HEIGHT;
        }

        Vector2 center = getCenter();
        Vector2 targetDirection = mouseWorldPosition.cpy().sub(center);

        if (targetDirection.len() > 0) {
            targetDirection.nor();

            // Bei autoSwing zählt gehalten, sonst nur der einzelne Klick-Moment
            boolean meleeInputActive = GameSettings.autoSwing
                    ? Gdx.input.isButtonPressed(GameSettings.meleeAttackButton)
                    : Gdx.input.isButtonJustPressed(GameSettings.meleeAttackButton);
            if (meleeInputActive && sword.tryAttack(center, targetDirection, targets)) {
                aimDirection = targetDirection;
            }

            boolean rangedInputActive = GameSettings.autoSwing
                    ? Gdx.input.isButtonPressed(GameSettings.rangedAttackButton)
                    : Gdx.input.isButtonJustPressed(GameSettings.rangedAttackButton);
            if (rangedInputActive && bow.tryAttack(center, targetDirection, targets)) {
                aimDirection = targetDirection;
            }
        }
    }

    // Wird vom Schadenssystem abgefragt, bevor Schaden angewendet wird
    public boolean isInvincible() {
        return state == PlayerState.DASHING;
    }

    public void takeDamage(float amount) {
        if (isInvincible()) {
            return;
        }

        hurtTimeRemaining = HURT_ANIMATION_DURATION;

        if (health - amount > 0) {
            health -= amount;
            if (DebugSettings.logDamage) {
                System.out.println("Schaden erlitten! Schaden: " + amount + ", verbleibendes Leben: " + health);
            }
        } else {
            health = 0;
            deathTimeRemaining = DEATH_ANIMATION_DURATION;
            if (DebugSettings.logDamage) {
                System.out.println("Tödlicher Schaden erlitten!");
            }
        }
    }

    public boolean isAlive() {
        return health > 0;
    }

    public float getHealth() {
        return health;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public Vector2 getCenter() {
        return new Vector2(position.x + SPRITE_WIDTH / 2f, position.y + SPRITE_HEIGHT / 2f);
    }

    // Gegenstück zu getCenter() - für Raumwechsel, wo der Spieler an einer Zielposition
    // (Zentrum, nicht Ecke) auftauchen soll
    public void setCenter(Vector2 center) {
        this.position.set(center.x - SPRITE_WIDTH / 2f, center.y - SPRITE_HEIGHT / 2f);
    }

    // Läuft UNABHÄNGIG von update() jeden Frame, auch wenn Main gerade wegen Game Over den
    // Aufruf von update() aussetzt - sonst würde die Death-Animation beim Sterben sofort auf
    // Frame 0 einfrieren statt durchzulaufen (siehe Main.render())
    public void updateAnimation(float deltaTime) {
        animationTime += deltaTime;
        updateFacing();

        if (hurtTimeRemaining > 0) {
            hurtTimeRemaining -= deltaTime;
        }
        if (deathTimeRemaining > 0) {
            deathTimeRemaining -= deltaTime;
        }

        AnimationState newState = determineAnimationState();
        if (newState != currentAnimationState && DebugSettings.logAnimationState) {
            System.out.println("Player-Animation: " + currentAnimationState + " -> " + newState);
        }
        currentAnimationState = newState;
    }

    // Nur bei tatsächlicher Eingabe auf der jeweiligen Achse aktualisieren - bei reiner
    // Bewegung auf der anderen Achse (oder Stillstand) bleibt die zuletzt bekannte
    // Blickrichtung erhalten (siehe facingLeft/facingUp)
    private void updateFacing() {
        if (direction.x < 0) {
            facingLeft = true;
        } else if (direction.x > 0) {
            facingLeft = false;
        }

        if (direction.y > 0) {
            facingUp = true;
        } else if (direction.y < 0) {
            facingUp = false;
        }
    }

    // Prioritätsreihenfolge: DEATH > HURT > ATTACK > WALK > IDLE. DEATH prüft bewusst "!isAlive()"
    // statt des ablaufenden deathTimeRemaining-Timers - so bleibt die Animation dauerhaft auf
    // DEATH stehen, statt nach Ablauf des Timers sichtbar auf IDLE/WALK zurückzuspringen.
    // Bei gleichzeitig aktivem Schwert- UND Bogen-Angriff gewinnt bewusst das Schwert - beide
    // gleichzeitig aktiv zu haben ist ohnehin ein Rand-/Debug-Fall (siehe ROADMAP Quest 7).
    // DASHING nutzt bewusst dieselbe Walk-Animation - der Vertical Slice sieht keinen eigenen
    // Dash-Animationszustand vor. Funktioniert "gratis": "direction" wird während des Dashs
    // nicht zurückgesetzt und bleibt daher automatisch != 0.
    private AnimationState determineAnimationState() {
        if (!isAlive()) {
            return AnimationState.DEATH;
        }
        if (hurtTimeRemaining > 0) {
            return AnimationState.HURT;
        }
        if (sword.isAttackVisualActive() || bow.isAttackVisualActive()) {
            return AnimationState.ATTACK;
        }
        if (direction.len() > 0) {
            return AnimationState.WALK;
        }
        return AnimationState.IDLE;
    }

    // Fortschritts-Anteil für an Gameplay-Timer gekoppelte Animationen (ATTACK/HURT/DEATH,
    // siehe GDD 5.3) - IDLE/WALK laufen seit der Aseprite-Anbindung stattdessen echtzeit-basiert
    // über Animation.getFrameByElapsedTime(), siehe draw().
    private float getAnimationProgress() {
        if (currentAnimationState == AnimationState.ATTACK) {
            // Dieselbe Priorität wie in determineAnimationState(): Schwert gewinnt, falls beide
            // Waffen gleichzeitig aktiv wären
            return sword.isAttackVisualActive() ? sword.getAttackVisualProgress() : bow.getAttackVisualProgress();
        }
        if (currentAnimationState == AnimationState.HURT) {
            return 1f - (hurtTimeRemaining / HURT_ANIMATION_DURATION);
        }
        if (currentAnimationState == AnimationState.DEATH) {
            // Bleibt auch nach Ablauf des Timers bei/über 1.0 (getFrame() clampt) - hält damit
            // automatisch den letzten Death-Frame, wie gewollt
            return 1f - (deathTimeRemaining / DEATH_ANIMATION_DURATION);
        }
        return 0f;
    }

    // TODO: Bei facingUp UND currentAnimationState IDLE bzw. WALK die jeweilige "*Up"-Variante
    // zurückgeben (idleUpAnimation/walkUpAnimation) statt des Eintrags aus "animations" - ATTACK/
    // HURT/DEATH haben keine Norden-Variante, dafür also immer animations.get(currentAnimationState).
    private Animation getCurrentAnimation() {
        if (facingUp && currentAnimationState == AnimationState.IDLE){
            return idleUpAnimation;
        }
        if (facingUp && currentAnimationState == AnimationState.WALK) {
            return walkUpAnimation;
        }
        return animations.get(currentAnimationState);
    }

    // null (Tag existiert noch nicht in der Aseprite-Datei) -> Platzhalter, sonst die echte Kunst
    private Animation orPlaceholder(Animation realAnimation, Animation placeholder) {
        return realAnimation != null ? realAnimation : placeholder;
    }

    private Texture createFrameTexture(Color color) {
        Pixmap pixmap = new Pixmap(SPRITE_WIDTH, SPRITE_HEIGHT, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture frameTexture = new Texture(pixmap);
        pixmap.dispose();
        return frameTexture;
    }

    public void draw(SpriteBatch batch) {
        // Zeichenposition aufs nächste logische Pixel runden (position selbst bleibt float-genau
        // für Bewegung/Kollision) - verhindert Wackeln/Shimmer an Sprite-Rändern beim skalierten
        // Nearest-Neighbor-Rendering
        // IDLE/WALK: echtzeit-basiert (individuelle Frame-Dauern), sonst progress-basiert
        // (Gameplay-Timer-gekoppelt) - siehe Animation-Klasse
        boolean isLoopingState = currentAnimationState == AnimationState.IDLE || currentAnimationState == AnimationState.WALK;
        TextureRegion frame = isLoopingState
            ? getCurrentAnimation().getFrameByElapsedTime(animationTime)
            : getCurrentAnimation().getFrame(getAnimationProgress());
        float drawX = MathUtils.round(position.x);
        float drawY = MathUtils.round(position.y);

        // Horizontale Spiegelung über negative Breite (kein separates gespiegeltes Asset nötig).
        // Anker liegt dabei bewusst auf der RECHTEN Kante (drawX + SPRITE_WIDTH), damit das
        // gezeichnete Rechteck trotzdem [drawX, drawX + SPRITE_WIDTH] abdeckt - exakt dieselbe
        // Fläche wie im ungespiegelten Fall, nur mit umgedrehter Textur statt verschobener Position
        if (facingLeft) {
            batch.draw(frame, drawX + SPRITE_WIDTH, drawY, -SPRITE_WIDTH, SPRITE_HEIGHT);
        } else {
            batch.draw(frame, drawX, drawY, SPRITE_WIDTH, SPRITE_HEIGHT);
        }
        bow.draw(batch);
    }

    public void drawHitboxDebug(ShapeRenderer shapeRenderer) {
        sword.drawHitboxDebug(shapeRenderer, getCenter());
        bow.drawHitboxDebug(shapeRenderer);
    }

    public void dispose() {
        for (Texture frameTexture : animationTextures) {
            frameTexture.dispose();
        }
        playerSpriteSheet.dispose();
        bow.dispose();
    }
}
