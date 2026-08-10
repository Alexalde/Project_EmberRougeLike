package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

// Gemeinsame Telegraph-Zeichen-Logik (siehe GDD 6/Quest 10, Task #77) - Nutzer-Entscheidung:
// die VOLLE Fläche eines Angriffs wird sofort bei Windup-Start sichtbar (Basisfarbe), keine
// wachsende Größe mehr. Der Windup-Fortschritt wird stattdessen durch eine zweite, ähnliche
// Farbe gezeigt, die sich füllt - bei Kreisen/Korridoren vom Zentrum bzw. der Boss-Seite nach
// außen (Länge/Radius bleibt die Ausweich-Information), bei Beams stattdessen quer zur Linie
// (Länge UND Endpunkt stehen dort schon von Anfang an fest, siehe drawFillingLineWidth). Fünf
// BossAttack-Implementierungen brauchen dieselbe Grund-Logik, deshalb hier extrahiert statt
// fünffach dupliziert (anders als z.B. TELEGRAPH_WIDTH, das pro Muster unterschiedlich bleiben darf).
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

    // Volle Linie in voller BREITE (Basisfarbe) sofort sichtbar, plus eine zweite Farbe, die
    // NICHT in der Länge, sondern in der BREITE wächst (von einer dünnen Mittellinie auf die
    // volle Breite) - Nutzer-Entscheidung für Beams: die Länge (Endpunkt) ist die Ausweich-
    // Information und muss sofort feststehen, der Windup-Fortschritt zeigt sich stattdessen
    // quer zur Linie
    public static void drawFillingLineWidth(ShapeRenderer shapeRenderer, Vector2 start, Vector2 end, float width, float progress) {
        shapeRenderer.setColor(BASE_COLOR);
        shapeRenderer.rectLine(start, end, width);
        shapeRenderer.setColor(FILL_COLOR);
        shapeRenderer.rectLine(start, end, width * progress);
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
