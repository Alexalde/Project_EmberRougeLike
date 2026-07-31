package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.List;

public class Projectile {

    private Vector2 position;
    private Vector2 direction;
    private float speed;
    private float damage;
    private float maxRange;
    private float distanceTraveled;
    private int hitsRemaining;

    // Eigener Kollisionsradius, passend zur tatsächlichen Größe der (Platzhalter-)Grafik -
    // wird von Bow übergeben, nicht hier selbst aus der Textur berechnet (siehe Bow-Konstruktor)
    private float hitboxRadius;

    // Textur wird nicht selbst geladen/verwaltet - gehört Bow, wird nur referenziert
    // (viele kurzlebige Projektile sollen sich eine einzige Textur teilen, nicht je eine eigene laden)
    private Texture texture;

    public Projectile(Vector2 origin, Vector2 direction, float speed, float damage, float maxRange, int hitsRemaining, float hitboxRadius, Texture texture) {
        this.position = origin.cpy();
        this.direction = direction.cpy();
        this.speed = speed;
        this.damage = damage;
        this.maxRange = maxRange;
        this.distanceTraveled = 0f;
        this.hitsRemaining = hitsRemaining;
        this.hitboxRadius = hitboxRadius;
        this.texture = texture;
    }

    public void update(float deltaTime, List<Enemy> targets) {
        float step = speed * deltaTime;
        position.x += direction.x * step;
        position.y += direction.y * step;
        distanceTraveled += step;

        for (Enemy target : targets) {
            if (hitsRemaining <= 0) {
                break;
            }
            if (target.isAlive() && target.isHitBy(position, hitboxRadius)) {
                target.takeDamage(damage);
                hitsRemaining--;
            }
        }
    }

    public boolean isExpired() {
        return hitsRemaining <= 0 || distanceTraveled >= maxRange;
    }

    public void draw(SpriteBatch batch) {
        // Platzhalter-Textur zeigt unrotiert nach oben (+Y = 90°) - daher der Versatz von -90°,
        // damit die Rotation die tatsächliche Flugrichtung trifft
        float rotation = direction.angleDeg() - 90f;

        // Pixel-Snapping wie bei Player.draw() - siehe dortiger Kommentar. Position wird VOR dem
        // Abzug des halben Texturmaßes gerundet, sonst könnte krummes Runden den Mittelpunkt
        // (das Rotationszentrum) minimal verschieben
        float drawX = MathUtils.round(position.x);
        float drawY = MathUtils.round(position.y);

        batch.draw(
            texture,
            drawX - texture.getWidth() / 2f, drawY - texture.getHeight() / 2f,
            texture.getWidth() / 2f, texture.getHeight() / 2f,
            texture.getWidth(), texture.getHeight(),
            1f, 1f,
            rotation,
            0, 0, texture.getWidth(), texture.getHeight(),
            false, false
        );
    }

    // Zeigt den echten Kollisionskreis (siehe DebugSettings.renderHitboxes), losgelöst von der Grafik
    public void drawHitboxDebug(ShapeRenderer shapeRenderer) {
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.circle(position.x, position.y, hitboxRadius);
    }

}
