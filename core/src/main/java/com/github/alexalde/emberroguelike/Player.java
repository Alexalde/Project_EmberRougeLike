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
    private enum PlayerState{
        NORMAL,
        DASHING
    };
    private PlayerState state;
    private Vector2 dashDirection;
    private float dashTimeRemaining;
    private float dashCooldownRemaining;

    private static final float DASH_DISTANCE = 150f;
    private static final float DASH_DURATION = 0.15f;
    private static final float DASH_COOLDOWN = 0.5f;
    private static final float DASH_SPEED = DASH_DISTANCE / DASH_DURATION;


    public Player(float startX, float startY) {
        this.position = new Vector2(startX, startY);
        this.direction = new Vector2(0, 0);
        this.speed = 300f;
        this.texture = new Texture("player_placeholder.png");
        this.state = PlayerState.NORMAL;
    }

    public void update(float deltaTime) {
        if (state == PlayerState.DASHING) {
            // Während des Dashs: nur mit der eingefrorenen Richtung bewegen, kein Input lesen
            position.x += dashDirection.x * DASH_SPEED * deltaTime;
            position.y += dashDirection.y * DASH_SPEED * deltaTime;

            dashTimeRemaining -= deltaTime;
            if (dashTimeRemaining <= 0) {
                state = PlayerState.NORMAL;
                dashCooldownRemaining = DASH_COOLDOWN;
            }
        } else {
            // Jeden Frame die Richtung zurücksetzen
            direction.set(0, 0);

            // Richtung basierend auf Input bestimmen
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

            // Der magische Mathe-Schritt: Normalisieren!
            // nor() macht die Länge des Vektors zu 1, wenn er nicht 0 ist (wichtig, sonst Crash durch Division durch 0)
            if (direction.len() > 0) {
                direction.nor();
            }

            // Bewegung auf die Position anrechnen
            // position = position + direction * speed * deltaTime
            position.x += direction.x * speed * deltaTime;
            position.y += direction.y * speed * deltaTime;

            // Cooldown läuft nur ab, während wir NICHT dashen
            if (dashCooldownRemaining > 0) {
                dashCooldownRemaining -= deltaTime;
            }

            // Dash auslösen: frisch gedrückt, kein Cooldown mehr, und wir bewegen uns überhaupt
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                    && dashCooldownRemaining <= 0
                    && direction.len() > 0) {
                state = PlayerState.DASHING;
                dashDirection = direction.cpy();
                dashTimeRemaining = DASH_DURATION;
            }
        }

        // Bildschirmbegrenzung (Clamping)
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
