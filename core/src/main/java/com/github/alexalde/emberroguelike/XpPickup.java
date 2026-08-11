package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class XpPickup extends Pickup {

    private final float xpValue;
    private final Texture texture;

    public XpPickup(Vector2 position, float xpValue) {
        super(position);
        this.xpValue = xpValue;
        this.texture = new Texture("xp_orb_placeholder.png");
    }

    @Override
    protected void applyEffect(Player player) {
        // Multiplikator auf JEDE eingesammelte XP-Menge (siehe GDD 2.1/Task #54), 1 = unverändert
        player.addXp(xpValue * player.getStats().xpMultiplier);
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
