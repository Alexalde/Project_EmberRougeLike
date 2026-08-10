package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.math.Vector2;

// Generische Trefferzone für JEDE spiellogische Treffer-Erkennung im Spiel (siehe Task #85) -
// sowohl Spieler-Waffen gegen Enemy/Boss (Sword/Projectile, siehe Damageable) als auch Boss-
// Angriffe gegen den Spieler (siehe BossAttack-Implementierungen). EIN einziger Kontaktpunkt
// reicht als Vertrag: alle Hurtboxen im Spiel (Player/Enemy/Boss) sind Kreise, egal wie komplex
// die ANGREIFENDE Form selbst ist - deshalb testet jede Shape nur gegen einen Kreis, statt
// Shape-gegen-Shape allgemein zu lösen (unnötige Komplexität für Formen, die es hier nicht gibt).
public interface Shape {
    boolean overlapsCircle(Vector2 center, float radius);
}
