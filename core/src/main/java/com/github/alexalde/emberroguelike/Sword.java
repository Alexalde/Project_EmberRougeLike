package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

public class Sword implements Weapon {

    private float attackCooldownDuration;
    private float attackCooldownRemaining;
    private float damage;
    private float range;

    // 90° nach jeder Seite = 180° Halbkreis insgesamt (Startwert, siehe Konstruktor)
    private float halfConeAngleDegrees;

    // Debug-Visualisierung der exakten Hitbox, bis es einen echten Angriffs-Effekt gibt (siehe GDD 5)
    // Entspricht zugleich der Angriffsdauer selbst - soll später z.B. mit einem Attackspeed-Stat skalieren
    private float attackVisualDuration;
    private Vector2 lastAttackDirection;
    private float attackVisualTimeRemaining;

    public Sword(){
        this.attackCooldownDuration = 0.3f;
        this.attackCooldownRemaining = 0f;
        this.damage = 10f;
        this.range = 40f;
        this.halfConeAngleDegrees = 90f;
        this.attackVisualDuration = 0.1f;
        this.lastAttackDirection = new Vector2(1, 0);
    }

    // "target" wird hier nicht gebraucht - Signatur folgt dem Weapon-Interface, das Bow braucht ihn
    @Override
    public void update(float deltaTime, Enemy target) {
        // Cooldown läuft ab
        if (attackCooldownRemaining > 0) {
            attackCooldownRemaining -= deltaTime;
        }

        // Anzeigedauer des Hitbox-Flashs läuft ab
        if (attackVisualTimeRemaining > 0) {
            attackVisualTimeRemaining -= deltaTime;
        }
    }

    private void attack(Vector2 origin, Vector2 direction, Enemy target) {
        lastAttackDirection = direction.cpy();
        attackVisualTimeRemaining = attackVisualDuration;

        if (target.isHitByArc(origin, direction, range, halfConeAngleDegrees)) {
            target.takeDamage((int) damage);
        }
    }

    @Override
    public boolean tryAttack(Vector2 origin, Vector2 direction, Enemy target) {
        if (attackCooldownRemaining <= 0){
            attackCooldownRemaining = attackCooldownDuration;
            attack(origin, direction, target);
            return true;
        }
        return false;
    }

    public void drawHitboxDebug(ShapeRenderer shapeRenderer, Vector2 origin) {
        if (attackVisualTimeRemaining > 0) {
            float facingAngle = lastAttackDirection.angleDeg();
            float startAngle = facingAngle - halfConeAngleDegrees;
            float totalAngle = halfConeAngleDegrees * 2f;

            shapeRenderer.setColor(Color.YELLOW);
            shapeRenderer.arc(origin.x, origin.y, range, startAngle, totalAngle);
        }
    }

}
