package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;

// Dreieck aus drei Eckpunkten (siehe Shape) - z.B. für einen Sturzangriff, dessen
// Gefahrenbereich sich zuspitzt statt gleichmäßig breit zu sein (LineShape/RectangleShape).
//
// Kreis-Dreieck-Check in zwei Schritten: (1) liegt der Kreis-Mittelpunkt bereits INNERHALB des
// Dreiecks, ist die Überlappung garantiert; sonst (2) kleinster Abstand zu einer der drei Kanten
// gegen den Kreisradius - Standard-Technik für Kreis-vs-konvexes-Polygon (gleiches Prinzip wie
// RectangleShape, nur ohne den Umweg über ein lokales Koordinatensystem).
public class TriangleShape implements Shape {

    private final Vector2 a;
    private final Vector2 b;
    private final Vector2 c;

    public TriangleShape(Vector2 a, Vector2 b, Vector2 c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    @Override
    public boolean overlapsCircle(Vector2 center, float radius) {
        if (containsPoint(center)) {
            return true;
        }
        float distance = Math.min(
            Intersector.distanceSegmentPoint(a, b, center),
            Math.min(Intersector.distanceSegmentPoint(b, c, center), Intersector.distanceSegmentPoint(c, a, center))
        );
        return distance <= radius;
    }

    // Vorzeichen der Kreuzprodukte über alle drei Kanten - liegt "point" bei jeder Kante auf
    // derselben Seite (unabhängig vom Umlaufsinn a->b->c), liegt er innerhalb des Dreiecks
    private boolean containsPoint(Vector2 point) {
        float d1 = cross(a, b, point);
        float d2 = cross(b, c, point);
        float d3 = cross(c, a, point);

        boolean hasNegative = d1 < 0 || d2 < 0 || d3 < 0;
        boolean hasPositive = d1 > 0 || d2 > 0 || d3 > 0;
        return !(hasNegative && hasPositive);
    }

    private float cross(Vector2 edgeStart, Vector2 edgeEnd, Vector2 point) {
        return (edgeEnd.x - edgeStart.x) * (point.y - edgeStart.y) - (edgeEnd.y - edgeStart.y) * (point.x - edgeStart.x);
    }
}
