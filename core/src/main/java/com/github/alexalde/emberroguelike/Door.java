package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

public class Door {

    private Vector2 position;
    private Texture texture;
    private boolean locked;

    // Eigener Name (Tiled-Objekt-Name), damit andere Räume gezielt DIESE Tür als Ziel referenzieren können
    private String name;
    private String targetRoom;
    private String targetDoorName;

    private static final float INTERACTION_RADIUS = 20f;

    public Door(Vector2 position, String name, String targetRoom, String targetDoorName) {
        this.position = position;
        this.texture = new Texture("door_placeholder.png");
        this.locked = true;
        this.name = name;
        this.targetRoom = targetRoom;
        this.targetDoorName = targetDoorName;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isLocked() {
        return locked;
    }

    public Vector2 getPosition() {
        return position;
    }

    public String getName() {
        return name;
    }

    public String getTargetRoom() {
        return targetRoom;
    }

    public String getTargetDoorName() {
        return targetDoorName;
    }

    // Kapselt beide Bedingungen ("nah genug" UND "entriegelt") an einer Stelle,
    // statt dass Main/Player das jedes Mal getrennt abfragen müsste
    public boolean isPlayerInRange(Vector2 playerCenter) {
        return !locked && position.dst(playerCenter) <= INTERACTION_RADIUS;
    }

    public void draw(SpriteBatch batch) {
        // Weiße Platzhalter-Textur eingefärbt statt zwei separater Grafiken für offen/zu
        batch.setColor(locked ? Color.RED : Color.GREEN);
        batch.draw(texture, position.x - texture.getWidth() / 2f, position.y - texture.getHeight() / 2f);
        batch.setColor(Color.WHITE); // zurücksetzen, sonst würde alles Danach-Gezeichnete mitgefärbt
    }

    public void dispose() {
        texture.dispose();
    }

}
