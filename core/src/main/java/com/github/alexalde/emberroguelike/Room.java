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
    private Vector2 doorPosition;

    public Room(String tmxPath) {
        this.map = new TmxMapLoader().load(tmxPath);
        this.renderer = new OrthogonalTiledMapRenderer(map);
        this.enemySpawnPoints = new ArrayList<>();

        int mapPixelHeight = map.getProperties().get("height", Integer.class)
            * map.getProperties().get("tileheight", Integer.class);

        MapObjects objects = map.getLayers().get("Objects").getObjects();
        for (MapObject object : objects) {
            String type = object.getProperties().get("type", String.class);
            float tiledX = object.getProperties().get("x", Float.class);
            float tiledY = object.getProperties().get("y", Float.class);

            // Tiled zählt Y von oben nach unten, unsere Welt von unten nach oben (Y-Flip, wie bei der Maus)
            float gameY = mapPixelHeight - tiledY;

            if ("EnemySpawn".equals(type)) {
                enemySpawnPoints.add(new Vector2(tiledX, gameY));
            } else if ("Door".equals(type)) {
                doorPosition = new Vector2(tiledX, gameY);
            }
        }

        // TODO: Debug-Print entfernen, sobald die Gegner-Liste (Task 16) sichtbar auf dem Schirm steht
        System.out.println("Room geladen: " + enemySpawnPoints.size() + " Enemy-Spawns, Door bei " + doorPosition);
    }

    public List<Vector2> getEnemySpawnPoints() {
        return enemySpawnPoints;
    }

    public Vector2 getDoorPosition() {
        return doorPosition;
    }

    public void render(OrthographicCamera camera) {
        renderer.setView(camera);
        renderer.render();
    }

    public void dispose() {
        map.dispose();
    }

}
