package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

public class Projectile {

    private Vector2 position;
    private Vector2 direction;
    private float speed;
    private float damage;
    private float maxRange;
    private float distanceTraveled;
    private int hitsRemaining;

    // Textur wird nicht selbst geladen/verwaltet - gehört Bow, wird nur referenziert
    // (viele kurzlebige Projektile sollen sich eine einzige Textur teilen, nicht je eine eigene laden)
    private Texture texture;

    public Projectile(Vector2 origin, Vector2 direction, float speed, float damage, float maxRange, int hitsRemaining, Texture texture) {
        this.position = origin.cpy();
        this.direction = direction.cpy();
        this.speed = speed;
        this.damage = damage;
        this.maxRange = maxRange;
        this.distanceTraveled = 0f;
        this.hitsRemaining = hitsRemaining;
        this.texture = texture;
    }

    public void update(float deltaTime, Enemy target) {
        float step = speed * deltaTime;
        position.x += direction.x * step;
        position.y += direction.y * step;
        distanceTraveled += step;

        if (hitsRemaining > 0 && target.isHitBy(position)) {
            target.takeDamage((int) damage);
            hitsRemaining--;
        }
    }

    public boolean isExpired() {
        return hitsRemaining <= 0 || distanceTraveled >= maxRange;
    }

    public void draw(SpriteBatch batch) {
        batch.draw(texture, position.x - texture.getWidth() / 2f, position.y - texture.getHeight() / 2f);
    }

}
