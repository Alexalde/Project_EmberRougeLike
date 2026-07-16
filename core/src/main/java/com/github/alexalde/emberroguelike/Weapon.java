package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.math.Vector2;

import java.util.List;

public interface Weapon {
    void update(float deltaTime, List<Enemy> targets);
    boolean tryAttack(Vector2 origin, Vector2 direction, List<Enemy> targets);

    // Für die Angriffs-Animation des Trägers (siehe Player.determineAnimationState()) - true,
    // solange der Angriff visuell noch "läuft", unabhängig vom Cooldown
    boolean isAttackVisualActive();

    // Fortschritts-Anteil des laufenden Angriffs (0 = gerade erst begonnen, 1 = fertig), siehe GDD 5.3
    float getAttackVisualProgress();
}
