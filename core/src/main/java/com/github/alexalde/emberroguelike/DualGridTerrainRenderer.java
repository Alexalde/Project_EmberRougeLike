package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
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
// Echte Kunst (seit Quest 7.10): EINE Aseprite-Datei, 128x128 pro Frame - jeder Frame ist ein
// KOMPLETTES Bild mit allen 16 Kacheln in einer festen 4x4-Matrix (Zeile = mask/4, Spalte =
// mask%4), keine einzelnen Tags pro Kachel. Alle 16 Kacheln animieren dadurch automatisch
// synchron zueinander (z.B. eine gemeinsame Wasser-Ripple-Animation), nicht unabhängig
// versetzt - siehe update()/render().
public class DualGridTerrainRenderer {

    private static final int TILE_SIZE = 32;

    // true = Lücke (z.B. Wasser/Loch), false = Boden
    private final boolean[][] isGap;
    private final int gridWidth;
    private final int gridHeight;

    private final AsepriteSpriteSheet terrainSpriteSheet;
    private final Animation[] lookupAnimations;
    // Frei laufender, gemeinsamer Zeit-Akkumulator für ALLE 16 Kacheln - sorgt dafür, dass sie
    // synchron animieren statt jede für sich unabhängig
    private float elapsedTime;

    public DualGridTerrainRenderer(boolean[][] isGap) {
        this.isGap = isGap;
        this.gridWidth = isGap.length;
        this.gridHeight = isGap[0].length;

        this.terrainSpriteSheet = new AsepriteSpriteSheet("terrain_dualgrid.png", "terrain_dualgrid.json");
        TextureRegion[] sheetFrames = terrainSpriteSheet.getAllFrameRegions(); // je ein komplettes 128x128-Bild
        float[] sheetDurations = terrainSpriteSheet.getAllFrameDurations();

        this.lookupAnimations = new Animation[16];
        for (int mask = 0; mask < 16; mask++) {
            int col = mask % 4;
            int row = mask / 4;

            TextureRegion[] maskFrames = new TextureRegion[sheetFrames.length];
            for (int frameIndex = 0; frameIndex < sheetFrames.length; frameIndex++) {
                // Unter-Bereich INNERHALB des jeweiligen Sheet-Frames herausschneiden
                maskFrames[frameIndex] = new TextureRegion(sheetFrames[frameIndex], col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
            lookupAnimations[mask] = new Animation(maskFrames, sheetDurations);
        }
    }

    // Läuft unabhängig von render() - siehe Room.update()/Main.render()
    public void update(float deltaTime) {
        elapsedTime += deltaTime;
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
                TextureRegion frame = lookupAnimations[mask].getFrameByElapsedTime(elapsedTime);
                batch.draw(frame, drawX, drawY, TILE_SIZE, TILE_SIZE);
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

    // Bitmask-Konvention (muss zur Kachel-Anordnung im Tileset passen, siehe ART_SPEC.md):
    // bottomLeft = Bit 0 (Wert 1), bottomRight = Bit 1 (Wert 2), topLeft = Bit 2 (Wert 4),
    // topRight = Bit 3 (Wert 8) - ist ein Bit gesetzt, bedeutet das "diese Ecke ist Lücke".
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

    public void dispose() {
        terrainSpriteSheet.dispose();
    }
}
