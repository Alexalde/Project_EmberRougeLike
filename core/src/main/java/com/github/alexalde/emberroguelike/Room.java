package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

public class Room {

    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;
    private List<Vector2> enemySpawnPoints;
    private List<Door> doors;
    // Interaktive Objekte (siehe Terminal, GDD 3.2/Quest 9) - aktuell nur im Hub-Raum vorhanden,
    // andere Räume haben einfach eine leere Liste
    private List<Terminal> terminals;
    // Fester Einstiegspunkt, UNABHÄNGIG vom Door-System (siehe GDD 3.2/Quest 9, Nutzer-
    // Entscheidung 2026-07-21) - für Räume, die man nicht über eine benannte Tür betritt (Hub,
    // Run-Startraum). null, wenn dieser Raum keinen hat (z.B. Räume, die nur über Türen erreicht
    // werden).
    private Vector2 playerSpawn;
    // Löst wie eine Tür bei Nähe automatisch aus, ruft aber Main.startRun() auf statt
    // switchRoom() - kein Türnamen-Nachschlagen nötig, da der Run-Startraum einen festen
    // playerSpawn hat. Aktuell nur im Hub vorhanden.
    private List<Vector2> runEntrances;
    private int pixelWidth;
    private int pixelHeight;

    // null, wenn dieser Raum (noch) keinen "TerrainData"-Layer hat (siehe Konstruktor) - bewusst
    // nur EIN Raum (testmap.tmx) für Quest 7.9 umgebaut, volle Netzwerk-Migration ist Non-Goal
    // dieser Quest (siehe ROADMAP)
    private DualGridRenderer terrainRenderer;
    // Wände als EIGENES, unabhängiges Dual-Grid (seit 2026-07-19, siehe ROADMAP) - eigenes
    // Daten-Gitter ("WallData") und eigenes Tileset, damit Wand-Kunst nicht pro Boden-Typ neu
    // angepasst werden muss. null, wenn dieser Raum (noch) keinen "WallData"-Layer hat.
    private DualGridRenderer wallRenderer;

    public Room(String tmxPath) {
        this.map = new TmxMapLoader().load(tmxPath);
        this.renderer = new OrthogonalTiledMapRenderer(map);
        this.enemySpawnPoints = new ArrayList<>();
        this.doors = new ArrayList<>();
        this.terminals = new ArrayList<>();
        this.runEntrances = new ArrayList<>();

        int tileWidth = map.getProperties().get("tilewidth", Integer.class);
        int tileHeight = map.getProperties().get("tileheight", Integer.class);
        this.pixelWidth = map.getProperties().get("width", Integer.class) * tileWidth;
        this.pixelHeight = map.getProperties().get("height", Integer.class) * tileHeight;

        MapLayer terrainDataLayer = map.getLayers().get("TerrainData");
        if (terrainDataLayer != null) {
            this.terrainRenderer = new DualGridRenderer(parseBooleanGrid((TiledMapTileLayer) terrainDataLayer), "terrain_dualgrid.png", "terrain_dualgrid.json");
        }

        MapLayer wallDataLayer = map.getLayers().get("WallData");
        if (wallDataLayer != null) {
            boolean[][] wallGrid = parseBooleanGrid((TiledMapTileLayer) wallDataLayer);
            // Wand-Tileset hat Wand/Transparent-Viertel vertauscht gezeichnet (gefunden
            // 2026-07-20) - früher wurde dafür das GITTER selbst umgekehrt, das machte aber
            // "true" in wallGrid semantisch falsch für alles außer dem Rendering (siehe
            // Sidequest-1-Bug: isBlocked() nutzte dieselben, verfälschten Daten für Kollision
            // und hielt dadurch den ganzen Rauminnenbereich für "blockiert"). Jetzt bleibt
            // wallGrid unverändert (true = echte Wand), die Kompensation passiert stattdessen
            // NUR in der Kachel-Bild-Auswahl (siehe DualGridRenderer.invertMaskLookup).
            this.wallRenderer = new DualGridRenderer(wallGrid, "walls_dualgrid.png", "walls_dualgrid.json", true);
        }

        // libGDX' TmxMapLoader flippt Objekt-Y-Koordinaten beim Laden bereits selbst (anders als
        // bei der Maus, wo WIR das manuell tun müssen) - "x"/"y" hier sind schon in unserem
        // Y-nach-oben-Weltkoordinatensystem, kein zusätzlicher Flip nötig
        MapObjects objects = map.getLayers().get("Objects").getObjects();
        for (MapObject object : objects) {
            String type = object.getProperties().get("type", String.class);
            float x = object.getProperties().get("x", Float.class);
            float y = object.getProperties().get("y", Float.class);

            if ("EnemySpawn".equals(type)) {
                enemySpawnPoints.add(new Vector2(x, y));
            } else if ("Door".equals(type)) {
                String name = object.getName();
                String targetRoom = object.getProperties().get("targetRoom", String.class);
                String targetDoorName = object.getProperties().get("targetDoorName", String.class);
                doors.add(new Door(new Vector2(x, y), name, targetRoom, targetDoorName));
            } else if ("Terminal".equals(type)) {
                terminals.add(new Terminal(new Vector2(x, y)));
            } else if ("PlayerSpawn".equals(type)) {
                playerSpawn = new Vector2(x, y);
            } else if ("RunEntrance".equals(type)) {
                runEntrances.add(new Vector2(x, y));
            }
        }
    }

    public List<Vector2> getEnemySpawnPoints() {
        return enemySpawnPoints;
    }

    public List<Door> getDoors() {
        return doors;
    }

    public List<Terminal> getTerminals() {
        return terminals;
    }

    // null, wenn dieser Raum keinen festen Einstiegspunkt hat (siehe playerSpawn oben)
    public Vector2 getPlayerSpawn() {
        return playerSpawn;
    }

    public List<Vector2> getRunEntrances() {
        return runEntrances;
    }

    public Door getDoorByName(String doorName) {
        for (Door door : doors) {
            if (door.getName() != null && door.getName().equals(doorName)) {
                return door;
            }
        }
        return null;
    }

    public int getPixelWidth() {
        return pixelWidth;
    }

    public int getPixelHeight() {
        return pixelHeight;
    }

    // Kollisionsabfrage für Player/Enemy (siehe Sidequest 1) - true, wenn an dieser Welt-Position
    // entweder eine Wand ODER eine Terrain-Lücke (Wasser/Loch) liegt. Nutzt dieselben Gitter wie
    // das Dual-Grid-Rendering (siehe DualGridRenderer.isSpecialAtWorld()), keine eigene
    // Kollisions-Datenkopie. Räume ohne die jeweilige Layer (Renderer == null) blockieren dort
    // gar nichts, statt versehentlich alles als frei ODER alles als blockiert zu behandeln.
    public boolean isBlocked(float worldX, float worldY) {
        boolean wall = wallRenderer != null && wallRenderer.isSpecialAtWorld(worldX, worldY);
        boolean gap = terrainRenderer != null && terrainRenderer.isSpecialAtWorld(worldX, worldY);
        return wall || gap;
    }

    // Läuft jeden Frame (siehe Main.render()), unabhängig davon ob Game Over ist - treibt die
    // Dual-Grid-Animationen an (z.B. Wasser-Ripple), rein kosmetisch, kein Gameplay-Bezug
    public void update(float deltaTime) {
        if (terrainRenderer != null) {
            terrainRenderer.update(deltaTime);
        }
        if (wallRenderer != null) {
            wallRenderer.update(deltaTime);
        }
    }

    // Liest einen Dual-Grid-Daten-Layer manuell aus ("TerrainData" oder "WallData", je eine
    // Zelle pro Kachel) - diese Layer werden NIE direkt vom OrthogonalTiledMapRenderer
    // gerendert, siehe render() unten
    private boolean[][] parseBooleanGrid(TiledMapTileLayer dataLayer) {
        int width = dataLayer.getWidth();
        int height = dataLayer.getHeight();
        boolean[][] grid = new boolean[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // Konvention (siehe ART_SPEC.md): Kachel-ID 1 = false (Boden/begehbar), Kachel-ID 2 = true (Lücke/Wand)
                int tileId = dataLayer.getCell(x, y).getTile().getId();
                grid[x][y] = tileId == 2;
            }
        }

        return grid;
    }

    public void render(OrthographicCamera camera) {
        renderer.setView(camera);

        if (terrainRenderer != null || wallRenderer != null) {
            // Boden zuerst zeichnen, danach Wände obendrauf - über den vom TiledMapRenderer
            // intern verwendeten Batch (spart ein zusätzliches SpriteBatch-Objekt)
            Batch batch = renderer.getBatch();
            batch.begin();
            if (terrainRenderer != null) {
                terrainRenderer.render(batch);
            }
            if (wallRenderer != null) {
                wallRenderer.render(batch);
            }
            batch.end();
        }

        // Der alte "Ground"-Layer (einzelne, nicht-autogekachelte Wand-Tiles) wird nur noch für
        // Räume OHNE eigenes Wand-Dual-Grid gerendert - sonst würden sich beide Wand-Systeme
        // überlappen. Für testmap.tmx bleiben die alten Wand-Daten bewusst noch in der Datei
        // liegen (als Fallback, klar als "Legacy" markiert), werden aber nicht mehr gezeichnet.
        if (wallRenderer == null) {
            renderer.render(new int[]{0});
        }
    }

    // Debug: Kachel-Gitter der Dual-Grid-Systeme, umschaltbar über DebugSettings.renderTerrainDebug
    public void renderTerrainDebug(ShapeRenderer shapeRenderer) {
        if (terrainRenderer != null) {
            terrainRenderer.renderDebugGrid(shapeRenderer);
        }
        if (wallRenderer != null) {
            wallRenderer.renderDebugGrid(shapeRenderer);
        }
    }

    public void dispose() {
        map.dispose();
        for (Door door : doors) {
            door.dispose();
        }
        for (Terminal terminal : terminals) {
            terminal.dispose();
        }
        if (terrainRenderer != null) {
            terrainRenderer.dispose();
        }
        if (wallRenderer != null) {
            wallRenderer.dispose();
        }
    }

}
