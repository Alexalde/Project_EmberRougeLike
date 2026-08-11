package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

public class Bow implements Weapon {

    private float attackCooldownDuration;
    private float attackCooldownRemaining;
    private float damage;
    private float projectileSpeed;
    private float maxRange;
    private int hitsPerProjectile;
    private float projectileHitboxRadius;

    private Texture projectileTexture;
    private List<Projectile> activeProjectiles;

    // Bow hat kein eigenes Angriffs-Sichtbarkeitsbedürfnis wie Sword (keine Debug-Hitbox-Anzeige)
    // - diese beiden Felder existieren nur, damit Player die Attack-Animation auch beim Bogen
    // auslösen kann (siehe Weapon-Interface)
    private float attackVisualDuration;
    private float attackVisualTimeRemaining;

    // Fächer-Winkel zwischen zusätzlichen Pfeilen bei projectileCount > 0 (siehe GDD 2.1) -
    // rein kosmetisch, damit mehrere Pfeile nicht exakt übereinanderliegend fliegen
    private static final float PROJECTILE_SPREAD_DEGREES = 10f;

    public Bow() {
        this.attackCooldownDuration = 0.5f;
        this.attackCooldownRemaining = 0f;
        this.damage = 5f;
        this.projectileSpeed = 250f;
        this.maxRange = 300f;
        this.hitsPerProjectile = 1;
        this.attackVisualDuration = 0.15f;

        this.projectileTexture = new Texture("arrow_placeholder.png");
        // An die Breite der Platzhalter-Textur gekoppelt, damit die Hitbox zur Grafik passt
        this.projectileHitboxRadius = projectileTexture.getWidth() / 2f;
        this.activeProjectiles = new ArrayList<>();
    }

    @Override
    public void update(float deltaTime, List<Damageable> targets, PlayerStats stats) {
        // Cooldown-Abbau skaliert mit attackSpeedMultiplier (siehe GDD 2.1), 1 = unverändert
        if (attackCooldownRemaining > 0) {
            attackCooldownRemaining -= deltaTime * stats.attackSpeedMultiplier;
        }
        if (attackVisualTimeRemaining > 0) {
            attackVisualTimeRemaining -= deltaTime;
        }

        for (Projectile projectile : activeProjectiles) {
            projectile.update(deltaTime, targets);
        }
        activeProjectiles.removeIf(Projectile::isExpired);
    }

    private void fire(Vector2 origin, Vector2 direction, PlayerStats stats) {
        attackVisualTimeRemaining = attackVisualDuration;

        // Der fertig verrechnete Schaden (Multiplikator + Crit-Roll, siehe
        // PlayerStats.computeDamage()) wird EINMAL pro Schuss ermittelt und in jedes der
        // gleichzeitig abgefeuerten Projektile fest eingebaut - passt zur bestehenden
        // Projectile-Architektur (ein fester Schadenswert pro Projektil-Instanz)
        float finalDamage = stats.computeDamage(damage, DamageType.PHYSICAL);
        int totalProjectiles = 1 + stats.projectileCount;

        // Multiplikator auf die Fluggeschwindigkeit (siehe GDD 2.1/Task #54), 1 = unverändert
        float finalProjectileSpeed = projectileSpeed * stats.projectileSpeedMultiplier;

        for (int i = 0; i < totalProjectiles; i++) {
            Vector2 projectileDirection = spreadDirection(direction, i, totalProjectiles);
            activeProjectiles.add(
                new Projectile(
                    origin, projectileDirection, finalProjectileSpeed, finalDamage, maxRange,
                    hitsPerProjectile, projectileHitboxRadius, projectileTexture, stats.bounceCount
                )
            );
        }
    }

    // Fächert mehrere gleichzeitige Projektile symmetrisch um die Zielrichtung auf (siehe
    // projectileCount, GDD 2.1) - bei nur einem Projektil bleibt die Richtung unverändert
    private Vector2 spreadDirection(Vector2 baseDirection, int index, int totalCount) {
        if (totalCount <= 1) {
            return baseDirection.cpy();
        }

        float offsetIndex = index - (totalCount - 1) / 2f;
        float angleOffset = offsetIndex * PROJECTILE_SPREAD_DEGREES;
        return baseDirection.cpy().rotateDeg(angleOffset);
    }

    @Override
    public boolean isAttackVisualActive() {
        return attackVisualTimeRemaining > 0;
    }

    @Override
    public float getAttackVisualProgress() {
        return 1f - (attackVisualTimeRemaining / attackVisualDuration);
    }

    // "targets" wird hier (noch) nicht direkt benutzt, gehört aber zur Signatur des Weapon-Interfaces
    @Override
    public boolean tryAttack(Vector2 origin, Vector2 direction, List<Damageable> targets, PlayerStats stats) {
        if (attackCooldownRemaining <= 0) {
            attackCooldownRemaining = attackCooldownDuration;
            fire(origin, direction, stats);
            return true;
        }
        return false;
    }

    @Override
    public float getBaseDamage() {
        return damage;
    }

    @Override
    public DamageType getDamageType() {
        return DamageType.PHYSICAL;
    }

    public void draw(SpriteBatch batch) {
        for (Projectile projectile : activeProjectiles) {
            projectile.draw(batch);
        }
    }

    public void dispose() {
        projectileTexture.dispose();
    }

    public void drawHitboxDebug(ShapeRenderer shapeRenderer) {
        for (Projectile projectile : activeProjectiles) {
            projectile.drawHitboxDebug(shapeRenderer);
        }
    }

}
