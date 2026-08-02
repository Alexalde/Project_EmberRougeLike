package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

// Interaktives Objekt im Hub (siehe GDD 3.2/Quest 9) - anders als Door KEIN Auto-Trigger bei
// Nähe, sondern ein expliziter Tastendruck (GameSettings.interactKey) nötig; Main prüft
// isPlayerInRange() UND den Tastendruck zusammen, Terminal selbst kennt nur die Nähe-Bedingung.
// Aktuell EIN zentrales Terminal für alle Upgrades - mehrere Stationen pro Upgrade-Typ sind eine
// spätere Erweiterung (siehe ROADMAP), kein Bestandteil dieser Quest.
public class Terminal {

    private static final float INTERACTION_RADIUS = 24f;

    private Vector2 position;
    private Texture texture;

    public Terminal(Vector2 position) {
        this.position = position;
        this.texture = new Texture("terminal_placeholder.png");
    }

    public boolean isPlayerInRange(Vector2 playerCenter) {
        return position.dst(playerCenter) <= INTERACTION_RADIUS;
    }

    public void draw(SpriteBatch batch) {
        // Weiße Platzhalter-Textur eingefärbt (gleiche Technik wie Door) statt einer eigenen Grafik
        batch.setColor(Color.CYAN);
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
