package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2; // Neu importiert!

import java.util.List;

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

    private Sword sword;
    // Bow testweise auf der rechten Maustaste - echtes Waffen-Slot-System (Wechsel zwischen
    // Waffen) kommt erst später, siehe GDD 2 / ROADMAP Quest 3
    private Bow bow;
    private Vector2 aimDirection;

    public Player(float startX, float startY) {
        this.position = new Vector2(startX, startY);
        this.direction = new Vector2(0, 0);
        this.speed = 300f;
        this.texture = new Texture("player_placeholder.png");
        this.state = PlayerState.NORMAL;
        this.sword = new Sword();
        this.bow = new Bow();
        this.aimDirection = new Vector2(1, 0);
    }

    // "mouseWorldPosition" kommt bereits fertig umgerechnet von Main (Viewport-aware unproject)
    public void update(float deltaTime, List<Enemy> targets, Vector2 mouseWorldPosition) {
        sword.update(deltaTime, targets);
        bow.update(deltaTime, targets);

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
            if (Gdx.input.isKeyPressed(GameSettings.moveUpKeyPrimary) || Gdx.input.isKeyPressed(GameSettings.moveUpKeySecondary)) {
                direction.y += 1;
            }
            if (Gdx.input.isKeyPressed(GameSettings.moveDownKeyPrimary) || Gdx.input.isKeyPressed(GameSettings.moveDownKeySecondary)) {
                direction.y -= 1;
            }
            if (Gdx.input.isKeyPressed(GameSettings.moveLeftKeyPrimary) || Gdx.input.isKeyPressed(GameSettings.moveLeftKeySecondary)) {
                direction.x -= 1;
            }
            if (Gdx.input.isKeyPressed(GameSettings.moveRightKeyPrimary) || Gdx.input.isKeyPressed(GameSettings.moveRightKeySecondary)) {
                direction.x += 1;
            }

            // Normalisieren
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
            if (Gdx.input.isKeyJustPressed(GameSettings.dashKey)
                    && dashCooldownRemaining <= 0
                    && direction.len() > 0) {
                state = PlayerState.DASHING;
                dashDirection = direction.cpy();
                dashTimeRemaining = DASH_DURATION;
            }
        }

        // Bildschirmbegrenzung (Clamping) - gegen die feste logische Weltgröße, nicht die
        // tatsächliche Fenstergröße (die kann jetzt größer/kleiner/anders skaliert sein)
        // Grenzen für X (0 bis Weltbreite minus Bildbreite)
        if (position.x < 0) {
            position.x = 0;
        } else if (position.x > GameConfig.WORLD_WIDTH - texture.getWidth()) {
            position.x = GameConfig.WORLD_WIDTH - texture.getWidth();
        }

        // Grenzen für Y (0 bis Welthöhe minus Bildhöhe)
        if (position.y < 0) {
            position.y = 0;
        } else if (position.y > GameConfig.WORLD_HEIGHT - texture.getHeight()) {
            position.y = GameConfig.WORLD_HEIGHT - texture.getHeight();
        }

        Vector2 center = getCenter();
        Vector2 targetDirection = mouseWorldPosition.cpy().sub(center);

        if (targetDirection.len() > 0) {
            targetDirection.nor();

            // Bei autoSwing zählt gehalten, sonst nur der einzelne Klick-Moment
            boolean meleeInputActive = GameSettings.autoSwing
                    ? Gdx.input.isButtonPressed(GameSettings.meleeAttackButton)
                    : Gdx.input.isButtonJustPressed(GameSettings.meleeAttackButton);
            if (meleeInputActive && sword.tryAttack(center, targetDirection, targets)) {
                aimDirection = targetDirection;
            }

            boolean rangedInputActive = GameSettings.autoSwing
                    ? Gdx.input.isButtonPressed(GameSettings.rangedAttackButton)
                    : Gdx.input.isButtonJustPressed(GameSettings.rangedAttackButton);
            if (rangedInputActive && bow.tryAttack(center, targetDirection, targets)) {
                aimDirection = targetDirection;
            }
        }
    }

    // Wird später vom Schadenssystem abgefragt, bevor Schaden angewendet wird (siehe Quest 5)
    public boolean isInvincible() {
        return state == PlayerState.DASHING;
    }

    private Vector2 getCenter() {
        return new Vector2(position.x + texture.getWidth() / 2f, position.y + texture.getHeight() / 2f);
    }

    public void draw(SpriteBatch batch) {
        // Da position nun ein Vector2 ist, nutzen wir position.x und position.y
        batch.draw(texture, position.x, position.y);
        bow.draw(batch);
    }

    public void drawHitboxDebug(ShapeRenderer shapeRenderer) {
        sword.drawHitboxDebug(shapeRenderer, getCenter());
        bow.drawHitboxDebug(shapeRenderer);
    }

    public void dispose() {
        texture.dispose();
        bow.dispose();
    }
}
