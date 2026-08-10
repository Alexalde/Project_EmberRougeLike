package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.math.Vector2;

import java.util.List;

// ODER-Verknüpfung beliebig vieler Shapes (siehe Shape) - deckt komplexere Formen ab, die sich
// aus einfachen Grundformen zusammensetzen (z.B. ein späteres gestaggertes Beam-Gitter aus
// mehreren LineShapes), ohne dass jede neue Kombination eine eigene Shape-Klasse bräuchte.
public class CompositeShape implements Shape {

    private final List<Shape> shapes;

    public CompositeShape(List<Shape> shapes) {
        this.shapes = shapes;
    }

    @Override
    public boolean overlapsCircle(Vector2 center, float radius) {
        for (Shape shape : shapes) {
            if (shape.overlapsCircle(center, radius)) {
                return true;
            }
        }
        return false;
    }
}
