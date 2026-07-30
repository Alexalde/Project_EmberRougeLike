package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

// Dual-Grid-Rendering-System (siehe ROADMAP Quest 7): das Render-Gitter liegt um eine halbe
// Kachel gegen das Daten-Gitter versetzt. Jede Render-Kachel wird aus den 4 angrenzenden
// Daten-Zellen bestimmt (2^4 = 16 Kombinationen) - deutlich weniger Kachel-Grafiken als beim
// klassischen Blob-Autotiling (bis zu 47), und kein natives Tiled-Feature, daher ein eigener
// Rendering-Layer statt eines Tiled-"Wang Sets".
//
// Bewusst generisch gehalten (umbenannt von "DualGridTerrainRenderer", seit Wände dieselbe
// Technik nutzen, siehe ROADMAP 2026-07-19): eine Instanz pro unabhängigem Daten-Gitter + Kunst
// (z.B. eine für Boden/Wasser, eine zweite, komplett getrennte für Wände) - "isSpecialCell"
// bedeutet je nach Einsatzzweck etwas anderes (Lücke beim Boden, Wand bei Wänden), das
// Bitmask-Rendering selbst ist davon unabhängig.
//
// Echte Kunst (seit Quest 7.10): EINE Aseprite-Datei, 128x128 pro Frame - jeder Frame ist ein
// KOMPLETTES Bild mit allen 16 Kacheln in einer festen 4x4-Matrix (Zeile = mask/4, Spalte =
// mask%4), keine einzelnen Tags pro Kachel. Alle 16 Kacheln animieren dadurch automatisch
// synchron zueinander (z.B. eine gemeinsame Wasser-Ripple-Animation), nicht unabhängig
// versetzt - siehe update()/render().
public class DualGridRenderer {

    private static final int TILE_SIZE = 32;

    // true = "besonderer" Zellentyp (z.B. Lücke/Wasser beim Boden-Terrain, Wand beim
    // Wand-Terrain) - die Bedeutung hängt vom jeweiligen Einsatzzweck ab
    private final boolean[][] isSpecialCell;
    private final int gridWidth;
    private final int gridHeight;

    private final AsepriteSpriteSheet spriteSheet;
    private final Animation[] lookupAnimations;
    // Frei laufender, gemeinsamer Zeit-Akkumulator für ALLE 16 Kacheln - sorgt dafür, dass sie
    // synchron animieren statt jede für sich unabhängig
    private float elapsedTime;
    // true, wenn dieses Tileset die "besonders"/"nicht besonders"-Viertel pro Kachel vertauscht
    // gezeichnet hat (z.B. walls_dualgrid.png, siehe ROADMAP 2026-07-20) - betrifft NUR, welche
    // der 16 Kachel-Grafiken beim Rendern ausgewählt wird (render() nutzt dann 15-mask statt
    // mask). isSpecialCell/isSpecialAt() selbst bleiben davon unberührt und behalten ihre echte
    // Bedeutung ("true = besonderer Zellentyp") - wichtig, weil Room.isBlocked() (Sidequest 1)
    // dieselben Daten für Kollision wiederverwendet und dafür die UNVERFÄLSCHTEN Werte braucht.
    private final boolean invertMaskLookup;

    public DualGridRenderer(boolean[][] isSpecialCell, String pngPath, String jsonPath) {
        this(isSpecialCell, pngPath, jsonPath, false);
    }

    public DualGridRenderer(boolean[][] isSpecialCell, String pngPath, String jsonPath, boolean invertMaskLookup) {
        this.isSpecialCell = isSpecialCell;
        this.gridWidth = isSpecialCell.length;
        this.gridHeight = isSpecialCell[0].length;
        this.invertMaskLookup = invertMaskLookup;

        this.spriteSheet = new AsepriteSpriteSheet(pngPath, jsonPath);
        TextureRegion[] sheetFrames = spriteSheet.getAllFrameRegions(); // je ein komplettes 128x128-Bild
        float[] sheetDurations = spriteSheet.getAllFrameDurations();

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
    private boolean isSpecialAt(int x, int y) {
        int clampedX = MathUtils.clamp(x, 0, gridWidth - 1);
        int clampedY = MathUtils.clamp(y, 0, gridHeight - 1);
        return isSpecialCell[clampedX][clampedY];
    }

    // Für Kollisionsabfragen (siehe Room.isBlocked(), Sidequest 1) - wandelt eine WELT-Koordinate
    // (Pixel) in die zugehörige Daten-Gitter-Zelle um. Das Daten-Gitter selbst ist NICHT versetzt
    // (nur das Render-Gitter ist eine halbe Kachel verschoben, siehe render()) - eine einfache
    // Division durch TILE_SIZE reicht, dieselbe Clamp-to-Edge-Logik wie beim Rendering greift
    // automatisch über isSpecialAt(). Keine eigene Kollisions-Datenkopie nötig.
    public boolean isSpecialAtWorld(float worldX, float worldY) {
        int gridX = (int) (worldX / TILE_SIZE);
        int gridY = (int) (worldY / TILE_SIZE);
        return isSpecialAt(gridX, gridY);
    }

    public void render(Batch batch) {
        // Render-Gitter ist in jeder Achse eine Kachel GRÖSSER als das Daten-Gitter (<=, nicht <)
        // - eine Render-Kachel pro Eck-Schnittpunkt des Daten-Gitters, siehe computeMask()
        for (int rx = 0; rx <= gridWidth; rx++) {
            for (int ry = 0; ry <= gridHeight; ry++) {
                int mask = computeMask(rx, ry);
                // Komplementär-Kachel (15-mask) statt der "echten" Maske, falls dieses Tileset
                // die Viertel vertauscht gezeichnet hat (siehe invertMaskLookup oben) - wirkt
                // rein auf die Bild-Auswahl, NICHT auf die Kollisions-/Bitmask-Daten selbst
                int lookupIndex = invertMaskLookup ? 15 - mask : mask;
                // Versatz um eine halbe Kachel: die Render-Kachel liegt zentriert auf dem
                // Eck-Schnittpunkt (rx, ry) des Daten-Gitters, nicht auf einer Daten-Zelle selbst
                float drawX = rx * TILE_SIZE - TILE_SIZE / 2f;
                float drawY = ry * TILE_SIZE - TILE_SIZE / 2f;
                TextureRegion frame = lookupAnimations[lookupIndex].getFrameByElapsedTime(elapsedTime);
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
    // topRight = Bit 3 (Wert 8) - ist ein Bit gesetzt, bedeutet das "diese Ecke ist der
    // besondere Zellentyp" (Lücke bzw. Wand, je nach Einsatzzweck).
    private int computeMask(int rx, int ry) {
        // Eck-Schnittpunkt (rx, ry) grenzt an genau diese 4 Daten-Zellen
        boolean bottomLeft = isSpecialAt(rx - 1, ry - 1);
        boolean bottomRight = isSpecialAt(rx, ry - 1);
        boolean topLeft = isSpecialAt(rx - 1, ry);
        boolean topRight = isSpecialAt(rx, ry);

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
        spriteSheet.dispose();
    }
}
