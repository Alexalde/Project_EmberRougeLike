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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;

    // Bildschirmfeste Kamera fürs UI (Healthbar etc.) - anders als "camera" wird ihre Position
    // NIE verändert (siehe updateCamera()), damit UI-Elemente beim Scrollen der Welt-Kamera
    // an derselben Bildschirmposition bleiben
    private OrthographicCamera uiCamera;
    private Healthbar healthbar;
    // Eingebaute Standard-Schrift (kein eigenes Font-Asset nötig) - ursprünglich nur für die
    // Sidequest-1.4-Debug-Anzeige gedacht, wird jetzt auch von RewardChoiceUI genutzt (daher kein
    // "debug"-Name mehr). Spätere eigene Pixel-Font siehe ROADMAP-Backlog.
    private BitmapFont uiFont;

    // Der tatsächliche, ganzzahlig skalierte und zentrierte Bildausschnitt auf dem echten Fenster
    private int viewportX, viewportY, viewportWidth, viewportHeight;

    private Room room;
    private Player player; // Hier ist unser Spieler-Objekt!
    private List<Enemy> enemies;
    // null, solange kein Boss im aktuellen Raum ist (siehe GDD 6/Quest 10) - vorerst nur über die
    // Debug-Taste (GameSettings.bossDebugSpawnKey) spawnbar, echte Raum-Integration folgt in
    // Quest 10.7
    private Boss boss;
    private boolean gameOver;

    // Pfade bereits geleerter Räume für den AKTUELLEN Run (siehe enterRoom()) - verhindert, dass
    // Gegner beim erneuten Betreten (Backtracking durch den Raum-Graphen) wieder auftauchen. Wird
    // in startGame() geleert, gilt also nur innerhalb eines Runs, nicht über Runs hinweg. Bewusst
    // pfad-basiert (ein Pfad = ein physischer Raum in Quest 4-6s festem Raum-Graphen) statt über
    // Instanz-IDs - Letzteres würde erfordern zu raten, wie Quest 11s noch nicht entschiedene
    // prozedurale Generierung Raum-Vorlagen wiederverwendet (siehe ROADMAP Quest 11).
    private Set<String> clearedRoomPaths;

    // Boden-Drops, die erst eingesammelt werden müssen (siehe Pickup, Nutzer-Entscheidung Quest 8:
    // XP kommt nicht mehr sofort beim Gegner-Tod, sondern als aufsammelbarer Orb)
    private List<Pickup> pickups;

    // Beute-/Level-Up-System (siehe GDD 3.1/Quest 8) - itemPool liefert die ziehbaren Items,
    // pendingRewardBatches sammelt ausgelöste, aber noch nicht angezeigte Auswahlrunden (mehrere
    // Level-Ups im selben Frame sollen nicht verlorengehen), activeReward ist die gerade sichtbare
    // Auswahl (null = keine). roomRewardGranted verhindert, dass derselbe Raum-Clear mehrfach
    // belohnt wird; roomHadEnemies verhindert eine Belohnung für Räume ohne jeden Gegner.
    private ItemPool itemPool;
    private Queue<List<Item>> pendingRewardBatches;
    private RewardChoiceUI activeReward;
    // Aktuell geöffneter Hub-Terminal-Shop (siehe GDD 3.2/Quest 9) - null, solange keiner offen ist
    private UpgradeShopUI activeShop;
    private boolean roomRewardGranted;
    private boolean roomHadEnemies;

    // Persistenter, Run-übergreifender Charakterbogen (siehe GDD 3.2/Quest 9) - einmalig beim
    // App-Start geladen (siehe create()), überlebt jeden Player-Neuaufbau in startGame(). Main
    // besitzt/speichert die Instanz, Player hält nur eine Referenz darauf.
    private MetaProgress metaProgress;
    // Feste Liste aller kaufbaren permanenten Upgrades (siehe GDD 3.2/Quest 9) - Player braucht
    // sie im Konstruktor, um gekaufte IDs in echte PermanentUpgrade-Objekte aufzulösen
    private PermanentUpgradePool upgradePool;

    // Drop-Tuning für Meta-Währungen (siehe GDD 3.2/Quest 9) - erster grober Wurf, kein finales
    // Balancing. Upgrade-Währung: häufige, kleine Menge pro Gegner-Tod. Unlock-Währung: seltener,
    // an Meilensteine gekoppelt (hier: Raum-Clear statt Einzel-Gegner).
    private static final float UPGRADE_CURRENCY_DROP_CHANCE = 0.3f;
    private static final float UPGRADE_CURRENCY_DROP_AMOUNT = 5f;
    private static final float UNLOCK_CURRENCY_DROP_AMOUNT = 10f;

    // Debug-Menü (siehe DebugValue/DebugMenuUI) - debugValues wird EINMALIG in create() gebaut,
    // die Lambdas greifen über die Instanzfelder "player"/"metaProgress" zu, bleiben also auch
    // nach einer Player-Neuerstellung (Tod -> startGame()) gültig. activeDebugMenu ist null,
    // solange das Menü geschlossen ist.
    private List<DebugValue> debugValues;
    private DebugMenuUI activeDebugMenu;

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
        uiFont = new BitmapFont();
        itemPool = new ItemPool();
        pendingRewardBatches = new LinkedList<>();
        metaProgress = MetaProgress.load();
        upgradePool = new PermanentUpgradePool();
        clearedRoomPaths = new HashSet<>();

        updateViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        startGame();
        debugValues = buildDebugValues();

        Pixmap pixmap = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.RED);
        pixmap.fill();
        mouseDebugTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    // Setzt beim allerersten Start UND nach jedem Tod komplett zurück: frischer Player (siehe
    // GDD 3.2/Quest 9 - Permanent-Upgrades werden hier in Quest 9.4 angewendet), Landung im Hub
    // an dessen festem playerSpawn (siehe Room.getPlayerSpawn() - UNABHÄNGIG vom Door-System,
    // Nutzer-Entscheidung 2026-07-21: kein begehbarer Rückweg aus einem Run in den Hub, nur Tod
    // führt zurück).
    private void startGame() {
        if (player != null) {
            player.dispose();
        }
        player = new Player(metaProgress, upgradePool);
        // Neuer Run-Zyklus - vorheriger Dungeon-Fortschritt (siehe enterRoom()) ist nicht mehr
        // relevant, Räume sollen beim nächsten Run wieder ihre Gegner haben
        clearedRoomPaths.clear();

        Room hub = new Room("maps/hub.tmx");
        enterRoom(hub, hub.getPlayerSpawn());

        gameOver = false;
        pendingRewardBatches.clear();
        activeReward = null;
    }

    // Startet einen neuen Run - vom Hub aus über ein RunEntrance-Objekt ausgelöst (siehe
    // Main.render()). Fester Einstiegspunkt (Room.getPlayerSpawn()), bewusst UNABHÄNGIG vom
    // Door-System (Nutzer-Entscheidung 2026-07-21: einheitlicher, fester Run-Start, später auch
    // für mehrere Stages gedacht) - kein Türnamen-Nachschlagen nötig.
    private void startRun() {
        Room dungeon = new Room("maps/testmap.tmx");
        enterRoom(dungeon, dungeon.getPlayerSpawn());
    }

    // Wechselt in einen anderen Raum durch eine Tür - der Spieler landet an der passenden Tür
    // im Zielraum (Door-basiertes Netzwerk, siehe Quest 6)
    private void switchRoom(String targetRoomPath, String targetDoorName) {
        Room newRoom = new Room(targetRoomPath);
        Door entryDoor = newRoom.getDoorByName(targetDoorName);
        enterRoom(newRoom, entryDoor.getEntryPosition());
    }

    // Gemeinsame Basis für startGame()/startRun()/switchRoom(): Raum/Gegner/Pickups komplett neu
    // aufbauen, Spieler an "spawnCenter" platzieren. Alte Objekte vorher aufräumen, sonst
    // verlieren wir bei jedem Wechsel Texturen/GPU-Ressourcen (Speicherleck). Player selbst wird
    // HIER nicht (neu) erstellt - nur startGame() tut das.
    private void enterRoom(Room newRoom, Vector2 spawnCenter) {
        if (room != null) {
            room.dispose();
        }
        if (enemies != null) {
            for (Enemy enemy : enemies) {
                enemy.dispose();
            }
        }
        if (pickups != null) {
            for (Pickup pickup : pickups) {
                pickup.dispose();
            }
        }

        room = newRoom;
        player.setCenter(spawnCenter);

        // Bereits geleerte Räume (siehe clearedRoomPaths) spawnen beim erneuten Betreten (z.B.
        // Backtracking im Raum-Graphen) keine Gegner mehr neu
        enemies = new ArrayList<>();
        if (!clearedRoomPaths.contains(room.getTmxPath())) {
            for (Vector2 spawnPoint : room.getEnemySpawnPoints()) {
                enemies.add(new Enemy(spawnPoint.x, spawnPoint.y));
            }
        }
        pickups = new ArrayList<>();

        // Beute-Zustand für den (neuen) Raum zurücksetzen - siehe roomHadEnemies/roomRewardGranted
        roomHadEnemies = !enemies.isEmpty();
        roomRewardGranted = false;
    }

    // Genau EINMAL in create() aufgerufen (siehe DebugValue-Klassenkommentar) - die Lambdas
    // greifen über "player"/"metaProgress" auf die aktuellen Instanzfelder zu, nicht auf eine hier
    // eingefangene Kopie, bleiben also auch nach einer Player-Neuerstellung (Tod -> startGame())
    // gültig.
    private List<DebugValue> buildDebugValues() {
        List<DebugValue> values = new ArrayList<>();
        values.add(new DebugValue("HP", player::getHealth, player::setHealth, 10f, "%.0f"));
        values.add(new DebugValue("Max-HP", player::getMaxHealth, player::setMaxHealth, 10f, "%.0f"));
        values.add(new DebugValue("Speed", player::getSpeed, player::setSpeed, 25f, "%.0f"));
        values.add(new DebugValue(
            "Dash-CD", player::getDashCooldownRemaining, player::setDashCooldownRemaining, 0.5f, "%.2f"
        ));
        values.add(new DebugValue(
            "Dmg-Multiplikator", () -> player.getStats().damageMultiplier, v -> player.getStats().damageMultiplier = v, 0.1f, "%.2f"
        ));
        values.add(new DebugValue(
            "AtkSpd-Multiplikator", () -> player.getStats().attackSpeedMultiplier,
            v -> player.getStats().attackSpeedMultiplier = v, 0.1f, "%.2f"
        ));
        values.add(new DebugValue(
            "Cooldown-Reduction", () -> player.getStats().cooldownReduction, v -> player.getStats().cooldownReduction = v, 0.05f, "%.2f"
        ));
        values.add(new DebugValue(
            "Crit-Chance", () -> player.getStats().critChance, v -> player.getStats().critChance = v, 0.05f, "%.2f"
        ));
        values.add(new DebugValue(
            "Crit-Dmg-Multiplikator", () -> player.getStats().critDamageMultiplier,
            v -> player.getStats().critDamageMultiplier = v, 0.1f, "%.2f"
        ));
        values.add(new DebugValue(
            "Projectile-Count", () -> (float) player.getStats().projectileCount,
            v -> player.getStats().projectileCount = Math.round(v), 1f, "%.0f"
        ));
        values.add(new DebugValue(
            "Upgrade-Waehrung", () -> metaProgress.upgradeCurrency, v -> metaProgress.upgradeCurrency = v, 5f, "%.0f"
        ));
        values.add(new DebugValue(
            "Unlock-Waehrung", () -> metaProgress.unlockCurrency, v -> metaProgress.unlockCurrency = v, 5f, "%.0f"
        ));
        return values;
    }

    // An DebugMenuUIs "Alles zurücksetzen"-Button verdrahtet - setzt jeden Debug-Wert auf seinen
    // bei Erstellung gesnapshotteten Ausgangswert zurück UND macht Terminal-Käufe rückgängig
    // (siehe Nutzer-Wunsch: Upgrades "entkaufen", Währung frei anpassen können)
    private void resetAllDebugValues() {
        for (DebugValue value : debugValues) {
            value.reset();
        }
        metaProgress.purchasedUpgradeIds.clear();
        metaProgress.upgradeCurrency = 0f;
        metaProgress.unlockCurrency = 0f;
        metaProgress.save();
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

    // Maus-Position in uiCamera-Koordinaten (eigene Umrechnung, da UI-Elemente wie RewardChoiceUI/
    // UpgradeShopUI in Bildschirm-fixen UI-Koordinaten liegen, nicht in Welt-Koordinaten wie
    // mouseWorldPosition) - gemeinsamer Helfer statt Duplikat in jedem Pause-Gate-Zweig
    private Vector2 getUiMousePosition() {
        Vector3 uiMouseScreenCoords = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        uiCamera.unproject(uiMouseScreenCoords, viewportX, viewportY, viewportWidth, viewportHeight);
        return new Vector2(uiMouseScreenCoords.x, uiMouseScreenCoords.y);
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

        // Unconditional (läuft auch während activeReward/activeShop offen sind) - Öffnen/
        // Schließen per Toggle-Taste, unabhängig vom sonstigen Pause-Gate-Zustand
        if (Gdx.input.isKeyJustPressed(GameSettings.debugMenuToggleKey)) {
            activeDebugMenu = (activeDebugMenu == null)
                ? new DebugMenuUI(debugValues, metaProgress::save, this::resetAllDebugValues)
                : null;
        }

        // Debug-Werkzeug (siehe GameSettings.bossDebugSpawnKey, Quest 10.3) - spawnt einen Boss
        // ohne Tiled-Änderung, bleibt auch nach der echten BossSpawn-Integration (Quest 10.7)
        // bestehen. Nur EIN Boss gleichzeitig, kein Effekt wenn schon einer da ist.
        if (Gdx.input.isKeyJustPressed(GameSettings.bossDebugSpawnKey) && boss == null) {
            boss = new Boss(player.getCenter().x + 100f, player.getCenter().y);
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

        // Nächste Auswahlrunde aktivieren, falls gerade keine läuft (siehe pendingRewardBatches) -
        // außerhalb des Pause-Gates unten, damit eine Runde auch sofort im selben Frame startet,
        // in dem sie ausgelöst wurde
        if (activeReward == null && !pendingRewardBatches.isEmpty()) {
            activeReward = new RewardChoiceUI(pendingRewardBatches.poll());
        }

        // 1. Logik updaten - pausiert komplett, solange eine Beute-/Level-Up-Auswahl läuft (wie
        // beim bestehenden Game-Over-Gate) ODER Game Over eingetreten ist. Das Debug-Menü hat die
        // höchste Priorität - pausiert auch mitten in einer Reward-/Shop-Auswahl.
        if (activeDebugMenu != null) {
            activeDebugMenu.update(getUiMousePosition());
            if (activeDebugMenu.isCloseRequested()) {
                activeDebugMenu = null;
            }
        } else if (activeReward != null) {
            activeReward.update(getUiMousePosition());

            Item confirmedChoice = activeReward.getConfirmedChoice();
            if (confirmedChoice != null) {
                confirmedChoice.apply(player.getStats());
                activeReward = null;
            }
        } else if (activeShop != null) {
            activeShop.update(getUiMousePosition());

            // Kauf-Anfrage verarbeiten (siehe UpgradeShopUI.consumePurchaseRequest()) - spendCurrency()
            // prüft das Guthaben nochmal selbst, UpgradeShopUI hat Kauf-Anfragen dafür zwar schon
            // vorgefiltert, aber doppelt genäht hält hier besser (kein stiller Datenverlust bei
            // einem eventuellen Bug in der UI-seitigen Vorfilterung)
            PermanentUpgrade purchaseRequest = activeShop.consumePurchaseRequest();
            if (purchaseRequest != null && metaProgress.spendCurrency(purchaseRequest.getCurrencyType(), purchaseRequest.getCost())) {
                metaProgress.purchasedUpgradeIds.add(purchaseRequest.getId());
                purchaseRequest.apply(player); // sofort wirksam, nicht erst beim nächsten Run
                metaProgress.save();
            }

            if (activeShop.isCloseRequested()) {
                activeShop = null;
            }
        } else if (!gameOver) {
            // Player.update() braucht List<Damageable> (siehe Quest 10) statt List<Enemy> direkt -
            // Java-Generics sind invariant, eine List<Enemy> lässt sich nicht direkt übergeben.
            // Boss wird mit reingenommen, sobald er lebt, damit Sword/Bow/Projectile ihn treffen.
            List<Damageable> combatTargets = new ArrayList<>(enemies);
            if (boss != null && boss.isAlive()) {
                combatTargets.add(boss);
            }
            player.update(deltaTime, combatTargets, mouseWorldPosition, room);

            for (Enemy enemy : enemies) {
                enemy.update(deltaTime, player, room);
            }
            if (boss != null) {
                boss.update(deltaTime, player, room);
                // Vorläufig (siehe Quest 10.7 für die volle Belohnungs-/Raum-Integration) - nur
                // ein einfacher XP-Drop, damit der Boss über die Debug-Taste testbar ist
                if (boss.isRemovable()) {
                    pickups.add(new XpPickup(boss.getCenter(), boss.getXpValue()));
                    boss.dispose();
                    boss = null;
                }
            }

            // Entfernbare Gegner entfernen (tot UND Sterbeanimation fertig, siehe
            // Enemy.isRemovable()): erst einen XP-Pickup an seiner Position platzieren + Texturen
            // aufräumen (sonst Speicherleck), dann erst aus der Liste löschen (siehe
            // ConcurrentModificationException-Thema von neulich - deshalb zwei getrennte Schritte
            // statt Aufräumen mitten in der removeIf-Bedingung). XP wird NICHT mehr sofort
            // vergeben (Nutzer-Entscheidung) - der Spieler muss den Orb erst einsammeln (siehe
            // Pickup-Schleife unten).
            for (Enemy enemy : enemies) {
                if (enemy.isRemovable()) {
                    pickups.add(new XpPickup(enemy.getCenter(), enemy.getXpValue()));
                    // Kleine Chance auf zusätzliche Upgrade-Währung (siehe GDD 3.2/Quest 9) -
                    // erster grober Wurf, kein finales Balancing
                    if (MathUtils.random() < UPGRADE_CURRENCY_DROP_CHANCE) {
                        pickups.add(new CurrencyPickup(enemy.getCenter(), MetaCurrencyType.UPGRADE, UPGRADE_CURRENCY_DROP_AMOUNT));
                    }
                    enemy.dispose();
                }
            }
            enemies.removeIf(Enemy::isRemovable);

            // Eingesammelte Pickups anwenden (siehe Pickup.collect()) - Level-Ups werden über
            // player.getLevel() VOR/NACH dem Einsammeln erkannt, nicht über einen Rückgabewert,
            // damit Pickup selbst nichts Level-Up-Spezifisches wissen muss (funktioniert später
            // genauso für Währungs-Pickups, die keine Level-Ups auslösen)
            int levelBeforePickups = player.getLevel();
            boolean currencyCollected = false;
            for (Pickup pickup : pickups) {
                // Magnet-Effekt zuerst (siehe Pickup.update()/PlayerStats.pickupRange) - zieht
                // Orbs heran, bevor auf den (viel kleineren) Kontakt-Radius geprüft wird. Behebt
                // nebenbei das Problem unerreichbar liegender Drops (z.B. an einer Wandkante).
                pickup.update(deltaTime, player.getCenter(), player.getStats().pickupRange);
                if (pickup.isInRange(player.getCenter())) {
                    pickup.collect(player);
                    if (pickup instanceof CurrencyPickup) {
                        currencyCollected = true;
                    }
                }
            }
            for (Pickup pickup : pickups) {
                if (pickup.isCollected()) {
                    pickup.dispose();
                }
            }
            pickups.removeIf(Pickup::isCollected);

            // Nach jeder Währungsänderung speichern (siehe GDD 3.2/Quest 9) - hier statt in
            // CurrencyPickup selbst, damit MetaProgress-Persistenz zentral in Main bleibt
            if (currencyCollected) {
                metaProgress.save();
            }

            int levelUps = player.getLevel() - levelBeforePickups;
            for (int i = 0; i < levelUps; i++) {
                pendingRewardBatches.add(itemPool.pickRandom(3));
            }

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

            // Run-Einstieg im Hub (siehe GDD 3.2/Quest 9) - Auto-Trigger wie bei Door, ruft aber
            // startRun() statt switchRoom() auf (fester Spawnpunkt, kein Türnamen-Nachschlagen).
            // In anderen Räumen ohne RunEntrance-Objekt ist die Liste einfach leer.
            for (RunEntrance runEntrance : room.getRunEntrances()) {
                if (runEntrance.isPlayerInRange(player.getCenter())) {
                    startRun();
                    break; // "room"/"enemies" zeigen jetzt auf den neuen Raum
                }
            }

            // Terminal-Interaktion (siehe GDD 3.2/Quest 9) - anders als Door kein Auto-Trigger,
            // sondern ein bewusster Tastendruck in Reichweite, öffnet den Upgrade-Shop
            if (Gdx.input.isKeyJustPressed(GameSettings.interactKey)) {
                for (Terminal terminal : room.getTerminals()) {
                    if (terminal.isPlayerInRange(player.getCenter())) {
                        if (DebugSettings.logInteraction) {
                            System.out.println("Terminal aktiviert");
                        }
                        activeShop = new UpgradeShopUI(upgradePool, metaProgress);
                        break;
                    }
                }
            }

            // Raum-Clear-Belohnung (siehe GDD 3.1/Quest 8) - genau einmal pro Raumbesuch, nur für
            // Räume, die überhaupt Gegner hatten. Bewusst enemies.isEmpty() statt !anyEnemyAlive
            // (anders als beim Tür-Lock oben): so löst die Belohnung erst aus, wenn der letzte
            // Gegner komplett entfernt ist (Sterbeanimation fertig, siehe Enemy.isRemovable()),
            // nicht schon im selben Frame, in dem er den tödlichen Treffer kassiert. Langfristig
            // soll das ohnehin durch einen Beute-Drop des letzten Gegners ersetzt werden (siehe
            // ROADMAP) statt eines Auto-Triggers.
            if (roomHadEnemies && enemies.isEmpty() && !roomRewardGranted) {
                roomRewardGranted = true;
                clearedRoomPaths.add(room.getTmxPath());
                pendingRewardBatches.add(itemPool.pickRandom(3));
                // Zusätzlich seltene Unlock-Währung, an den Meilenstein "Raum geschafft"
                // gekoppelt (siehe GDD 3.1 "Freischaltungen über Meilensteine") - dropt an der
                // Spielerposition, da es keinen "letzten Gegner" als Ursprung mehr gibt
                pickups.add(new CurrencyPickup(player.getCenter(), MetaCurrencyType.UNLOCK, UNLOCK_CURRENCY_DROP_AMOUNT));
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

        // Boss-Platzhalter (siehe Boss.draw(), Quest 10) - UNCONDITIONAL (kein Debug-Overlay,
        // sondern sein tatsächlicher Körper), eigener ShapeRenderer-Durchgang VOR dem
        // SpriteBatch-Weltdurchgang, damit Spieler/Gegner/Pickups (SpriteBatch) immer sichtbar
        // über dem großen Platzhalter-Kreis liegen
        if (boss != null) {
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            boss.draw(shapeRenderer);
            shapeRenderer.end();
        }

        // Jeden Frame neu setzen (nicht nur einmal in create()) - wichtig, sobald die Kamera sich
        // später bewegt (siehe Diskussion zu größeren, scrollenden Räumen)
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (Door door : room.getDoors()) {
            door.draw(batch);
        }
        for (Terminal terminal : room.getTerminals()) {
            terminal.draw(batch);
        }
        for (RunEntrance runEntrance : room.getRunEntrances()) {
            runEntrance.draw(batch);
        }
        player.draw(batch); // Wir übergeben dem Spieler die "Mal-Hand"
        for (Enemy enemy : enemies) {
            enemy.draw(batch);
        }
        for (Pickup pickup : pickups) {
            pickup.draw(batch);
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
            PlayerStats stats = player.getStats();
            String statsText = String.format(
                "HP: %.0f/%.0f\nSpeed: %.0f\nDash-CD: %.2f\n"
                    + "Level %d (%.0f/%.0f XP)\n"
                    + "Dmg x%.2f  AtkSpd x%.2f  CDR %.0f%%\n"
                    + "Crit %.0f%% (x%.2f)  Proj +%d\n"
                    + "Upgrade-Waehrung: %.0f  Unlock-Waehrung: %.0f",
                player.getHealth(), player.getMaxHealth(), player.getSpeed(), Math.max(0f, player.getDashCooldownRemaining()),
                player.getLevel(), player.getCurrentXp(), player.getXpToNextLevel(),
                stats.damageMultiplier, stats.attackSpeedMultiplier, stats.cooldownReduction * 100,
                stats.critChance * 100, stats.critDamageMultiplier, stats.projectileCount,
                metaProgress.getCurrency(MetaCurrencyType.UPGRADE), metaProgress.getCurrency(MetaCurrencyType.UNLOCK)
            );
            // Startet knapp unterhalb der Healthbar (die bei y=320..352 liegt, siehe
            // Healthbar.draw()) mit reichlich Platz nach unten - die feste virtuelle Auflösung
            // (siehe GameConfig.WORLD_HEIGHT, Quest 7.1 Letterboxing) ändert sich beim
            // Fenster-Resize NICHT, ein zu niedriger y-Wert läuft deshalb unten aus dem Bild,
            // egal wie das Fenster skaliert wird
            uiFont.draw(batch, statsText, 8f, 300f);
        }

        batch.end();

        // Beute-/Level-Up-Auswahl (siehe GDD 3.1/Quest 8) - eigener uiCamera-Durchgang, Karten
        // zuerst (ShapeRenderer), dann Text obendrauf (SpriteBatch), da beide APIs sich nicht im
        // selben begin()/end() mischen lassen
        if (activeReward != null) {
            shapeRenderer.setProjectionMatrix(uiCamera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            activeReward.renderCards(shapeRenderer);
            shapeRenderer.end();

            batch.setProjectionMatrix(uiCamera.combined);
            batch.begin();
            activeReward.renderText(batch, uiFont);
            batch.end();
        }

        // Hub-Terminal-Shop (siehe GDD 3.2/Quest 9) - gleiches zweigeteiltes Render-Muster wie
        // activeReward oben (Karten via ShapeRenderer, Text via SpriteBatch)
        if (activeShop != null) {
            shapeRenderer.setProjectionMatrix(uiCamera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            activeShop.renderCards(shapeRenderer);
            shapeRenderer.end();

            batch.setProjectionMatrix(uiCamera.combined);
            batch.begin();
            activeShop.renderText(batch, uiFont);
            batch.end();
        }

        // Debug-Menü (siehe DebugValue/DebugMenuUI) - gleiches zweigeteiltes Render-Muster wie
        // activeReward/activeShop oben
        if (activeDebugMenu != null) {
            shapeRenderer.setProjectionMatrix(uiCamera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            activeDebugMenu.renderRows(shapeRenderer);
            shapeRenderer.end();

            batch.setProjectionMatrix(uiCamera.combined);
            batch.begin();
            activeDebugMenu.renderText(batch, uiFont);
            batch.end();
        }

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
            if (boss != null) {
                boss.drawHitboxDebug(shapeRenderer);
                boss.drawTerrainCollisionDebug(shapeRenderer);
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
        if (boss != null) {
            boss.dispose();
        }
        for (Pickup pickup : pickups) {
            pickup.dispose();
        }
        mouseDebugTexture.dispose();
        healthbar.dispose();
        uiFont.dispose();
    }
}
