package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.math.Vector2;

public interface Weapon {
    void update(float deltaTime, Enemy target);
    boolean tryAttack(Vector2 origin, Vector2 direction, Enemy target);
}
