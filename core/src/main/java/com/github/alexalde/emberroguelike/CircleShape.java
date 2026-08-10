package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.math.Vector2;

// Kreis-Kreis-Check: Abstand der Mittelpunkte gegen die SUMME beider Radien (siehe Shape) -
// deckt z.B. Melee-/AOE-Angriffe und einzelne Projektile ab
public class CircleShape implements Shape {

    private final Vector2 center;
    private final float radius;

    public CircleShape(Vector2 center, float radius) {
        this.center = center;
        this.radius = radius;
    }

    @Override
    public boolean overlapsCircle(Vector2 otherCenter, float otherRadius) {
        return center.dst(otherCenter) <= radius + otherRadius;
    }
}
