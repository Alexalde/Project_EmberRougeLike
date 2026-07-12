package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

public class Door {

    private Vector2 position;
    private Texture texture;
    private boolean locked;

    public Door(Vector2 position) {
        this.position = position;
        this.texture = new Texture("door_placeholder.png");
        this.locked = true;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isLocked() {
        return locked;
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
