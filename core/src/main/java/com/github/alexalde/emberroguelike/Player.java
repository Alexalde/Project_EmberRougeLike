package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Player {
    // Eigenschaften des Spielers (Private, damit niemand von außen ungefragt rumpfuscht!)
    private float x;
    private float y;
    private float speed;
    private Texture texture;

    // Der Konstruktor: Wird aufgerufen, wenn wir "new Player()" sagen
    public Player(float startX, float startY) {
        this.x = startX;
        this.y = startY;
        this.speed = 300f;
        this.texture = new Texture("libgdx.png"); // Nutzen vorerst das gleiche Logo
    }

    // Die Logik: Input verarbeiten und Position updaten
    public void update(float deltaTime) {
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            y += speed * deltaTime;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            y -= speed * deltaTime;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            x -= speed * deltaTime;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            x += speed * deltaTime;
        }
    }

    // Die Darstellung: Der Spieler weiß selbst, wie er sich zeichnen muss
    public void draw(SpriteBatch batch) {
        batch.draw(texture, x, y);
    }

    // Speicher freigeben: Wichtig für sauberen Java-Spielecode!
    public void dispose() {
        texture.dispose();
    }
}
