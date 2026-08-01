package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

// Gemeinsame Basis für Boden-Gegenstände, die der Spieler erst EINSAMMELN muss, um ihre Wirkung
// zu erhalten (Nutzer-Entscheidung, Quest 8: XP soll nicht sofort beim Gegner-Tod vergeben
// werden, sondern als Drop, den man aufsammelt). Bewusst so gebaut, dass später auch 1-2
// Währungen darüber laufen können - eine neue Unterklasse (z.B. CurrencyPickup) reicht dafür,
// kein Umbau von Pickup selbst nötig.
public abstract class Pickup {

    private static final float PICKUP_RADIUS = 12f;

    protected Vector2 position;
    private boolean collected;

    protected Pickup(Vector2 position) {
        this.position = position.cpy();
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
