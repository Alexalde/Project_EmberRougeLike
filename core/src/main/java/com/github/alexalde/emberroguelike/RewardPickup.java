package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

// Raum-Clear-Belohnung als Boden-Drop vom letzten besiegten Gegner (siehe GDD 3.1/Quest 8,
// Task #53) statt automatischem Popup beim Raum-Clear - der Spieler muss die Item-Auswahlrunde
// erst einsammeln, bevor sie startet. applyEffect() bleibt bewusst LEER: ItemPool/
// pendingRewardBatches sind Main-Zustand, kein Player-Zustand, deshalb liest Main den
// Sammel-Moment über instanceof aus und stößt die Auswahlrunde selbst an (siehe Main.render(),
// gleiches Muster wie CurrencyPickup/currencyCollected).
public class RewardPickup extends Pickup {

    private static final int PLACEHOLDER_SIZE = 16;

    private final int itemCount;
    private final Texture texture;

    public RewardPickup(Vector2 position, int itemCount) {
        super(position);
        this.itemCount = itemCount;
        this.texture = createPlaceholderTexture();
    }

    public int getItemCount() {
        return itemCount;
    }

    // Bewusst KEIN Magnet-Effekt (Nutzer-Entscheidung, siehe docs/ROADMAP.md-Backlog) - anders
    // als XP-/Ressourcen-Pickups soll eine Raum-Belohnung nicht automatisch angezogen werden,
    // der Spieler muss bewusst zu ihr hingehen. Überschreibt Pickup.update() komplett, statt nur
    // magnetRange auf 0 zu setzen - macht die Absicht am Aufruf-Ort unmissverständlich.
    @Override
    public void update(float deltaTime, Vector2 playerCenter, float magnetRange) {
    }

    // Reiner Laufzeit-Platzhalter (gleiches Prinzip wie Enemy.createFrameTexture()) statt einer
    // Asset-Datei - GOLD als "besondere Belohnung"-Farbe, unterscheidet sich klar von den
    // vorhandenen XP-/Währungs-Icons
    private Texture createPlaceholderTexture() {
        Pixmap pixmap = new Pixmap(PLACEHOLDER_SIZE, PLACEHOLDER_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.GOLD);
        pixmap.fill();
        Texture placeholderTexture = new Texture(pixmap);
        pixmap.dispose();
        return placeholderTexture;
    }

    @Override
    protected void applyEffect(Player player) {
        // Kein direkter Effekt auf den Spieler - siehe Klassenkommentar
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
