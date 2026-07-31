package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.EnumMap;

public class Enemy {

    private Vector2 position;

    private float health;

    private static final float HURTBOX_RADIUS = 16f;

    // Alle Animations-Frames haben dieselbe feste Pixelgröße, siehe Player.SPRITE_WIDTH/HEIGHT
    private static final int SPRITE_WIDTH = 32;
    private static final int SPRITE_HEIGHT = 32;

    // Terrain-Kollisionsbox (Sidequest 1), gleiches Prinzip wie Player.hitboxBounds - aus dem
    // "Hitbox"-Tag im Spritesheet abgeleitet, Fallback auf die volle Sprite-Fläche solange
    // dieser Tag noch nicht gezeichnet ist (siehe Konstruktor)
    private Rectangle hitboxBounds;

    // Eigene Texturen (Platzhalter, laufzeit-generiert) - werden in dispose() aufgeräumt.
    // Bleibt null, sobald/sofern ALLE 5 Zustands-Tags in enemy_spritesheet.json existieren -
    // dann werden keine Platzhalter mehr gebraucht (siehe Konstruktor/putStateAnimation()).
    private Texture[] animationTextures;
    // Echte, von Aseprite exportierte Kunst (seit Quest 7.10, gleiches Prinzip wie Player) -
    // Tags, die darin noch nicht existieren, liefern null und fallen auf die
    // Platzhalter-Textur für diesen Zustand zurück (siehe putStateAnimation())
    private AsepriteSpriteSheet enemySpriteSheet;
    private EnumMap<AnimationState, Animation> animations;
    private AnimationState currentAnimationState;
    private float animationTime;
    private static final float WALK_ANIMATION_DURATION = 0.4f;

    // Anders als bei Sword/Bow gibt es hier kein Weapon-Interface zu erfüllen - Enemy braucht
    // diese Felder einfach direkt für seine eigene Angriffs-Animation
    private float attackVisualDuration;
    private float attackVisualTimeRemaining;

    // Countdown-Timer wie bei Player - werden in takeDamage() gesetzt, ticken in
    // updateAnimationTimers() herunter (siehe update())
    private float hurtTimeRemaining;
    private float deathTimeRemaining;
    private static final float HURT_ANIMATION_DURATION = 0.2f;
    private static final float DEATH_ANIMATION_DURATION = 0.6f;

    private enum EnemyState {
        CHASING,
        ATTACKING
    }

    private EnemyState state;
    private float moveSpeed;
    private float attackRange;
    // Größerer Schwellenwert zum Verlassen von ATTACKING als zum Betreten, damit der Zustand
    // an der Grenze nicht zwischen CHASING/ATTACKING "zittert" (Hysterese)
    private float attackRangeExitBuffer;
    private float attackCooldownDuration;
    private float attackCooldownRemaining;
    private float attackDamage;

    // XP-Belohnung beim Tod (siehe GDD 3.1/Quest 8.4)
    private float xpValue;

    public Enemy(float startX, float startY){
        this.position = new Vector2(startX, startY);
        this.health = 20;

        // Platzhalter-Frames: Idle 1, Walk 2, Attack 2, Hurt 1, Death 3 - andere Farbtöne als
        // Player, damit beide im Spiel unterscheidbar bleiben. Echte Pixelart ersetzt das
        // später (Quest 7.10)
        this.animationTextures = new Texture[] {
            createFrameTexture(new Color(0.6f, 0.2f, 0.2f, 1f)),
            createFrameTexture(new Color(0.6f, 0.2f, 0.2f, 1f)),
            createFrameTexture(new Color(0.8f, 0.35f, 0.2f, 1f)),
            createFrameTexture(new Color(0.9f, 0.5f, 0.1f, 1f)),
            createFrameTexture(new Color(1f, 0.7f, 0.2f, 1f)),
            createFrameTexture(new Color(1f, 0.9f, 0.3f, 1f)),
            createFrameTexture(new Color(0.4f, 0.05f, 0.05f, 1f)),
            createFrameTexture(new Color(0.25f, 0.05f, 0.05f, 1f)),
            createFrameTexture(new Color(0.15f, 0.15f, 0.15f, 1f))
        };
        this.animations = new EnumMap<>(AnimationState.class);
        animations.put(AnimationState.IDLE, new Animation(new TextureRegion[] {
            new TextureRegion(animationTextures[0])
        }));
        animations.put(AnimationState.WALK, new Animation(new TextureRegion[] {
            new TextureRegion(animationTextures[1]),
            new TextureRegion(animationTextures[2])
        }));
        animations.put(AnimationState.ATTACK, new Animation(new TextureRegion[] {
            new TextureRegion(animationTextures[3]),
            new TextureRegion(animationTextures[4])
        }));
        animations.put(AnimationState.HURT, new Animation(new TextureRegion[] {
            new TextureRegion(animationTextures[5])
        }));
        animations.put(AnimationState.DEATH, new Animation(new TextureRegion[] {
            new TextureRegion(animationTextures[6]),
            new TextureRegion(animationTextures[7]),
            new TextureRegion(animationTextures[8])
        }));
        // Echte Kunst nur laden, wenn die Dateien bereits existieren (anders als beim Player,
        // dessen Sheet zum Zeitpunkt der Verdrahtung schon vorlag) - AsepriteSpriteSheet würde
        // sonst beim Textur-Laden crashen. Solange die Dateien fehlen, bleiben alle Zustände auf
        // den obigen Platzhaltern; sobald sie da sind, übernimmt putRealAnimationIfExists() pro
        // Tag einzeln (kein Alles-oder-nichts, genau wie beim Spieler)
        Rectangle realHitboxBounds = null;
        if (Gdx.files.internal("enemy_spritesheet.png").exists() && Gdx.files.internal("enemy_spritesheet.json").exists()) {
            this.enemySpriteSheet = new AsepriteSpriteSheet("enemy_spritesheet.png", "enemy_spritesheet.json");
            putRealAnimationIfExists(AnimationState.IDLE, "Idle");
            putRealAnimationIfExists(AnimationState.WALK, "Walk");
            putRealAnimationIfExists(AnimationState.ATTACK, "Attack");
            putRealAnimationIfExists(AnimationState.HURT, "Hurt");
            putRealAnimationIfExists(AnimationState.DEATH, "Death");
            realHitboxBounds = enemySpriteSheet.getHitboxBounds();
        }
        this.hitboxBounds = realHitboxBounds != null ? realHitboxBounds : new Rectangle(0, 0, SPRITE_WIDTH, SPRITE_HEIGHT);

        this.currentAnimationState = AnimationState.IDLE;
        this.animationTime = 0f;
        this.attackVisualDuration = 0.15f;

        this.state = EnemyState.CHASING;
        this.moveSpeed = 60f;
        this.attackRange = 24f;
        this.attackRangeExitBuffer = 8f;
        this.attackCooldownDuration = 1f;
        this.attackCooldownRemaining = 0f;
        this.attackDamage = 10f;
        this.xpValue = 25f;
    }

    public void update(float deltaTime, Player player, Room room) {
        // Läuft IMMER, auch nach dem Tod - sonst würde die Death-Animation nie über Frame 0
        // hinauskommen (analog zu Player.updateAnimation(), siehe dortiger Kommentar). Erst
        // DANACH darf die KI/Bewegungslogik unten früh aussteigen.
        updateAnimationTimers(deltaTime);

        if (!isAlive()) {
            return;
        }

        Vector2 myCenter = getCenter();
        Vector2 playerCenter = player.getCenter();
        float distance = myCenter.dst(playerCenter);

        // Schwellenwert hängt vom AKTUELLEN Zustand ab - verhindert Zittern an der Grenze,
        // ohne einen zusätzlichen Timer, der aus dem Ruder laufen könnte
        if (state == EnemyState.ATTACKING) {
            if (distance > attackRange + attackRangeExitBuffer) {
                state = EnemyState.CHASING;
            }
        } else {
            if (distance <= attackRange) {
                state = EnemyState.ATTACKING;
            }
        }

        if (state == EnemyState.CHASING) {
            // Richtung zum Player bestimmen
            Vector2 direction = playerCenter.cpy().sub(myCenter);

            // Normalisieren
            // nor() macht die Länge des Vektors zu 1, wenn er nicht 0 ist (wichtig, sonst Crash durch Division durch 0)
            if (direction.len() > 0) {
                direction.nor();
            }

            // Bewegung auf die Position anrechnen (mit Terrain-Kollision, siehe moveWithCollision())
            moveWithCollision(direction.x * moveSpeed * deltaTime, direction.y * moveSpeed * deltaTime, room);

        } else {
            if (attackCooldownRemaining > 0) {
                attackCooldownRemaining -= deltaTime;
            } else {
                attackCooldownRemaining = attackCooldownDuration;
                attackVisualTimeRemaining = attackVisualDuration;
                if (DebugSettings.logDamage) {
                    System.out.println("Enemy greift an!");
                }
                player.takeDamage(attackDamage);
            }
        }
    }

    // Gleiche Technik wie Player.moveWithCollision() (Sidequest 1) - X und Y einzeln probeweise
    // anwenden, nur übernehmen wenn frei, damit der Gegner an Wänden entlang rutscht statt
    // stehenzubleiben oder durchzulaufen.
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

    // TODO (Sidequest 1.3, Lückentext): genau wie Player.collidesWithRoom() - prüfe alle 4 Ecken
    // der KOLLISIONSBOX (hitboxBounds, nicht die volle Sprite-Fläche!) bei Sprite-Position (x, y)
    // gegen room.isBlocked(). Box liegt bei (x + hitboxBounds.x, y + hitboxBounds.y) mit Breite
    // hitboxBounds.width und Höhe hitboxBounds.height. True, wenn irgendeine Ecke blockiert ist.
    private boolean collidesWithRoom(float x, float y, Room room) {
        return room.isBlocked(x + hitboxBounds.x, y + hitboxBounds.y)
            || room.isBlocked(x + hitboxBounds.x + hitboxBounds.width - 1, y + hitboxBounds.y)
            || room.isBlocked(x + hitboxBounds.x + hitboxBounds.width - 1, y + hitboxBounds.y + hitboxBounds.height - 1)
            || room.isBlocked(x + hitboxBounds.x, y + hitboxBounds.y + hitboxBounds.height - 1);
    }

    // float statt int (Quest 8.2) - passend zu Player.takeDamage(float) und Enemy.health
    // (bereits float); vermeidet Präzisionsverlust nach der Stat-Verrechnung (Multiplikator/Crit)
    public void takeDamage(float amount){
        this.health = this.health - amount;

        hurtTimeRemaining = HURT_ANIMATION_DURATION;
        if (!isAlive()) {
            deathTimeRemaining = DEATH_ANIMATION_DURATION;
        }

        if (DebugSettings.logDamage) {
            System.out.println("Enemy getroffen! Schaden: " + amount + ", verbleibendes Leben: " + health);
        }
    }

    public boolean isAlive(){
        return health > 0;
    }

    // Kapselt "tot UND Sterbeanimation fertig" - Main entfernt/disposed den Gegner erst dann,
    // statt ihn im selben Frame zu entfernen, in dem er stirbt (siehe Main.render())
    public boolean isRemovable() {
        return !isAlive() && deathTimeRemaining <= 0;
    }

    public float getXpValue() {
        return xpValue;
    }

    private Vector2 getCenter() {
        return new Vector2(position.x + SPRITE_WIDTH / 2f, position.y + SPRITE_HEIGHT / 2f);
    }

    private void updateAnimationTimers(float deltaTime) {
        if (attackVisualTimeRemaining > 0) {
            attackVisualTimeRemaining -= deltaTime;
        }
        if (hurtTimeRemaining > 0) {
            hurtTimeRemaining -= deltaTime;
        }
        if (deathTimeRemaining > 0) {
            deathTimeRemaining -= deltaTime;
        }
        animationTime += deltaTime;
        currentAnimationState = determineAnimationState();
    }

    // Prioritätsreihenfolge DEATH > HURT > ATTACK > WALK > IDLE. DEATH prüft bewusst
    // "!isAlive()" statt des ablaufenden deathTimeRemaining-Timers, damit die Animation
    // dauerhaft auf DEATH stehen bleibt (siehe Player.determineAnimationState()).
    // Fällt weder ATTACK noch WALK zu (state == ATTACKING, aber gerade zwischen zwei Treffern
    // im Cooldown), bleibt als einziger verbleibender Fall IDLE übrig - kein extra
    // "wartet"-Zustand nötig.
    private AnimationState determineAnimationState() {
        if (!isAlive()) {
            return AnimationState.DEATH;
        }
        if (hurtTimeRemaining > 0) {
            return AnimationState.HURT;
        }
        if (attackVisualTimeRemaining > 0) {
            return AnimationState.ATTACK;
        }
        if (state == EnemyState.CHASING) {
            return AnimationState.WALK;
        }
        return AnimationState.IDLE;
    }

    // Fortschritts-Anteil - dieselben Formeln wie bei Player (WALK: looping über
    // animationTime, ATTACK/HURT/DEATH: Countdown-Fortschritt), nur auf Enemys eigene Felder
    // angewendet
    private float getAnimationProgress() {
        if (currentAnimationState == AnimationState.WALK) {
            return (animationTime % WALK_ANIMATION_DURATION) / WALK_ANIMATION_DURATION;
        }
        if (currentAnimationState == AnimationState.ATTACK) {
            return 1f - (attackVisualTimeRemaining / attackVisualDuration);
        }
        if (currentAnimationState == AnimationState.HURT) {
            return 1f - (hurtTimeRemaining / HURT_ANIMATION_DURATION);
        }
        if (currentAnimationState == AnimationState.DEATH) {
            return 1f - (deathTimeRemaining / DEATH_ANIMATION_DURATION);
        }
        return 0f;
    }

    // real == null (Tag existiert noch nicht in der Aseprite-Datei) -> Platzhalter für diesen
    // Zustand bleibt unverändert bestehen, sonst wird er durch die echte Kunst ersetzt
    private void putRealAnimationIfExists(AnimationState state, String tagName) {
        Animation real = enemySpriteSheet.getAnimation(tagName);
        if (real != null) {
            animations.put(state, real);
        }
    }

    private Texture createFrameTexture(Color color) {
        Pixmap pixmap = new Pixmap(SPRITE_WIDTH, SPRITE_HEIGHT, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture frameTexture = new Texture(pixmap);
        pixmap.dispose();
        return frameTexture;
    }

    // Reichweiten- und Kegel-Check: Ist dieser Gegner innerhalb "range" und ungefähr in "direction"?
    public boolean isHitByArc(Vector2 origin, Vector2 direction, float range, float halfAngleDegrees) {
        Vector2 toThis = getCenter().sub(origin);
        float distance = toThis.len();

        if (distance > range + HURTBOX_RADIUS) {
            return false;
        }
        if (distance == 0) {
            return true;
        }

        toThis.nor();
        float minDot = (float) Math.cos(Math.toRadians(halfAngleDegrees));
        return direction.dot(toThis) >= minDot;
    }

    // Kreis-Kreis-Check: Abstand der Mittelpunkte gegen die SUMME beider Radien,
    // damit auch größere Projektile eine passend große Hitbox haben (nicht nur ein Punkt)
    public boolean isHitBy(Vector2 point, float otherRadius) {
        return getCenter().dst(point) <= HURTBOX_RADIUS + otherRadius;
    }

    public void draw(SpriteBatch batch) {
        // Pixel-Snapping wie bei Player.draw() - siehe dortiger Kommentar
        TextureRegion frame = animations.get(currentAnimationState).getFrame(getAnimationProgress());
        batch.draw(frame, MathUtils.round(position.x), MathUtils.round(position.y));
    }

    public void drawHitboxDebug(ShapeRenderer shapeRenderer) {
        Vector2 center = getCenter();
        shapeRenderer.setColor(Color.GREEN);
        shapeRenderer.circle(center.x, center.y, HURTBOX_RADIUS);
    }

    // Zeigt die tatsächlich für Terrain-Kollision genutzte Box (Sidequest 1), gleiches Prinzip
    // wie Player.drawTerrainCollisionDebug() - GELB, zur Unterscheidung von der GRÜNEN Hurtbox oben
    public void drawTerrainCollisionDebug(ShapeRenderer shapeRenderer) {
        shapeRenderer.setColor(Color.YELLOW);
        float drawX = MathUtils.round(position.x);
        float drawY = MathUtils.round(position.y);
        shapeRenderer.rect(drawX + hitboxBounds.x, drawY + hitboxBounds.y, hitboxBounds.width, hitboxBounds.height);
    }

    public void dispose() {
        for (Texture frameTexture : animationTextures) {
            frameTexture.dispose();
        }
        if (enemySpriteSheet != null) {
            enemySpriteSheet.dispose();
        }
    }
}
