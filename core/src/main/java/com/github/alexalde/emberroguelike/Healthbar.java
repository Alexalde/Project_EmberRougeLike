package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

// Bildschirmfeste UI-Anzeige der Spieler-Lebenspunkte (siehe Main: eigene UI-Kamera, unabhängig
// von der scrollenden Welt-Kamera). Liest health/maxHealth bei jedem draw() frisch aus dem
// Player - braucht deshalb keinen eigenen update()/Zustand.
public class Healthbar {

    private static final float PADDING = 8f;
    private static final float WIDTH = 100f;
    private static final float HEIGHT = 12f;
    private static final float BORDER = 2f;

    // Eine einzelne weiße 1x1-Pixel-Textur, wie schon bei Door - Größe/Farbe kommen vom
    // draw()-Aufruf (batch.setColor() + Ziel-Rechteck), nicht von der Textur selbst
    private Texture pixelTexture;

    public Healthbar() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixelTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    public void draw(SpriteBatch batch, Player player) {
        float x = PADDING;
        float y = GameConfig.WORLD_HEIGHT - PADDING - HEIGHT;

        // Rahmen/Hintergrund
        batch.setColor(Color.DARK_GRAY);
        batch.draw(pixelTexture, x, y, WIDTH, HEIGHT);

        float healthPercent = player.getHealth() / player.getMaxHealth();
        float fillWidth = (WIDTH - BORDER * 2) * healthPercent;

        batch.setColor(Color.RED);
        batch.draw(pixelTexture, x + BORDER, y + BORDER, fillWidth, HEIGHT - BORDER * 2);

        batch.setColor(Color.WHITE); // zurücksetzen, sonst würde alles Danach-Gezeichnete mitgefärbt
    }

    public void dispose() {
        pixelTexture.dispose();
    }
}
