package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.math.Vector2;

import java.util.List;

public interface Weapon {
    void update(float deltaTime, List<Enemy> targets);
    boolean tryAttack(Vector2 origin, Vector2 direction, List<Enemy> targets);
}
