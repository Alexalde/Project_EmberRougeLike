package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

public class Enemy {

    private Vector2 position;
    private Texture texture;

    private float health;

    private static final float HURTBOX_RADIUS = 16f;

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
        this.texture = new Texture("enemy_placeholder.png");
        this.health = 20;

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
        return new Vector2(position.x + texture.getWidth() / 2f, position.y + texture.getHeight() / 2f);
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
        batch.draw(texture, position.x, position.y);
    }

    public void drawHitboxDebug(ShapeRenderer shapeRenderer) {
        Vector2 center = getCenter();
        shapeRenderer.setColor(Color.GREEN);
        shapeRenderer.circle(center.x, center.y, HURTBOX_RADIUS);
    }

    public void dispose() {
        texture.dispose();
    }
}
