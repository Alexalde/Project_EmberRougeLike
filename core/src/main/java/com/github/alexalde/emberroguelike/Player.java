package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2; // Neu importiert!

public class Player {
    // Wir ersetzen die losen floats für x und y durch LibGDX Vektoren
    private Vector2 position;
    private Vector2 direction; // Speichert die reine Richtung (-1, 0, 1)

    private float speed;
    private Texture texture;

    public Player(float startX, float startY) {
        this.position = new Vector2(startX, startY);
        this.direction = new Vector2(0, 0);
        this.speed = 300f;
        this.texture = new Texture("libgdx.png");
    }

    public void update(float deltaTime) {
        // Jeden Frame die Richtung zurücksetzen
        direction.set(0, 0);

        // 1. Richtung basierend auf Input bestimmen
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            direction.y += 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            direction.y -= 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            direction.x -= 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            direction.x += 1;
        }

        // 2. Der magische Mathe-Schritt: Normalisieren!
        // nor() macht die Länge des Vektors zu 1, wenn er nicht 0 ist (wichtig, sonst Crash durch Division durch 0)
        if (direction.len() > 0) {
            direction.nor();
        }

        // 3. Bewegung auf die Position anrechnen
        // position = position + direction * speed * deltaTime
        position.x += direction.x * speed * deltaTime;
        position.y += direction.y * speed * deltaTime;

        // 4. Bildschirmbegrenzung (Clamping)
        // Grenzen für X (0 bis Bildschirmbreite minus Bildbreite)
        if (position.x < 0) {
            position.x = 0;
        } else if (position.x > Gdx.graphics.getWidth() - texture.getWidth()) {
            position.x = Gdx.graphics.getWidth() - texture.getWidth();
        }

        // Grenzen für Y (0 bis Bildschirmhöhe minus Bildhöhe)
        if (position.y < 0) {
            position.y = 0;
        } else if (position.y > Gdx.graphics.getHeight() - texture.getHeight()) {
            position.y = Gdx.graphics.getHeight() - texture.getHeight();
        }
    }

    public void draw(SpriteBatch batch) {
        // Da position nun ein Vector2 ist, nutzen wir position.x und position.y
        batch.draw(texture, position.x, position.y);
    }

    public void dispose() {
        texture.dispose();
    }
}
