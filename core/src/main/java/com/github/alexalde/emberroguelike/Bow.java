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
    public void update(float deltaTime, List<Enemy> targets) {
        if (attackCooldownRemaining > 0) {
            attackCooldownRemaining -= deltaTime;
        }
        if (attackVisualTimeRemaining > 0) {
            attackVisualTimeRemaining -= deltaTime;
        }

        for (Projectile projectile : activeProjectiles) {
            projectile.update(deltaTime, targets);
        }
        activeProjectiles.removeIf(Projectile::isExpired);
    }

    private void fire(Vector2 origin, Vector2 direction) {
        attackVisualTimeRemaining = attackVisualDuration;
        activeProjectiles.add(
            new Projectile(origin, direction, projectileSpeed, damage, maxRange, hitsPerProjectile, projectileHitboxRadius, projectileTexture)
        );
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
    public boolean tryAttack(Vector2 origin, Vector2 direction, List<Enemy> targets) {
        if (attackCooldownRemaining <= 0) {
            attackCooldownRemaining = attackCooldownDuration;
            fire(origin, direction);
            return true;
        }
        return false;
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
