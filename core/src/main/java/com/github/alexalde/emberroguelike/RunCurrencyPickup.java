package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

// Run-lokale Dritt-Währung als Boden-Drop (siehe Player.runCurrency, Task #90) - gedacht für
// spätere In-Run-Shops/Rerolls/Events (siehe ROADMAP-Backlog), aktuell nur als Gegner-Drop
// verdrahtet. Anders als RewardPickup MIT Magnet-Effekt (Pickup.update() unverändert übernommen) -
// verhält sich wie XpPickup/CurrencyPickup: ein häufiger, kleiner Drop, kein bewusst
// aufzusuchender Belohnungs-Gegenstand.
public class RunCurrencyPickup extends Pickup {

    private static final int PLACEHOLDER_SIZE = 10;
    // Kupfer-/Bronze-Ton - unterscheidet sich klar von RewardPickups GOLD und den bestehenden
    // Währungs-/XP-Texturen
    private static final Color PLACEHOLDER_COLOR = new Color(0.8f, 0.5f, 0.15f, 1f);

    private final float amount;
    private final Texture texture;

    public RunCurrencyPickup(Vector2 position, float amount) {
        super(position);
        this.amount = amount;
        this.texture = createPlaceholderTexture();
    }

    private Texture createPlaceholderTexture() {
        Pixmap pixmap = new Pixmap(PLACEHOLDER_SIZE, PLACEHOLDER_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(PLACEHOLDER_COLOR);
        pixmap.fill();
        Texture placeholderTexture = new Texture(pixmap);
        pixmap.dispose();
        return placeholderTexture;
    }

    @Override
    protected void applyEffect(Player player) {
        // Multiplikator hier angewendet, nicht in Player.addRunCurrency() - gleiches Prinzip wie
        // XpPickup.applyEffect()/xpMultiplier
        player.addRunCurrency(amount * player.getStats().runCurrencyMultiplier);
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
