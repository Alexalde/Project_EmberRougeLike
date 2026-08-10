package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

import java.util.List;

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

    // "targets" wird hier nicht gebraucht - Signatur folgt dem Weapon-Interface, das Bow braucht sie
    @Override
    public void update(float deltaTime, List<Damageable> targets, PlayerStats stats) {
        // Cooldown läuft ab - skaliert mit attackSpeedMultiplier (siehe GDD 2.1), 1 = unverändert
        if (attackCooldownRemaining > 0) {
            attackCooldownRemaining -= deltaTime * stats.attackSpeedMultiplier;
        }

        // Anzeigedauer des Hitbox-Flashs läuft ab
        if (attackVisualTimeRemaining > 0) {
            attackVisualTimeRemaining -= deltaTime;
        }
    }

    private void attack(Vector2 origin, Vector2 direction, List<Damageable> targets, PlayerStats stats) {
        lastAttackDirection = direction.cpy();
        attackVisualTimeRemaining = attackVisualDuration;

        // Ein Kegel-Schwung trifft ALLE Gegner darin gleichzeitig, nicht nur einen - jeder
        // getroffene Gegner bekommt einen EIGENEN Crit-Roll (siehe PlayerStats.computeDamage()).
        // Trefferzone als Shape (siehe Task #85) statt Damageable.isHitByArc()
        Shape swingShape = new ArcShape(origin, direction, range, halfConeAngleDegrees);
        for (Damageable target : targets) {
            if (target.isAlive() && swingShape.overlapsCircle(target.getCenter(), target.getHurtboxRadius())) {
                target.takeDamage(stats.computeDamage(damage, DamageType.PHYSICAL));
            }
        }
    }

    @Override
    public boolean tryAttack(Vector2 origin, Vector2 direction, List<Damageable> targets, PlayerStats stats) {
        if (attackCooldownRemaining <= 0){
            attackCooldownRemaining = attackCooldownDuration;
            attack(origin, direction, targets, stats);
            return true;
        }
        return false;
    }

    @Override
    public boolean isAttackVisualActive() {
        return attackVisualTimeRemaining > 0;
    }

    // Fortschritts-Anteil nach der GDD-5.3-Formel: 0 = Angriff hat gerade erst begonnen, 1 = fertig
    @Override
    public float getAttackVisualProgress() {
        return 1 - (attackVisualTimeRemaining / attackVisualDuration);
    }

    @Override
    public float getBaseDamage() {
        return damage;
    }

    @Override
    public DamageType getDamageType() {
        return DamageType.PHYSICAL;
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
