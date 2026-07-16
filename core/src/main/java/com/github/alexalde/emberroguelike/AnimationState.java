package com.github.alexalde.emberroguelike;

// Rein visueller Zustand, unabhängig von Gameplay-Zustandsmaschinen wie PlayerState/EnemyState
// (siehe deren Kommentare) - wird jeden Frame frisch aus vorhandenen Signalen (Bewegung,
// Angriffs-/Schadens-Timer, Leben) per Prioritätsreihenfolge in determineAnimationState()
// abgeleitet, nicht selbst gesetzt.
public enum AnimationState {
    IDLE,
    WALK,
    ATTACK,
    HURT,
    DEATH
}
