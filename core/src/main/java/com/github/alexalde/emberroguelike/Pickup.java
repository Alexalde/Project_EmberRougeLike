package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

// Gemeinsame Basis für Boden-Gegenstände, die der Spieler erst EINSAMMELN muss, um ihre Wirkung
// zu erhalten (Nutzer-Entscheidung, Quest 8: XP soll nicht sofort beim Gegner-Tod vergeben
// werden, sondern als Drop, den man aufsammelt). Bewusst so gebaut, dass später auch 1-2
// Währungen darüber laufen können - eine neue Unterklasse (z.B. CurrencyPickup) reicht dafür,
// kein Umbau von Pickup selbst nötig.
public abstract class Pickup {

    // Kontakt-Radius für das tatsächliche Einsammeln - klein und fest, im Gegensatz zum viel
    // größeren, veränderbaren Magnet-Radius (siehe update()/PlayerStats.pickupRange). Sobald ein
    // Pickup vom Magneten nah genug herangezogen wurde, greift dieser Radius ganz von selbst.
    private static final float PICKUP_RADIUS = 12f;
    // Fluggeschwindigkeit während des Magnet-Effekts (Einheiten/Sekunde)
    private static final float MAGNET_SPEED = 300f;

    protected Vector2 position;
    private boolean collected;

    protected Pickup(Vector2 position) {
        this.position = position.cpy();
    }

    // Fliegt Richtung Spieler, sobald dieser innerhalb von magnetRange ist (siehe
    // PlayerStats.pickupRange, Nutzer-Entscheidung: verhindert unerreichbar liegenbleibende
    // Drops UND fühlt sich fürs Einsammeln besser an). Läuft nur, solange noch nicht eingesammelt.
    public void update(float deltaTime, Vector2 playerCenter, float magnetRange) {
        if (collected) {
            return;
        }

        float distance = position.dst(playerCenter);
        if (distance > 0 && distance <= magnetRange) {
            Vector2 direction = playerCenter.cpy().sub(position).nor();
            float step = Math.min(MAGNET_SPEED * deltaTime, distance);
            position.add(direction.scl(step));
        }
    }

    public boolean isInRange(Vector2 playerCenter) {
        return !collected && position.dst(playerCenter) <= PICKUP_RADIUS;
    }

    public boolean isCollected() {
        return collected;
    }

    // Template-Method: markiert sich selbst als eingesammelt, die eigentliche Wirkung (z.B. XP
    // gutschreiben) übernimmt applyEffect() in der jeweiligen Unterklasse. Bewusst OHNE
    // Rückgabewert - Main erkennt z.B. ausgelöste Level-Ups separat über player.getLevel() vor/
    // nach dem Einsammeln, statt dass diese Basisklasse XP-spezifisches Wissen bräuchte.
    public void collect(Player player) {
        applyEffect(player);
        collected = true;
    }

    protected abstract void applyEffect(Player player);

    public abstract void draw(SpriteBatch batch);

    public abstract void dispose();
}
