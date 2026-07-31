package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.List;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;

    // Bildschirmfeste Kamera fürs UI (Healthbar etc.) - anders als "camera" wird ihre Position
    // NIE verändert (siehe updateCamera()), damit UI-Elemente beim Scrollen der Welt-Kamera
    // an derselben Bildschirmposition bleiben
    private OrthographicCamera uiCamera;
    private Healthbar healthbar;
    // Reine Debug-Anzeige (Sidequest 1.4, DebugSettings.renderStats) - eingebaute Standard-
    // Schrift (kein eigenes Font-Asset nötig), spätere eigene Pixel-Font siehe ROADMAP-Backlog
    private BitmapFont debugFont;

    // Der tatsächliche, ganzzahlig skalierte und zentrierte Bildausschnitt auf dem echten Fenster
    private int viewportX, viewportY, viewportWidth, viewportHeight;

    private Room room;
    private Player player; // Hier ist unser Spieler-Objekt!
    private List<Enemy> enemies;
    private boolean gameOver;

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

        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);
        uiCamera.update();
        healthbar = new Healthbar();
        debugFont = new BitmapFont();

        updateViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        startGame();

        Pixmap pixmap = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.RED);
        pixmap.fill();
        mouseDebugTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    // Baut Raum/Spieler/Gegner komplett neu auf - beim allerersten Start UND bei jedem Neustart
    // nach Game Over. Alte Objekte vorher aufräumen, sonst verlieren wir bei jedem Neustart
    // Texturen/GPU-Ressourcen (Speicherleck)
    private void startGame() {
        if (room != null) {
            room.dispose();
        }
        if (player != null) {
            player.dispose();
        }
        if (enemies != null) {
            for (Enemy enemy : enemies) {
                enemy.dispose();
            }
        }

        room = new Room("maps/testmap.tmx");

        // Wir instanziieren unseren Spieler bei X:200, Y:200
        player = new Player(200, 200);

        enemies = new ArrayList<>();
        for (Vector2 spawnPoint : room.getEnemySpawnPoints()) {
            enemies.add(new Enemy(spawnPoint.x, spawnPoint.y));
        }

        gameOver = false;
    }

    // Wechselt in einen anderen Raum durch eine Tür - Raum/Gegner werden komplett neu aufgebaut
    // (wie bei startGame()), der Spieler landet an der passenden Tür im Zielraum
    private void switchRoom(String targetRoomPath, String targetDoorName) {
        room.dispose();
        for (Enemy enemy : enemies) {
            enemy.dispose();
        }

        room = new Room(targetRoomPath);

        Door entryDoor = room.getDoorByName(targetDoorName);
        player.setCenter(entryDoor.getEntryPosition());

        enemies = new ArrayList<>();
        for (Vector2 spawnPoint : room.getEnemySpawnPoints()) {
            enemies.add(new Enemy(spawnPoint.x, spawnPoint.y));
        }
    }

    // Kamera folgt dem Spieler, geklammert an die Raumgrenzen - ist der Raum in einer
    // Dimension kleiner/gleich dem Bildschirm, wird stattdessen auf die Raummitte zentriert
    // (sonst wäre der Klammer-Bereich ungültig: obere Grenze kleiner als untere)
    private void updateCamera() {
        Vector2 playerCenter = player.getCenter();
        float halfViewWidth = GameConfig.WORLD_WIDTH / 2f;
        float halfViewHeight = GameConfig.WORLD_HEIGHT / 2f;

        float camX;
        if (room.getPixelWidth() <= GameConfig.WORLD_WIDTH) {
            camX = room.getPixelWidth() / 2f;
        } else {
            camX = MathUtils.clamp(playerCenter.x, halfViewWidth, room.getPixelWidth() - halfViewWidth);
        }

        float camY;
        if (room.getPixelHeight() <= GameConfig.WORLD_HEIGHT) {
            camY = room.getPixelHeight() / 2f;
        } else {
            camY = MathUtils.clamp(playerCenter.y, halfViewHeight, room.getPixelHeight() - halfViewHeight);
        }

        // Auf ganze Pixel runden (analog zum Pixel-Snapping bei Player/Enemy/Door, Quest 7.1) -
        // sonst läge die Kamera bei laufender Bewegung auf einem sich ständig ändernden
        // Sub-Pixel-Offset, wodurch selbst pixelgenau gerundete Sprites beim Scrollen wieder
        // zittern/verschwimmen (Welt->Bildschirm-Projektion verschiebt dann alles minimal)
        camera.position.set(MathUtils.round(camX), MathUtils.round(camY), 0);
        camera.update();
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

        // Läuft IMMER, auch nach Game Over (siehe Player.updateAnimation()) - sonst würde z.B.
        // die spätere Death-Animation beim Einfrieren der restlichen Logik sofort mit einfrieren
        player.updateAnimation(deltaTime);
        // Treibt die Dual-Grid-Terrain-Animation an (rein kosmetisch, siehe Room.update())
        room.update(deltaTime);

        // 1. Logik updaten - läuft nicht mehr, sobald Game Over eingetreten ist
        if (!gameOver) {
            player.update(deltaTime, enemies, mouseWorldPosition, room);

            for (Enemy enemy : enemies) {
                enemy.update(deltaTime, player, room);
            }

            // Entfernbare Gegner entfernen (tot UND Sterbeanimation fertig, siehe
            // Enemy.isRemovable()): erst Texturen aufräumen (sonst Speicherleck), dann erst aus
            // der Liste löschen (siehe ConcurrentModificationException-Thema von neulich - deshalb
            // zwei getrennte Schritte statt Aufräumen mitten in der removeIf-Bedingung)
            for (Enemy enemy : enemies) {
                if (enemy.isRemovable()) {
                    enemy.dispose();
                }
            }
            enemies.removeIf(Enemy::isRemovable);

            // Türen bleiben verriegelt, solange noch Gegner LEBEN - bewusst NICHT
            // enemies.isEmpty(), sonst würde eine Leiche, die noch ihre Sterbeanimation
            // abspielt, die Tür unnötig verriegelt halten
            boolean anyEnemyAlive = enemies.stream().anyMatch(Enemy::isAlive);
            for (Door door : room.getDoors()) {
                door.setLocked(anyEnemyAlive);

                if (door.isPlayerInRange(player.getCenter())) {
                    switchRoom(door.getTargetRoom(), door.getTargetDoorName());
                    break; // "room"/"enemies" zeigen jetzt auf den neuen Raum - alte Türen-Liste nicht weiter durchgehen
                }
            }

            if (!player.isAlive()) {
                gameOver = true;
                System.out.println("GAME OVER - Neustart mit " + Input.Keys.toString(GameSettings.restartKey));
            }
        } else if (Gdx.input.isKeyJustPressed(GameSettings.restartKey)) {
            startGame();
        }

        updateCamera();

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
        for (Door door : room.getDoors()) {
            door.draw(batch);
        }
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

        // Bildschirmfeste UI (Healthbar) - eigener Zeichen-Durchgang mit uiCamera statt camera,
        // damit sie beim Scrollen der Welt-Kamera an derselben Bildschirmposition bleibt
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        healthbar.draw(batch, player);

        // Debug: einfache Text-Anzeige der wichtigsten Spieler-Stats, umschaltbar über
        // DebugSettings.renderStats - bewusst nur Debug-Text mit der eingebauten
        // Standard-Schrift, kein gestaltetes UI-Element (siehe ROADMAP Sidequest 1.4)
        if (DebugSettings.renderStats) {
            String statsText = String.format(
                "HP: %.0f/%.0f\nSpeed: %.0f\nDash-CD: %.2f",
                player.getHealth(), player.getMaxHealth(), player.getSpeed(), Math.max(0f, player.getDashCooldownRemaining())
            );
            debugFont.draw(batch, statsText, 8f, 60f);
        }

        batch.end();

        // Debug: exakte Hitbox-Form, umschaltbar über DebugSettings.renderHitboxes
        if (DebugSettings.renderHitboxes) {
            // ShapeRenderer hat eine EIGENE Projektionsmatrix, unabhängig vom SpriteBatch -
            // ohne das hier würde er weiterhin in physischen Fenster-Pixeln statt Welt-Koordinaten zeichnen
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            player.drawHitboxDebug(shapeRenderer);
            shapeRenderer.end();

            // Eigener Line-Durchgang für die Enemy-Hurtbox - als Kontur statt gefüllter Fläche,
            // sonst verdeckt der Kreis den darunterliegenden Sprite komplett (siehe Filled oben,
            // lässt sich nicht innerhalb desselben begin()/end() mit Line mischen). Zeigt hier
            // auch gleich die Terrain-Kollisionsbox von Player + Enemy (Sidequest 1, gelb).
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            player.drawTerrainCollisionDebug(shapeRenderer);
            for (Enemy enemy : enemies) {
                enemy.drawHitboxDebug(shapeRenderer);
                enemy.drawTerrainCollisionDebug(shapeRenderer);
            }
            shapeRenderer.end();
        }

        // Debug: Kachel-Gitter des Dual-Grid-Terrains, umschaltbar über
        // DebugSettings.renderTerrainDebug - eigener begin()/end()-Durchgang mit ShapeType.Line,
        // lässt sich nicht mit dem Filled-Durchgang oben mischen
        if (DebugSettings.renderTerrainDebug) {
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            room.renderTerrainDebug(shapeRenderer);
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
        healthbar.dispose();
        debugFont.dispose();
    }
}
