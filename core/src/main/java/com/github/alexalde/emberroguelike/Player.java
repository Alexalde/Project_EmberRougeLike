package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
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

    // Terrain-Kollisionsbox (Sidequest 1), relativ zur Zeichenposition (position.x/y = untere
    // linke Ecke des Sprites) - aus dem "Hitbox"-Tag im Spritesheet abgeleitet (siehe
    // AsepriteSpriteSheet.getHitboxBounds()). Solange dieser Tag noch nicht gezeichnet ist,
    // Fallback auf die volle Sprite-Fläche (siehe Konstruktor).
    private Rectangle hitboxBounds;

    // Echte, von Aseprite exportierte Kunst (siehe Quest 7.10) - Tags, die darin noch nicht
    // existieren, liefern null. Für solche Zustände wird stattdessen die bereits fertige
    // Idle-South-Animation wiederverwendet (echte Form + Bewegung statt eines Rechtecks), nur
    // eingefärbt (siehe tints/draw()), damit die Zustände optisch unterscheidbar bleiben. So
    // kann Kunst nach und nach eintrudeln, ohne dass der Rest auf ein fertiges Set wartet.
    private AsepriteSpriteSheet playerSpriteSheet;
    private EnumMap<AnimationState, Animation> animations;
    // WHITE = echte Kunst (keine Verfälschung der echten Farben), jede andere Farbe = Tönung
    // für einen Zustand, der noch die wiederverwendete Idle-Form nutzt
    private EnumMap<AnimationState, Color> tints;
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
    private Color idleUpTint;
    private Color walkUpTint;
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

    // Zahlen-Charakterbogen (siehe GDD 2.1/Quest 8) - Items/Level-Ups verändern diese Werte über
    // getStats(), statt direkt auf Player-interne Felder zuzugreifen
    private PlayerStats stats;

    public Player(float startX, float startY) {
        this.position = new Vector2(startX, startY);
        this.direction = new Vector2(0, 0);
        this.speed = 300f;
        this.stats = new PlayerStats();

        this.playerSpriteSheet = new AsepriteSpriteSheet("player_spritesheet.png", "player_spritesheet.json");

        Rectangle realHitboxBounds = playerSpriteSheet.getHitboxBounds();
        this.hitboxBounds = realHitboxBounds != null ? realHitboxBounds : new Rectangle(0, 0, SPRITE_WIDTH, SPRITE_HEIGHT);

        // Idle-South existiert bereits als echte Kunst (siehe Quest 7.10) - dient als
        // Ersatz-Optik (Form + echte Bewegung) für jeden Zustand, der noch keine eigene echte
        // Kunst hat, statt eines einfarbigen Rechtecks
        Animation idleShapeFallback = playerSpriteSheet.getAnimation("Idle-South");

        this.animations = new EnumMap<>(AnimationState.class);
        this.tints = new EnumMap<>(AnimationState.class);
        putStateAnimation(AnimationState.IDLE, playerSpriteSheet.getAnimation("Idle-South"), idleShapeFallback, Color.WHITE);
        putStateAnimation(AnimationState.WALK, playerSpriteSheet.getAnimation("Walk-South"), idleShapeFallback, new Color(0.5f, 0.7f, 1f, 1f));
        putStateAnimation(AnimationState.ATTACK, playerSpriteSheet.getAnimation("Attack"), idleShapeFallback, new Color(1f, 0.8f, 0.2f, 1f));
        putStateAnimation(AnimationState.HURT, playerSpriteSheet.getAnimation("Hurt"), idleShapeFallback, new Color(1f, 0.2f, 0.2f, 1f));
        putStateAnimation(AnimationState.DEATH, playerSpriteSheet.getAnimation("Death"), idleShapeFallback, new Color(0.35f, 0.35f, 0.35f, 1f));

        Animation realIdleNorth = playerSpriteSheet.getAnimation("Idle-North");
        this.idleUpAnimation = realIdleNorth != null ? realIdleNorth : idleShapeFallback;
        this.idleUpTint = realIdleNorth != null ? Color.WHITE : new Color(0.6f, 0.4f, 0.9f, 1f);

        Animation realWalkNorth = playerSpriteSheet.getAnimation("Walk-North");
        this.walkUpAnimation = realWalkNorth != null ? realWalkNorth : idleShapeFallback;
        this.walkUpTint = realWalkNorth != null ? Color.WHITE : new Color(0.3f, 0.1f, 0.6f, 1f);

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
    // "room" liefert sowohl die Raum-Maße als auch die Terrain-Kollisionsabfrage (siehe
    // Sidequest 1) - ersetzt die vorherigen einzelnen roomWidth/roomHeight-Parameter
    public void update(float deltaTime, List<Enemy> targets, Vector2 mouseWorldPosition, Room room) {
        sword.update(deltaTime, targets, stats);
        bow.update(deltaTime, targets, stats);

        if (state == PlayerState.DASHING) {
            // Während des Dashs: nur mit der eingefrorenen Richtung bewegen, kein Input lesen
            moveWithCollision(dashDirection.x * DASH_SPEED * deltaTime, dashDirection.y * DASH_SPEED * deltaTime, room);

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

            // Bewegung auf die Position anrechnen (mit Terrain-Kollision, siehe moveWithCollision())
            moveWithCollision(direction.x * speed * deltaTime, direction.y * speed * deltaTime, room);

            // Cooldown läuft nur ab, während wir NICHT dashen - skaliert mit cooldownReduction
            // (siehe GDD 2.1, wirkt NUR auf den Dash, nicht auf Waffen-Cooldowns), 0 = unverändert
            if (dashCooldownRemaining > 0) {
                dashCooldownRemaining -= deltaTime * (1f + stats.cooldownReduction);
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
        } else if (position.x > room.getPixelWidth() - SPRITE_WIDTH) {
            position.x = room.getPixelWidth() - SPRITE_WIDTH;
        }

        // Grenzen für Y (0 bis Raumhöhe minus Bildhöhe)
        if (position.y < 0) {
            position.y = 0;
        } else if (position.y > room.getPixelHeight() - SPRITE_HEIGHT) {
            position.y = room.getPixelHeight() - SPRITE_HEIGHT;
        }

        Vector2 center = getCenter();
        Vector2 targetDirection = mouseWorldPosition.cpy().sub(center);

        if (targetDirection.len() > 0) {
            targetDirection.nor();

            // Bei autoSwing zählt gehalten, sonst nur der einzelne Klick-Moment
            boolean meleeInputActive = GameSettings.autoSwing
                    ? Gdx.input.isButtonPressed(GameSettings.meleeAttackButton)
                    : Gdx.input.isButtonJustPressed(GameSettings.meleeAttackButton);
            if (meleeInputActive && sword.tryAttack(center, targetDirection, targets, stats)) {
                aimDirection = targetDirection;
            }

            boolean rangedInputActive = GameSettings.autoSwing
                    ? Gdx.input.isButtonPressed(GameSettings.rangedAttackButton)
                    : Gdx.input.isButtonJustPressed(GameSettings.rangedAttackButton);
            if (rangedInputActive && bow.tryAttack(center, targetDirection, targets, stats)) {
                aimDirection = targetDirection;
            }
        }
    }

    // Achsen-getrennte Sliding-Kollision (Sidequest 1): X und Y werden EINZELN probeweise
    // angewendet und nur übernommen, wenn die neue Position frei ist - so rutscht der Spieler
    // an einer Wand entlang, statt bei diagonaler Bewegung komplett stehenzubleiben, sobald nur
    // eine der beiden Achsen blockiert ist. Wird sowohl für normale Bewegung als auch fürs
    // Dashen verwendet (siehe update()) - Dashen soll genauso wenig durch Wände gehen können.
    private void moveWithCollision(float deltaX, float deltaY, Room room) {
        float newX = position.x + deltaX;
        if (!collidesWithRoom(newX, position.y, room)) {
            position.x = newX;
        }

        float newY = position.y + deltaY;
        if (!collidesWithRoom(position.x, newY, room)) {
            position.y = newY;
        }
    }

    // TODO (Sidequest 1.2, Lückentext): Prüfe alle 4 Ecken der KOLLISIONSBOX (nicht der ganzen
    // Sprite-Fläche!) bei Sprite-Position (x, y) gegen room.isBlocked(). Die Box liegt bei
    // (x + hitboxBounds.x, y + hitboxBounds.y) mit Breite hitboxBounds.width und Höhe
    // hitboxBounds.height - reicht also von diesem Punkt bis
    // (boxX + hitboxBounds.width - 1, boxY + hitboxBounds.height - 1). Gib true zurück, wenn
    // IRGENDEINE der 4 Ecken auf einer Wand oder Terrain-Lücke liegt, sonst false.
    private boolean collidesWithRoom(float x, float y, Room room) {
        return room.isBlocked(x + hitboxBounds.x, y + hitboxBounds.y)
            || room.isBlocked(x + hitboxBounds.x + hitboxBounds.width - 1, y + hitboxBounds.y)
            || room.isBlocked(x + hitboxBounds.x + hitboxBounds.width - 1, y + hitboxBounds.y + hitboxBounds.height - 1)
            || room.isBlocked(x + hitboxBounds.x, y + hitboxBounds.y + hitboxBounds.height - 1);
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

    // Für die Debug-Stat-Anzeige (Sidequest 1.4)
    public float getSpeed() {
        return speed;
    }

    public float getDashCooldownRemaining() {
        return dashCooldownRemaining;
    }

    // Zahlen-Charakterbogen (siehe GDD 2.1/Quest 8) - Items/Level-Ups greifen hierüber zu
    public PlayerStats getStats() {
        return stats;
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
        } else if (direction.x != 0) {
            // Reine Horizontalbewegung (kein Y-Anteil): explizit auf Süden zurücksetzen statt
            // eine evtl. vorher aktive Norden-Blickrichtung stehen zu lassen. Nur bei komplettem
            // Stillstand (direction == (0,0)) bleibt facingUp unverändert, damit Idle-North nach
            // dem Anhalten weiterhin erreichbar ist.
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

    // Tönung passend zur von getCurrentAnimation() gewählten Animation (siehe dortige Fälle) -
    // WHITE für echte Kunst (keine Verfälschung), sonst die Platzhalter-Farbe (siehe Konstruktor)
    private Color getCurrentTint() {
        if (facingUp && currentAnimationState == AnimationState.IDLE) {
            return idleUpTint;
        }
        if (facingUp && currentAnimationState == AnimationState.WALK) {
            return walkUpTint;
        }
        return tints.get(currentAnimationState);
    }

    // real == null (Tag existiert noch nicht in der Aseprite-Datei) -> shapeFallback (i.d.R.
    // Idle-South) mit Tönung, sonst die echte Kunst ungetönt (WHITE)
    private void putStateAnimation(AnimationState state, Animation real, Animation shapeFallback, Color tint) {
        animations.put(state, real != null ? real : shapeFallback);
        tints.put(state, real != null ? Color.WHITE : tint);
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

        // Tönung für Platzhalter-Zustände, die die wiederverwendete Idle-Form nutzen (siehe
        // Konstruktor/getCurrentTint()) - bei echter Kunst ist die Tönung WHITE, verändert also nichts
        batch.setColor(getCurrentTint());

        // Horizontale Spiegelung über negative Breite (kein separates gespiegeltes Asset nötig).
        // Anker liegt dabei bewusst auf der RECHTEN Kante (drawX + SPRITE_WIDTH), damit das
        // gezeichnete Rechteck trotzdem [drawX, drawX + SPRITE_WIDTH] abdeckt - exakt dieselbe
        // Fläche wie im ungespiegelten Fall, nur mit umgedrehter Textur statt verschobener Position
        if (facingLeft) {
            batch.draw(frame, drawX + SPRITE_WIDTH, drawY, -SPRITE_WIDTH, SPRITE_HEIGHT);
        } else {
            batch.draw(frame, drawX, drawY, SPRITE_WIDTH, SPRITE_HEIGHT);
        }
        batch.setColor(Color.WHITE); // zurücksetzen, sonst würde alles Danach-Gezeichnete mitgefärbt
        bow.draw(batch);
    }

    public void drawHitboxDebug(ShapeRenderer shapeRenderer) {
        sword.drawHitboxDebug(shapeRenderer, getCenter());
        bow.drawHitboxDebug(shapeRenderer);
    }

    // Zeigt die tatsächlich für Terrain-Kollision genutzte Box (Sidequest 1) - als Kontur, damit
    // man beim Zeichnen des "Hitbox"-Tags direkt sieht, ob die abgeleitete Box wie erwartet
    // aussieht. Eigene Farbe (GELB) zur Unterscheidung von Enemy.HURTBOX_RADIUS (GRÜN).
    public void drawTerrainCollisionDebug(ShapeRenderer shapeRenderer) {
        shapeRenderer.setColor(Color.YELLOW);
        float drawX = MathUtils.round(position.x);
        float drawY = MathUtils.round(position.y);
        shapeRenderer.rect(drawX + hitboxBounds.x, drawY + hitboxBounds.y, hitboxBounds.width, hitboxBounds.height);
    }

    public void dispose() {
        playerSpriteSheet.dispose();
        bow.dispose();
    }
}
