package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

public class Enemy {

    private Vector2 position;
    private Texture texture;

    private float health;

    private static final float HURTBOX_RADIUS = 16f;

    public Enemy(float startX, float startY){
        this.position = new Vector2(startX, startY);
        this.texture = new Texture("enemy_placeholder.png");
        this.health = 20;
    }

    public void takeDamage(int amount){
        this.health = this.health - amount;
    }

    public boolean isAlive(){
        return health > 0;
    }

    // Reichweiten- und Kegel-Check: Ist dieser Gegner innerhalb "range" und ungefähr in "direction"?
    public boolean isHitByArc(Vector2 origin, Vector2 direction, float range, float halfAngleDegrees) {
        Vector2 toThis = position.cpy().sub(origin);
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

    // Einfacher Punkt-Treffer-Check, passend für sich bewegende Objekte wie Projectile
    public boolean isHitBy(Vector2 point) {
        return position.dst(point) <= HURTBOX_RADIUS;
    }

    public void draw(SpriteBatch batch) {
        batch.draw(texture, position.x, position.y);
    }

    public void dispose() {
        texture.dispose();
    }
}
