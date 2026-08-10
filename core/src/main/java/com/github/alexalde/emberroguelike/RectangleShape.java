package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.math.Vector2;

// Orientiertes Rechteck mit ECHTEN scharfen Ecken (siehe Shape) - Gegensatz zu LineShape, das
// technisch eine Kapsel mit abgerundeten Enden ist (für Beams nicht sichtbar, aber eben kein
// echtes Rechteck). Parametrisiert wie LineShape (Zentrum, Richtung, Länge, Breite) statt
// achsenparallel, damit Angriffe in beliebige Richtungen zeigen können.
//
// Kreis-Rechteck-Check: Kreis-Mittelpunkt ins lokale (unrotierte) Koordinatensystem des
// Rechtecks transformieren, dort auf die Rechteck-Grenzen klemmen (= nächstgelegener Punkt AUF
// dem Rechteck) und den Abstand dorthin gegen den Kreisradius prüfen - Standard-Technik für
// Kreis-vs-orientierte-Box.
public class RectangleShape implements Shape {

    private final Vector2 center;
    // Normalisierte Richtung der LÄNGS-Achse (wie bei LineShape) - definiert die Rotation
    private final Vector2 direction;
    private final float halfLength;
    private final float halfWidth;

    public RectangleShape(Vector2 center, Vector2 direction, float length, float width) {
        this.center = center;
        this.direction = direction.cpy().nor();
        this.halfLength = length / 2f;
        this.halfWidth = width / 2f;
    }

    @Override
    public boolean overlapsCircle(Vector2 otherCenter, float radius) {
        Vector2 offset = otherCenter.cpy().sub(center);

        // Projektion auf die Längs-Achse (= "direction") und die dazu senkrechte Quer-Achse -
        // ergibt die Kreis-Position im lokalen, unrotierten Koordinatensystem des Rechtecks
        float localX = offset.dot(direction);
        float localY = offset.x * -direction.y + offset.y * direction.x;

        // Auf die Rechteck-Grenzen klemmen = nächstgelegener Punkt AUF dem Rechteck
        float closestX = Math.max(-halfLength, Math.min(halfLength, localX));
        float closestY = Math.max(-halfWidth, Math.min(halfWidth, localY));

        float dx = localX - closestX;
        float dy = localY - closestY;
        return dx * dx + dy * dy <= radius * radius;
    }
}
