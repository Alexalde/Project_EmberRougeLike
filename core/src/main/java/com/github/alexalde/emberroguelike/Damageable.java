package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.math.Vector2;

// Alles, was von den Spieler-Waffen (Sword/Bow/Projectile) getroffen und beschädigt werden kann
// (siehe GDD 6/Quest 10) - bewusst NUR die Methoden, die Waffen wirklich brauchen. Enthält
// ABSICHTLICH NICHT isRemovable()/getXpValue()/update()/draw()/dispose() - die bleiben
// Enemy-/Boss-spezifisch, kein Waffen-Anliegen. Enemy implementiert dieses Interface bereits mit
// unveränderten Methodenkörpern (reiner Compile-Zeit-Vertrag); Boss (Quest 10) ist der zweite
// Implementierer, ohne von Enemy zu erben.
//
// isHitByArc()/isHitBy() sind seit der Shape-Abstraktion (Task #85) entfallen - die Trefferzone
// (Kreis/Kegel/Linie) baut jetzt der ANGREIFER als Shape und testet sie selbst gegen
// getCenter()/getHurtboxRadius(), statt dass jede Damageable-Implementierung ihre eigene
// Geometrie-Mathematik dupliziert (gleiches Prinzip wie Player.getHurtboxRadius(), Task #83).
public interface Damageable {
    boolean isAlive();
    void takeDamage(float amount);
    Vector2 getCenter();
    float getHurtboxRadius();
}
