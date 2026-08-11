package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import java.util.List;

// Bildschirmfest verankerte Boss-Healthbar(s) (siehe Task #89) - bewusst NICHT Teil von Boss
// selbst (Gegensatz zur ersten Version, die über der Boss-Position in Welt-Koordinaten schwebte
// und deshalb je nach Kamera-Ausschnitt unsichtbar sein konnte). Liegt stattdessen unten
// zentriert über die uiCamera, wie Healthbar (Spieler) - unabhängig von der scrollenden
// Welt-Kamera.
//
// Nimmt bewusst eine LISTE von Bossen entgegen statt eines einzelnen (aktuell unterstützt Main
// ohnehin nur einen gleichzeitigen Boss) - Nutzer-Wunsch: spätere Boss-Konzepte mit mehreren
// gleichzeitig aktiven Bossen (z.B. phasenweise) sollen ihre Leisten einfach nebeneinander
// bekommen, ohne dass diese Klasse dafür umgebaut werden muss. Instanziiert statt statisch,
// obwohl aktuell zustandslos - sobald echte Grafik das ersetzt (siehe Klassenkommentar
// Healthbar), braucht das hier vermutlich geladene Texturen wie dort auch.
public class BossHealthbarUI {

    private static final float BAR_HEIGHT = 16f;
    private static final float BOTTOM_MARGIN = 12f;
    // Lässt an beiden Seiten etwas Rand statt wirklich bis zum Bildrand zu reichen - "fast die
    // ganze Screenlänge", kein hartes 0-bis-WORLD_WIDTH
    private static final float SIDE_MARGIN = 40f;
    private static final float GAP_BETWEEN_BARS = 12f;

    // Erwartet ein bereits laufendes ShapeRenderer.begin(ShapeType.Filled) (siehe Main.render())
    public void draw(ShapeRenderer shapeRenderer, List<Boss> bosses) {
        if (bosses.isEmpty()) {
            return;
        }

        float totalWidth = GameConfig.WORLD_WIDTH - 2 * SIDE_MARGIN;
        int count = bosses.size();
        float barWidth = (totalWidth - (count - 1) * GAP_BETWEEN_BARS) / count;

        for (int i = 0; i < count; i++) {
            float barX = SIDE_MARGIN + i * (barWidth + GAP_BETWEEN_BARS);
            drawSingleBar(shapeRenderer, bosses.get(i), barX, barWidth);
        }
    }

    private void drawSingleBar(ShapeRenderer shapeRenderer, Boss boss, float barX, float barWidth) {
        float healthPercent = MathUtils.clamp(boss.getHealth() / boss.getMaxHealth(), 0f, 1f);

        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(barX, BOTTOM_MARGIN, barWidth, BAR_HEIGHT);

        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(barX, BOTTOM_MARGIN, barWidth * healthPercent, BAR_HEIGHT);
    }
}
