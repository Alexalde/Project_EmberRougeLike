package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.math.Vector2;

import java.util.List;

public interface Weapon {
    // "stats" (siehe GDD 2.1/Quest 8) - Waffen verrechnen ihren Basisschaden selbst darüber
    // (statt eine Rückreferenz auf Player zu halten), skaliert auch den Cooldown-Abbau über
    // attackSpeedMultiplier
    void update(float deltaTime, List<Enemy> targets, PlayerStats stats);
    boolean tryAttack(Vector2 origin, Vector2 direction, List<Enemy> targets, PlayerStats stats);

    // Für die Angriffs-Animation des Trägers (siehe Player.determineAnimationState()) - true,
    // solange der Angriff visuell noch "läuft", unabhängig vom Cooldown
    boolean isAttackVisualActive();

    // Fortschritts-Anteil des laufenden Angriffs (0 = gerade erst begonnen, 1 = fertig), siehe GDD 5.3
    float getAttackVisualProgress();

    // Basiswert VOR jeder Stat-Verrechnung (siehe PlayerStats.computeDamage(), GDD 2.1)
    float getBaseDamage();
    DamageType getDamageType();
}
