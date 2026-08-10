package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.math.Vector2;

// Kegel/Sektor mit Reichweite (siehe Shape) - deckt den Schwert-Schwung ab (siehe Sword). Erwartet
// "direction" bereits NORMALISIERT (gleiche Annahme wie die bisherige Damageable.isHitByArc-
// Implementierung, siehe Player.update() - dort wird vor jedem Waffen-Aufruf normalisiert).
public class ArcShape implements Shape {

    private final Vector2 origin;
    private final Vector2 direction;
    private final float range;
    private final float halfAngleDegrees;

    public ArcShape(Vector2 origin, Vector2 direction, float range, float halfAngleDegrees) {
        this.origin = origin;
        this.direction = direction;
        this.range = range;
        this.halfAngleDegrees = halfAngleDegrees;
    }

    @Override
    public boolean overlapsCircle(Vector2 center, float radius) {
        Vector2 toCenter = center.cpy().sub(origin);
        float distance = toCenter.len();

        if (distance > range + radius) {
            return false;
        }
        if (distance == 0) {
            return true;
        }

        toCenter.nor();
        float minDot = (float) Math.cos(Math.toRadians(halfAngleDegrees));
        return direction.dot(toCenter) >= minDot;
    }
}
