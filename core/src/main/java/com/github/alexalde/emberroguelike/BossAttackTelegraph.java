package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

// Gemeinsame Telegraph-Zeichen-Logik (siehe GDD 6/Quest 10, Task #77) - Nutzer-Entscheidung:
// die VOLLE Fläche eines Angriffs wird sofort bei Windup-Start sichtbar (Basisfarbe), keine
// wachsende Größe mehr. Der Windup-Fortschritt wird stattdessen durch eine zweite, ähnliche
// Farbe gezeigt, die sich von der Boss-Seite (bzw. der Mitte, wenn es keine Boss-Seite gibt,
// siehe raumgebundene Beams) nach außen füllt. Fünf BossAttack-Implementierungen brauchen
// exakt dieselbe Logik, deshalb hier extrahiert statt fünffach dupliziert (anders als z.B.
// TELEGRAPH_WIDTH, das pro Muster bewusst unterschiedlich bleiben darf).
public class BossAttackTelegraph {

    public static final Color BASE_COLOR = Color.YELLOW;
    public static final Color FILL_COLOR = Color.ORANGE;

    private BossAttackTelegraph() {
    }

    // Voller Kreis (Basisfarbe) sofort sichtbar, plus ein vom Zentrum nach außen wachsender
    // Füllkreis (zweite Farbe) - für Melee/AOE, deren Zentrum ohnehin die Boss-Position ist
    public static void drawFillingCircle(ShapeRenderer shapeRenderer, Vector2 center, float radius, float progress) {
        shapeRenderer.setColor(BASE_COLOR);
        shapeRenderer.circle(center.x, center.y, radius);
        shapeRenderer.setColor(FILL_COLOR);
        shapeRenderer.circle(center.x, center.y, radius * progress);
    }

    // Volle Linie (Basisfarbe) sofort sichtbar, plus eine von der MITTE nach BEIDEN Seiten
    // wachsende Füllung (zweite Farbe) - für Muster ohne echte "Boss-Seite" der Linie
    // (raumgebundene Beams, die den Boss für ihre Platzierung bewusst ignorieren)
    public static void drawFillingLineFromCenter(ShapeRenderer shapeRenderer, Vector2 start, Vector2 end, float width, float progress) {
        shapeRenderer.setColor(BASE_COLOR);
        shapeRenderer.rectLine(start, end, width);

        Vector2 mid = start.cpy().add(end).scl(0.5f);
        Vector2 halfDelta = end.cpy().sub(start).scl(0.5f * progress);
        shapeRenderer.setColor(FILL_COLOR);
        shapeRenderer.rectLine(mid.cpy().sub(halfDelta), mid.cpy().add(halfDelta), width);
    }

    // Volle Linie (Basisfarbe) sofort sichtbar, plus eine vom START (nahe am Boss) zum Ende
    // wachsende Füllung (zweite Farbe) - für Muster mit echtem Boss-Ursprung (z.B.
    // Projektil-Korridore), im Gegensatz zu drawFillingLineFromCenter
    public static void drawFillingLineFromStart(ShapeRenderer shapeRenderer, Vector2 start, Vector2 end, float width, float progress) {
        shapeRenderer.setColor(BASE_COLOR);
        shapeRenderer.rectLine(start, end, width);

        Vector2 filledEnd = start.cpy().lerp(end, progress);
        shapeRenderer.setColor(FILL_COLOR);
        shapeRenderer.rectLine(start, filledEnd, width);
    }
}
