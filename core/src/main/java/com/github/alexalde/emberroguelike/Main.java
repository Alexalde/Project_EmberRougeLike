package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.List;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;

    // Der tatsächliche, ganzzahlig skalierte und zentrierte Bildausschnitt auf dem echten Fenster
    private int viewportX, viewportY, viewportWidth, viewportHeight;

    private Room room;
    private Player player; // Hier ist unser Spieler-Objekt!
    private List<Enemy> enemies;

    // TODO: Debug-Platzhalter für den Mauszeiger, entfernen bzw. durch echten Cursor/Crosshair ersetzen (Quest 7)
    private Texture mouseDebugTexture;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // false = Y zeigt nach oben (unsere bisherige Konvention), feste logische Größe (GameConfig.WORLD_WIDTH/GameConfig.WORLD_HEIGHT)
        camera = new OrthographicCamera();
        camera.setToOrtho(false, GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        updateViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        room = new Room("maps/testmap.tmx");

        // Wir instanziieren unseren Spieler bei X:200, Y:200
        player = new Player(200, 200);

        enemies = new ArrayList<>();
        for (Vector2 spawnPoint : room.getEnemySpawnPoints()) {
            enemies.add(new Enemy(spawnPoint.x, spawnPoint.y));
        }

        Pixmap pixmap = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.RED);
        pixmap.fill();
        mouseDebugTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    // Größtmöglicher GANZZAHLIGER Skalierungsfaktor, der noch aufs Fenster passt - Rest wird
    // als schwarzer Rand (Letterboxing) aufgefüllt, damit jedes Pixel gleichmäßig groß bleibt
    private void updateViewport(int screenWidth, int screenHeight) {
        int scale = Math.max(1, Math.min(screenWidth / GameConfig.WORLD_WIDTH, screenHeight / GameConfig.WORLD_HEIGHT));
        viewportWidth = GameConfig.WORLD_WIDTH * scale;
        viewportHeight = GameConfig.WORLD_HEIGHT * scale;
        viewportX = (screenWidth - viewportWidth) / 2;
        viewportY = (screenHeight - viewportHeight) / 2;
    }

    @Override
    public void resize(int width, int height) {
        updateViewport(width, height);
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        if (Gdx.input.isKeyJustPressed(GameSettings.fullscreenToggleKey)) {
            if (Gdx.graphics.isFullscreen()) {
                Gdx.graphics.setWindowedMode(GameConfig.WORLD_WIDTH * 2, GameConfig.WORLD_HEIGHT * 2);
            } else {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            }
        }

        // Bildschirm-Mausposition in die logische Welt-Koordinate umrechnen - berücksichtigt
        // automatisch den aktuellen Skalierungsfaktor UND die Letterboxing-Verschiebung
        Vector3 mouseScreenCoords = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouseScreenCoords, viewportX, viewportY, viewportWidth, viewportHeight);
        Vector2 mouseWorldPosition = new Vector2(mouseScreenCoords.x, mouseScreenCoords.y);

        // 1. Logik updaten
        player.update(deltaTime, enemies, mouseWorldPosition);

        // Tote Gegner entfernen: erst Texturen aufräumen (sonst Speicherleck), dann erst aus
        // der Liste löschen (siehe ConcurrentModificationException-Thema von neulich - deshalb
        // zwei getrennte Schritte statt Aufräumen mitten in der removeIf-Bedingung)
        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) {
                enemy.dispose();
            }
        }
        enemies.removeIf(enemy -> !enemy.isAlive());

        // Tür bleibt verriegelt, solange noch Gegner übrig sind
        room.getDoor().setLocked(!enemies.isEmpty());

        // 2. Bildschirm leeren
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        // Nur in den berechneten, ganzzahlig skalierten Bereich zeichnen (Rest bleibt schwarz)
        Gdx.gl.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);

        // 3. Zeichnen - Raum zuerst (eigenes internes SpriteBatch, daher außerhalb von batch.begin()/end())
        room.render(camera);

        // Jeden Frame neu setzen (nicht nur einmal in create()) - wichtig, sobald die Kamera sich
        // später bewegt (siehe Diskussion zu größeren, scrollenden Räumen)
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        room.getDoor().draw(batch);
        player.draw(batch); // Wir übergeben dem Spieler die "Mal-Hand"
        for (Enemy enemy : enemies) {
            enemy.draw(batch);
        }

        // Debug: rotes Quadrat an der (bereits umgerechneten) Mausposition, zur Kontrolle der Aim-Berechnung
        batch.draw(
            mouseDebugTexture,
            mouseWorldPosition.x - mouseDebugTexture.getWidth() / 2f,
            mouseWorldPosition.y - mouseDebugTexture.getHeight() / 2f
        );

        batch.end();

        // Debug: exakte Hitbox-Form, umschaltbar über DebugSettings.renderHitboxes
        if (DebugSettings.renderHitboxes) {
            // ShapeRenderer hat eine EIGENE Projektionsmatrix, unabhängig vom SpriteBatch -
            // ohne das hier würde er weiterhin in physischen Fenster-Pixeln statt Welt-Koordinaten zeichnen
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            player.drawHitboxDebug(shapeRenderer);
            for (Enemy enemy : enemies) {
                enemy.drawHitboxDebug(shapeRenderer);
            }
            shapeRenderer.end();
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        room.dispose();
        player.dispose(); // Auch der Spieler muss seinen Speicher aufräumen
        for (Enemy enemy : enemies) {
            enemy.dispose();
        }
        mouseDebugTexture.dispose();
    }
}
