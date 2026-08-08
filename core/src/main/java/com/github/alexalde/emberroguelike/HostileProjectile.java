package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

// Bewegter Gefahren-Punkt (siehe GDD 6/Quest 10) - spiegelt die spieler-eigene Projectile-Klasse,
// aber gegen genau EINEN Player statt eine Damageable-Liste. Eigene Top-Level-Klasse statt in
// einer bestimmten BossAttack-Implementierung verschachtelt, da mehrere unterschiedliche Muster
// (aktuell nur ProjectileBurstAttack, potenziell mehr später) HostileProjectiles spawnen können.
// Reiner ShapeRenderer-Kreis, kein Textur-Aufwand (passt zum Boss-Platzhalter-Stil, kein
// dispose() nötig) und keine Terrain-Kollision (wie schon bei der bestehenden Projectile-Klasse).
public class HostileProjectile {

    private final Vector2 position;
    private final Vector2 direction;
    private final float speed;
    private final float damage;
    private final float maxRange;
    private final float hitboxRadius;

    private float distanceTraveled;
    private boolean expired;

    public HostileProjectile(Vector2 origin, Vector2 direction, float speed, float damage, float maxRange, float hitboxRadius) {
        this.position = origin.cpy();
        this.direction = direction.cpy();
        this.speed = speed;
        this.damage = damage;
        this.maxRange = maxRange;
        this.hitboxRadius = hitboxRadius;
    }

    public void update(float deltaTime, Player player) {
        if (expired) {
            return;
        }

        float step = speed * deltaTime;
        position.x += direction.x * step;
        position.y += direction.y * step;
        distanceTraveled += step;

        if (player.getCenter().dst(position) <= hitboxRadius + player.getHurtboxRadius()) {
            if (DebugSettings.logDamage) {
                System.out.println("Boss-Projektil trifft!");
            }
            player.takeDamage(damage);
            expired = true;
        } else if (distanceTraveled >= maxRange) {
            expired = true;
        }
    }

    public boolean isExpired() {
        return expired;
    }

    public void draw(ShapeRenderer shapeRenderer) {
        shapeRenderer.setColor(Color.ORANGE);
        shapeRenderer.circle(position.x, position.y, hitboxRadius);
    }
}
