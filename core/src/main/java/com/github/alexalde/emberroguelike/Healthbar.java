package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

// Bildschirmfeste UI-Anzeige der Spieler-Lebenspunkte (siehe Main: eigene UI-Kamera, unabhängig
// von der scrollenden Welt-Kamera). Liest health/maxHealth bei jedem draw() frisch aus dem
// Player - braucht deshalb keinen eigenen update()/Zustand.
//
// "Full" wird nicht gestreckt, sondern über einen Textur-Ausschnitt (TextureRegion) von links
// nach rechts aufgedeckt, je nach Lebens-Anteil - siehe draw(). Anders als Player/Enemy/Terrain/
// Wände sind das zwei einfache, statische Bilder (kein Aseprite-JSON nötig, siehe ROADMAP).
public class Healthbar {

    private static final float PADDING = 8f;

    private Texture emptyTexture;
    private Texture fullTexture;
    private TextureRegion fullRegion;

    // Echter sichtbarer Inhaltsbereich der "Full"-Grafik (ohne transparenten Rand links/rechts) -
    // einmalig beim Laden ermittelt (siehe findContentLeftEdge()/findContentRightEdge()), damit
    // spätere Sprite-Tauschaktionen automatisch korrekt aufgedeckt werden, egal wie viel
    // transparenten Rand die jeweilige Grafik hat
    private int contentLeft;
    private int contentRight;

    public Healthbar() {
        this.emptyTexture = new Texture("healthbar_empty.png");

        Pixmap fullPixmap = new Pixmap(Gdx.files.internal("healthbar_full.png"));
        this.contentLeft = findContentLeftEdge(fullPixmap);
        this.contentRight = findContentRightEdge(fullPixmap);
        this.fullTexture = new Texture(fullPixmap);
        fullPixmap.dispose();

        this.fullRegion = new TextureRegion(fullTexture);
    }

    public void draw(SpriteBatch batch, Player player) {
        float x = PADDING;
        float y = GameConfig.WORLD_HEIGHT - PADDING - emptyTexture.getHeight();

        batch.draw(emptyTexture, x, y);

        float healthPercent = player.getHealth() / player.getMaxHealth();
        int contentWidth = contentRight - contentLeft;
        int revealedWidth = contentLeft + (int) (contentWidth * healthPercent);
        fullRegion.setRegionWidth(revealedWidth);

        batch.draw(fullRegion, x, y);
    }

    // Ersten Pixel von links, der nicht komplett transparent ist (Alpha > 0) - Pixmap.getPixel()
    // liefert bei RGBA8888 einen int im Format 0xRRGGBBAA, das niedrigste Byte (& 0xFF) ist Alpha
    private int findContentLeftEdge(Pixmap pixmap) {
        for (int x = 0; x < pixmap.getWidth(); x++) {
            for (int y = 0; y < pixmap.getHeight(); y++) {
                if ((pixmap.getPixel(x, y) & 0xFF) != 0) {
                    return x;
                }
            }
        }
        return 0;
    }

    // Spiegelbild von findContentLeftEdge() - von RECHTS nach links suchen, dann +1 zurückgeben
    // (EXKLUSIVES Ende, passend zu TextureRegion.setRegionWidth())
    private int findContentRightEdge(Pixmap pixmap) {
        for (int x = pixmap.getWidth() - 1; x >= 0; x--) {
            for (int y = 0; y < pixmap.getHeight(); y++) {
                if ((pixmap.getPixel(x, y) & 0xFF) != 0) {
                    return x + 1;
                }
            }
        }
        return pixmap.getWidth();
    }

    public void dispose() {
        emptyTexture.dispose();
        fullTexture.dispose();
    }
}
