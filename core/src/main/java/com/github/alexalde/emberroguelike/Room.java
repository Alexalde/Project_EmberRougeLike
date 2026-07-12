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
    private Door door;

    public Room(String tmxPath) {
        this.map = new TmxMapLoader().load(tmxPath);
        this.renderer = new OrthogonalTiledMapRenderer(map);
        this.enemySpawnPoints = new ArrayList<>();

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
                door = new Door(new Vector2(x, y));
            }
        }
    }

    public List<Vector2> getEnemySpawnPoints() {
        return enemySpawnPoints;
    }

    public Door getDoor() {
        return door;
    }

    public void render(OrthographicCamera camera) {
        renderer.setView(camera);
        renderer.render();
    }

    public void dispose() {
        map.dispose();
        door.dispose();
    }

}
