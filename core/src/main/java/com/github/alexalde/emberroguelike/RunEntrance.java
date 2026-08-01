package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

// Startpunkt für einen neuen Run im Hub (siehe GDD 3.2/Quest 9) - löst wie eine Tür bei Nähe
// automatisch aus, ruft aber Main.startRun() auf (fester Spawnpunkt im Zielraum statt
// Türnamen-Nachschlagen, siehe Room.getPlayerSpawn()). Eigene, sichtbare Platzhalter-Grafik wie
// Door/Terminal - kein unsichtbarer Trigger.
public class RunEntrance {

    private static final float TRIGGER_RADIUS = 20f;

    private Vector2 position;
    private Texture texture;

    public RunEntrance(Vector2 position) {
        this.position = position;
        this.texture = new Texture("run_entrance_placeholder.png");
    }

    public boolean isPlayerInRange(Vector2 playerCenter) {
        return position.dst(playerCenter) <= TRIGGER_RADIUS;
    }

    public void draw(SpriteBatch batch) {
        // Weiße Platzhalter-Textur eingefärbt (gleiche Technik wie Door/Terminal)
        batch.setColor(Color.ORANGE);
        batch.draw(
            texture,
            MathUtils.round(position.x - texture.getWidth() / 2f),
            MathUtils.round(position.y - texture.getHeight() / 2f)
        );
        batch.setColor(Color.WHITE);
    }

    public void dispose() {
        texture.dispose();
    }
}
