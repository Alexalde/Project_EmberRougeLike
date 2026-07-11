package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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

    private Texture projectileTexture;
    private List<Projectile> activeProjectiles;

    public Bow() {
        this.attackCooldownDuration = 0.5f;
        this.attackCooldownRemaining = 0f;
        this.damage = 5f;
        this.projectileSpeed = 250f;
        this.maxRange = 300f;
        this.hitsPerProjectile = 1;

        this.projectileTexture = new Texture("arrow_placeholder.png");
        this.activeProjectiles = new ArrayList<>();
    }

    @Override
    public void update(float deltaTime, Enemy target) {
        if (attackCooldownRemaining > 0) {
            attackCooldownRemaining -= deltaTime;
        }

        for (Projectile projectile : activeProjectiles) {
            projectile.update(deltaTime, target);
        }
        activeProjectiles.removeIf(Projectile::isExpired);
    }

    private void fire(Vector2 origin, Vector2 direction) {
        activeProjectiles.add(
            new Projectile(origin, direction, projectileSpeed, damage, maxRange, hitsPerProjectile, projectileTexture)
        );
    }

    // "target" wird hier (noch) nicht direkt benutzt, gehört aber zur Signatur des Weapon-Interfaces
    @Override
    public boolean tryAttack(Vector2 origin, Vector2 direction, Enemy target) {
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

}
