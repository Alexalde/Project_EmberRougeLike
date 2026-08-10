package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;

// Segment mit Breite (siehe Shape) - deckt BEIDE Beam-Typen ab: PlayerLockedBeamAttack (kurzes,
// am Boss verankertes Segment) UND RoomLockedBeamAttack (Segment über die volle Raumbreite/
// -höhe). Für ein Segment, das den ganzen Raum abdeckt, ist der senkrechte Abstand zu einem
// Punkt INNERHALB des Raums identisch mit einem reinen 1D-Achsen-Check - Intersector.
// distanceSegmentPoint deckt also auch den "raumgebundenen" Fall korrekt ab, kein separater Pfad nötig.
public class LineShape implements Shape {

    private final Vector2 start;
    private final Vector2 end;
    private final float width;

    public LineShape(Vector2 start, Vector2 end, float width) {
        this.start = start;
        this.end = end;
        this.width = width;
    }

    @Override
    public boolean overlapsCircle(Vector2 center, float radius) {
        return Intersector.distanceSegmentPoint(start, end, center) <= width / 2f + radius;
    }
}
