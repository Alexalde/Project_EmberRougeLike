package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

// Generische Pickup-Klasse für BEIDE Meta-Währungen (siehe MetaCurrencyType, GDD 3.2/Quest 9) -
// anders als XpPickup bewusst KEINE eigene Klasse pro Währung: UPGRADE- und UNLOCK-Pickups
// unterscheiden sich in nichts außer "welcher Zähler wird erhöht", es gibt keinen
// Verhaltensunterschied, der eigene Klassen rechtfertigen würde.
public class CurrencyPickup extends Pickup {

    private final MetaCurrencyType type;
    private final float amount;
    private final Texture texture;

    public CurrencyPickup(Vector2 position, MetaCurrencyType type, float amount) {
        super(position);
        this.type = type;
        this.amount = amount;
        this.texture = new Texture(type == MetaCurrencyType.UPGRADE
            ? "currency_upgrade_placeholder.png"
            : "currency_unlock_placeholder.png");
    }

    @Override
    protected void applyEffect(Player player) {
        player.getMetaProgress().addCurrency(type, amount);
    }

    @Override
    public void draw(SpriteBatch batch) {
        float drawX = MathUtils.round(position.x - texture.getWidth() / 2f);
        float drawY = MathUtils.round(position.y - texture.getHeight() / 2f);
        batch.draw(texture, drawX, drawY);
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
