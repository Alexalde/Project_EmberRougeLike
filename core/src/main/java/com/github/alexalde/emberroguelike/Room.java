package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.tiled.TiledMap;
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

    public Room(String tmxPath) {
        this.map = new TmxMapLoader().load(tmxPath);
        this.renderer = new OrthogonalTiledMapRenderer(map);
        this.enemySpawnPoints = new ArrayList<>();
        this.doors = new ArrayList<>();

        int tileWidth = map.getProperties().get("tilewidth", Integer.class);
        int tileHeight = map.getProperties().get("tileheight", Integer.class);
        this.pixelWidth = map.getProperties().get("width", Integer.class) * tileWidth;
        this.pixelHeight = map.getProperties().get("height", Integer.class) * tileHeight;

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

    public void render(OrthographicCamera camera) {
        renderer.setView(camera);
        renderer.render();
    }

    public void dispose() {
        map.dispose();
        for (Door door : doors) {
            door.dispose();
        }
    }

}
