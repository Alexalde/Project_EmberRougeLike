package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.EnumMap;

public class Enemy {

    private Vector2 position;

    private float health;

    private static final float HURTBOX_RADIUS = 16f;

    // Alle Animations-Frames haben dieselbe feste Pixelgröße, siehe Player.SPRITE_WIDTH/HEIGHT
    private static final int SPRITE_WIDTH = 32;
    private static final int SPRITE_HEIGHT = 32;

    // Eigene Texturen (Platzhalter, laufzeit-generiert) - werden in dispose() aufgeräumt
    private Texture[] animationTextures;
    private EnumMap<AnimationState, Animation> animations;
    private AnimationState currentAnimationState;
    private float animationTime;
    private static final float WALK_ANIMATION_DURATION = 0.4f;

    // Anders als bei Sword/Bow gibt es hier kein Weapon-Interface zu erfüllen - Enemy braucht
    // diese Felder einfach direkt für seine eigene Angriffs-Animation
    private float attackVisualDuration;
    private float attackVisualTimeRemaining;

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

    public Enemy(float startX, float startY){
        this.position = new Vector2(startX, startY);
        this.health = 20;

        // Platzhalter-Frames: Idle 1, Walk 2, Attack 2 - andere Farbtöne als Player, damit beide
        // im Spiel unterscheidbar bleiben. Echte Pixelart ersetzt das später (Quest 7.10)
        this.animationTextures = new Texture[] {
            createFrameTexture(new Color(0.6f, 0.2f, 0.2f, 1f)),
            createFrameTexture(new Color(0.6f, 0.2f, 0.2f, 1f)),
            createFrameTexture(new Color(0.8f, 0.35f, 0.2f, 1f)),
            createFrameTexture(new Color(0.9f, 0.5f, 0.1f, 1f)),
            createFrameTexture(new Color(1f, 0.7f, 0.2f, 1f))
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
        // HURT/DEATH kommen erst in Quest 7.7 dazu
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
    }

    public void update(float deltaTime, Player player) {
        if (!isAlive()) {
            return;
        }

        if (attackVisualTimeRemaining > 0) {
            attackVisualTimeRemaining -= deltaTime;
        }
        animationTime += deltaTime;
        currentAnimationState = determineAnimationState();

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

            // Bewegung auf die Position anrechnen
            // position = position + direction * speed * deltaTime
            position.x += direction.x * moveSpeed * deltaTime;
            position.y += direction.y * moveSpeed * deltaTime;

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

    public void takeDamage(int amount){
        this.health = this.health - amount;

        if (DebugSettings.logDamage) {
            System.out.println("Enemy getroffen! Schaden: " + amount + ", verbleibendes Leben: " + health);
        }
    }

    public boolean isAlive(){
        return health > 0;
    }

    private Vector2 getCenter() {
        return new Vector2(position.x + SPRITE_WIDTH / 2f, position.y + SPRITE_HEIGHT / 2f);
    }

    // Prioritätsreihenfolge ATTACK > WALK > IDLE. Fällt beides nicht zu (state == ATTACKING,
    // aber gerade zwischen zwei Treffern im Cooldown), bleibt als einziger verbleibender Fall
    // IDLE übrig - kein extra "wartet"-Zustand nötig.
    private AnimationState determineAnimationState() {
        if (attackVisualTimeRemaining > 0) {
            return AnimationState.ATTACK;
        }
        if (state == EnemyState.CHASING) {
            return AnimationState.WALK;
        }
        return AnimationState.IDLE;
    }

    // Fortschritts-Anteil - dieselben zwei Formeln wie bei Player (WALK: looping über
    // animationTime, ATTACK: Countdown-Fortschritt), nur auf Enemys eigene Felder angewendet
    private float getAnimationProgress() {
        if (currentAnimationState == AnimationState.WALK) {
            return (animationTime % WALK_ANIMATION_DURATION) / WALK_ANIMATION_DURATION;
        }
        if (currentAnimationState == AnimationState.ATTACK) {
            return 1f - (attackVisualTimeRemaining / attackVisualDuration);
        }
        return 0f;
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

    public void dispose() {
        for (Texture frameTexture : animationTextures) {
            frameTexture.dispose();
        }
    }
}
