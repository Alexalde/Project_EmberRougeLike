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
