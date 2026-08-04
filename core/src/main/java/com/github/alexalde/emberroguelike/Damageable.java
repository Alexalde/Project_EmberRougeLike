package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.math.Vector2;

// Alles, was von den Spieler-Waffen (Sword/Bow/Projectile) getroffen und beschädigt werden kann
// (siehe GDD 6/Quest 10) - bewusst NUR die Methoden, die Waffen wirklich brauchen. Enthält
// ABSICHTLICH NICHT isRemovable()/getXpValue()/update()/draw()/dispose() - die bleiben
// Enemy-/Boss-spezifisch, kein Waffen-Anliegen. Enemy implementiert dieses Interface bereits mit
// unveränderten Methodenkörpern (reiner Compile-Zeit-Vertrag); Boss (Quest 10) ist der zweite
// Implementierer, ohne von Enemy zu erben.
public interface Damageable {
    boolean isAlive();
    void takeDamage(float amount);
    Vector2 getCenter();
    boolean isHitByArc(Vector2 origin, Vector2 direction, float range, float halfAngleDegrees);
    boolean isHitBy(Vector2 point, float otherRadius);
}
