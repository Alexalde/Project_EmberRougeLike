package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

// Dual-Grid-Terrain-System (siehe ROADMAP Quest 7): das Render-Gitter liegt um eine halbe
// Kachel gegen das Daten-Gitter versetzt. Jede Render-Kachel wird aus den 4 angrenzenden
// Daten-Zellen bestimmt (2^4 = 16 Kombinationen) - deutlich weniger Kachel-Grafiken als beim
// klassischen Blob-Autotiling (bis zu 47), und kein natives Tiled-Feature, daher ein eigener
// Rendering-Layer statt eines Tiled-"Wang Sets".
//
// Isolierter Prototyp (Quest 7.8): bekommt sein Daten-Gitter direkt übergeben (z.B. fest im
// Code hinterlegt zum Testen), OHNE jede Tiled-Anbindung - die kommt erst in Quest 7.9, wenn
// die konkrete Layer-Konvention gemeinsam entworfen wird.
public class DualGridTerrainRenderer {

    private static final int TILE_SIZE = 32;

    // true = Lücke (z.B. Wasser/Loch), false = Boden
    private final boolean[][] isGap;
    private final int gridWidth;
    private final int gridHeight;

    private final Texture[] lookupTextures;
    private final TextureRegion[] lookupRegions;

    public DualGridTerrainRenderer(boolean[][] isGap) {
        this.isGap = isGap;
        this.gridWidth = isGap.length;
        this.gridHeight = isGap[0].length;

        this.lookupTextures = new Texture[16];
        this.lookupRegions = new TextureRegion[16];
        for (int mask = 0; mask < 16; mask++) {
            lookupTextures[mask] = createLookupTexture(mask);
            lookupRegions[mask] = new TextureRegion(lookupTextures[mask]);
        }
    }

    // Clamp-to-Edge: Zellen außerhalb des Gitters übernehmen den Wert der nächstgelegenen
    // gültigen Zelle, statt eines künstlichen Default-Werts - Rand-Terrain setzt sich so
    // natürlich über den Kartenrand hinaus fort, ohne Sonderfall-Code
    private boolean isGapAt(int x, int y) {
        int clampedX = MathUtils.clamp(x, 0, gridWidth - 1);
        int clampedY = MathUtils.clamp(y, 0, gridHeight - 1);
        return isGap[clampedX][clampedY];
    }

    public void render(Batch batch) {
        // Render-Gitter ist in jeder Achse eine Kachel GRÖSSER als das Daten-Gitter (<=, nicht <)
        // - eine Render-Kachel pro Eck-Schnittpunkt des Daten-Gitters, siehe computeMask()
        for (int rx = 0; rx <= gridWidth; rx++) {
            for (int ry = 0; ry <= gridHeight; ry++) {
                int mask = computeMask(rx, ry);
                // Versatz um eine halbe Kachel: die Render-Kachel liegt zentriert auf dem
                // Eck-Schnittpunkt (rx, ry) des Daten-Gitters, nicht auf einer Daten-Zelle selbst
                float drawX = rx * TILE_SIZE - TILE_SIZE / 2f;
                float drawY = ry * TILE_SIZE - TILE_SIZE / 2f;
                batch.draw(lookupRegions[mask], drawX, drawY, TILE_SIZE, TILE_SIZE);
            }
        }
    }

    // Debug-Hilfe (siehe DebugSettings.renderTerrainDebug): zeichnet ein dünnes Rechteck um JEDE
    // einzelne Render-Kachel - ohne das verschwimmen gleichfarbige Nachbarkacheln optisch zu
    // einer Fläche, man sieht die Kachel-Grenzen selbst nicht. Eigener begin()/end()-Durchgang
    // mit ShapeType.Line nötig (siehe Main.render()) - Line und Filled lassen sich nicht mischen.
    public void renderDebugGrid(ShapeRenderer shapeRenderer) {
        shapeRenderer.setColor(Color.MAGENTA);
        for (int rx = 0; rx <= gridWidth; rx++) {
            for (int ry = 0; ry <= gridHeight; ry++) {
                float drawX = rx * TILE_SIZE - TILE_SIZE / 2f;
                float drawY = ry * TILE_SIZE - TILE_SIZE / 2f;
                shapeRenderer.rect(drawX, drawY, TILE_SIZE, TILE_SIZE);
            }
        }
    }

    // Bitmask-Konvention (muss zu createLookupTexture() passen): bottomLeft = Bit 0 (Wert 1),
    // bottomRight = Bit 1 (Wert 2), topLeft = Bit 2 (Wert 4), topRight = Bit 3 (Wert 8) - ist
    // ein Bit gesetzt, bedeutet das "diese Ecke ist Lücke".
    private int computeMask(int rx, int ry) {
        // Eck-Schnittpunkt (rx, ry) grenzt an genau diese 4 Daten-Zellen
        boolean bottomLeft = isGapAt(rx - 1, ry - 1);
        boolean bottomRight = isGapAt(rx, ry - 1);
        boolean topLeft = isGapAt(rx - 1, ry);
        boolean topRight = isGapAt(rx, ry);

        // "+=" statt des sonst für Bitmasken üblichen "|=" (bitweises ODER) - hier funktional
        // identisch, weil sich die vier Werte (1/2/4/8) nie überschneidende Bits setzen. "|="
        // wäre die konventionellere Schreibweise für Bitmasken-Code und würde die Absicht
        // ("hier wird ein Bit gesetzt", nicht "hier wird ein Zahlenwert aufaddiert") klarer
        // ausdrücken - bewusst bei "+=" belassen, da beide hier zum selben Ergebnis führen.
        int ret = 0;
        if (bottomLeft) { ret += 1; }
        if (bottomRight) { ret += 2; }
        if (topLeft) { ret += 4; }
        if (topRight) { ret += 8; }

        return ret;
    }

    // Platzhalter-Kachel für EINE der 16 Kombinationen: viertelt das Bild und färbt jedes
    // Viertel passend zur jeweiligen Ecke ein (Boden/Lücke) - macht die Bitmask-Logik visuell
    // nachprüfbar, ganz ohne echte Pixelart (echte Kunst ersetzt das später in Quest 7.10)
    private Texture createLookupTexture(int mask) {
        Pixmap pixmap = new Pixmap(TILE_SIZE, TILE_SIZE, Pixmap.Format.RGBA8888);
        int half = TILE_SIZE / 2;

        Color ground = new Color(0.45f, 0.6f, 0.25f, 1f);
        Color gap = new Color(0.25f, 0.45f, 0.75f, 1f);

        // Pixmap-Koordinaten: y=0 ist oben (anders als unser Y-nach-oben-Weltkoordinatensystem!)
        // - "topLeft"/"topRight" landen deshalb bei y=0, "bottomLeft"/"bottomRight" bei y=half
        pixmap.setColor((mask & 4) != 0 ? gap : ground); // topLeft
        pixmap.fillRectangle(0, 0, half, half);
        pixmap.setColor((mask & 8) != 0 ? gap : ground); // topRight
        pixmap.fillRectangle(half, 0, half, half);
        pixmap.setColor((mask & 1) != 0 ? gap : ground); // bottomLeft
        pixmap.fillRectangle(0, half, half, half);
        pixmap.setColor((mask & 2) != 0 ? gap : ground); // bottomRight
        pixmap.fillRectangle(half, half, half, half);

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    public void dispose() {
        for (Texture texture : lookupTextures) {
            texture.dispose();
        }
    }
}
