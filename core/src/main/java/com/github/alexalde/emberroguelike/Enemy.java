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
        System.out.println("Enemy getroffen! Schaden: " + amount + ", verbleibendes Leben: " + health); // TODO: Debug-Print entfernen, sobald es einen echten Treffer-Indikator gibt
    }

    public boolean isAlive(){
        return health > 0;
    }

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
