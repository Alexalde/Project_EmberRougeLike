package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private Player player; // Hier ist unser Spieler-Objekt!
    private Enemy enemy;

    // TODO: Debug-Platzhalter für den Mauszeiger, entfernen bzw. durch echten Cursor/Crosshair ersetzen (Quest 7)
    private Texture mouseDebugTexture;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        // Wir instanziieren unseren Spieler bei X:200, Y:200
        player = new Player(200, 200);

        enemy = new Enemy(400, 400);

        Pixmap pixmap = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.RED);
        pixmap.fill();
        mouseDebugTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        // 1. Logik updaten
        player.update(deltaTime, enemy);

        // 2. Bildschirm leeren
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        // 3. Zeichnen
        batch.begin();
        player.draw(batch); // Wir übergeben dem Spieler die "Mal-Hand"
        enemy.draw(batch);

        // Debug: rotes Quadrat an der (Y-geflippten) Mausposition, zur Kontrolle der Aim-Berechnung
        float mouseWorldX = Gdx.input.getX();
        float mouseWorldY = Gdx.graphics.getHeight() - Gdx.input.getY();
        batch.draw(
            mouseDebugTexture,
            mouseWorldX - mouseDebugTexture.getWidth() / 2f,
            mouseWorldY - mouseDebugTexture.getHeight() / 2f
        );

        batch.end();

        // Debug: exakte Hitbox-Form, umschaltbar über DebugSettings.renderHitboxes
        if (DebugSettings.renderHitboxes) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            player.drawHitboxDebug(shapeRenderer);
            shapeRenderer.end();
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        player.dispose(); // Auch der Spieler muss seinen Speicher aufräumen
        enemy.dispose();
        mouseDebugTexture.dispose();
    }
}
