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
    private int pixelWidth;
    private int pixelHeight;

    // null, wenn dieser Raum (noch) keinen "TerrainData"-Layer hat (siehe Konstruktor) - bewusst
    // nur EIN Raum (testmap.tmx) für Quest 7.9 umgebaut, volle Netzwerk-Migration ist Non-Goal
    // dieser Quest (siehe ROADMAP)
    private DualGridTerrainRenderer terrainRenderer;

    public Room(String tmxPath) {
        this.map = new TmxMapLoader().load(tmxPath);
        this.renderer = new OrthogonalTiledMapRenderer(map);
        this.enemySpawnPoints = new ArrayList<>();
        this.doors = new ArrayList<>();

        int tileWidth = map.getProperties().get("tilewidth", Integer.class);
        int tileHeight = map.getProperties().get("tileheight", Integer.class);
        this.pixelWidth = map.getProperties().get("width", Integer.class) * tileWidth;
        this.pixelHeight = map.getProperties().get("height", Integer.class) * tileHeight;

        MapLayer terrainDataLayer = map.getLayers().get("TerrainData");
        if (terrainDataLayer != null) {
            this.terrainRenderer = new DualGridTerrainRenderer(parseTerrainGrid((TiledMapTileLayer) terrainDataLayer));
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
            }
        }
    }

    public List<Vector2> getEnemySpawnPoints() {
        return enemySpawnPoints;
    }

    public List<Door> getDoors() {
        return doors;
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

    // Liest den "TerrainData"-Layer manuell aus (Boden/Lücke pro Zelle) - dieser Layer wird
    // NIE direkt vom OrthogonalTiledMapRenderer gerendert, siehe render() unten
    private boolean[][] parseTerrainGrid(TiledMapTileLayer terrainDataLayer) {
        int width = terrainDataLayer.getWidth();
        int height = terrainDataLayer.getHeight();
        boolean[][] isGap = new boolean[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // Konvention aus dem "TerrainData"-Layer in testmap.tmx: Kachel-ID 1 = Boden, Kachel-ID 2 = Lücke
                int tileId = terrainDataLayer.getCell(x, y).getTile().getId();
                if (tileId == 2){
                    isGap[x][y] = true;
                } else {
                    isGap[x][y] = false;
                }
            }
        }

        return isGap;
    }

    public void render(OrthographicCamera camera) {
        renderer.setView(camera);

        if (terrainRenderer != null) {
            // Boden zuerst zeichnen - über den vom TiledMapRenderer intern verwendeten Batch
            // (spart ein zusätzliches SpriteBatch-Objekt), DANACH erst renderer.render() für die
            // Wände aus "Ground" - so liegen die Wände sichtbar über dem Boden
            Batch batch = renderer.getBatch();
            batch.begin();
            terrainRenderer.render(batch);
            batch.end();
        }

        // Nur Layer 0 ("Ground", nach dem TerrainData-Umbau nur noch Wand-Kacheln) rendern lassen
        // - funktioniert unverändert für Räume OHNE TerrainData-Layer (dort ist Layer 0 weiterhin
        // der einzige Tile-Layer mit vollständigen Boden+Wand-Daten wie bisher)
        renderer.render(new int[]{0});
    }

    // Debug: Kachel-Gitter des Dual-Grid-Terrains, umschaltbar über DebugSettings.renderTerrainDebug
    public void renderTerrainDebug(ShapeRenderer shapeRenderer) {
        if (terrainRenderer != null) {
            terrainRenderer.renderDebugGrid(shapeRenderer);
        }
    }

    public void dispose() {
        map.dispose();
        for (Door door : doors) {
            door.dispose();
        }
        if (terrainRenderer != null) {
            terrainRenderer.dispose();
        }
    }

}
