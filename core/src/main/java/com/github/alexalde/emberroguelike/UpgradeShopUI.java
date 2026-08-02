package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;

import java.util.List;

// Kauf-Bildschirm für permanente Upgrades im Hub-Terminal (siehe GDD 3.2/Quest 9) - anders als
// RewardChoiceUI KEINE Einmal-Auswahl aus 1-von-N: zeigt ALLE verfügbaren Upgrades gleichzeitig,
// bleibt offen bis der Spieler sie schließt, erlaubt mehrere Käufe nacheinander. Maus+Tastatur
// gleichberechtigt wie bei RewardChoiceUI (gleiches Prinzip: Hover schreibt denselben
// selectedIndex wie Pfeiltasten-Navigation) - schließen geht ebenfalls über beide Wege (Escape/
// interactKey ODER Klick auf den "X"-Button oben rechts).
public class UpgradeShopUI {

    private static final float CARD_WIDTH = 140f;
    private static final float CARD_HEIGHT = 160f;
    private static final float CARD_GAP = 16f;
    private static final float TEXT_PADDING = 10f;
    private static final float CLOSE_BUTTON_SIZE = 24f;

    private final PermanentUpgradePool upgradePool;
    private final MetaProgress metaProgress;
    private final Rectangle[] cardBounds;
    private final Rectangle closeButtonBounds;
    private int selectedIndex;
    // null, solange kein Kauf angefragt wurde - Main verarbeitet ihn (Guthaben abziehen,
    // anwenden, speichern) und liest ihn über consumePurchaseRequest() aus
    private PermanentUpgrade purchaseRequest;
    private boolean closeRequested;
    private boolean closeButtonHovered;

    public UpgradeShopUI(PermanentUpgradePool upgradePool, MetaProgress metaProgress) {
        this.upgradePool = upgradePool;
        this.metaProgress = metaProgress;
        this.selectedIndex = 0;

        List<PermanentUpgrade> upgrades = upgradePool.getAllUpgrades();
        this.cardBounds = new Rectangle[upgrades.size()];
        float totalWidth = upgrades.size() * CARD_WIDTH + (upgrades.size() - 1) * CARD_GAP;
        float startX = (GameConfig.WORLD_WIDTH - totalWidth) / 2f;
        float cardY = (GameConfig.WORLD_HEIGHT - CARD_HEIGHT) / 2f;

        for (int i = 0; i < upgrades.size(); i++) {
            float cardX = startX + i * (CARD_WIDTH + CARD_GAP);
            cardBounds[i] = new Rectangle(cardX, cardY, CARD_WIDTH, CARD_HEIGHT);
        }

        // Oben rechts über der Karten-Reihe - Maus steuert den Shop ohnehin schon (Hover/Klick auf
        // Karten), ein reiner Tastatur-Ausstieg (Escape/interactKey) wäre da inkonsequent
        this.closeButtonBounds = new Rectangle(
            startX + totalWidth - CLOSE_BUTTON_SIZE, cardY + CARD_HEIGHT + 8f, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE
        );
    }

    // "uiMousePosition" ist bereits in uiCamera-Koordinaten umgerechnet (siehe Main.render())
    public void update(Vector2 uiMousePosition) {
        closeButtonHovered = closeButtonBounds.contains(uiMousePosition);
        if (closeButtonHovered && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            closeRequested = true;
        }

        for (int i = 0; i < cardBounds.length; i++) {
            if (cardBounds[i].contains(uiMousePosition)) {
                selectedIndex = i;
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    tryRequestPurchase(i);
                }
                break;
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            selectedIndex = (selectedIndex - 1 + cardBounds.length) % cardBounds.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            selectedIndex = (selectedIndex + 1) % cardBounds.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            tryRequestPurchase(selectedIndex);
        }

        // Sowohl Escape als auch der Interaktions-Taste selbst schließen den Shop wieder (erneut
        // "E" drücken = raus, kein separater Muskel nötig)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(GameSettings.interactKey)) {
            closeRequested = true;
        }
    }

    // Verhindert unnötige Kauf-Anfragen für bereits gekaufte oder nicht leistbare Upgrades direkt
    // hier - Main prüft beim Verarbeiten trotzdem nochmal (siehe MetaProgress.spendCurrency())
    private void tryRequestPurchase(int index) {
        PermanentUpgrade upgrade = upgradePool.getAllUpgrades().get(index);
        boolean alreadyOwned = metaProgress.purchasedUpgradeIds.contains(upgrade.getId());
        boolean canAfford = metaProgress.getCurrency(upgrade.getCurrencyType()) >= upgrade.getCost();

        if (!alreadyOwned && canAfford) {
            purchaseRequest = upgrade;
        }
    }

    // null, solange kein (gültiger) Kauf angefragt wurde - liefert die Anfrage genau einmal
    // zurück (danach wieder null), damit Main sie nicht versehentlich mehrfach verarbeitet
    public PermanentUpgrade consumePurchaseRequest() {
        PermanentUpgrade request = purchaseRequest;
        purchaseRequest = null;
        return request;
    }

    public boolean isCloseRequested() {
        return closeRequested;
    }

    // Erwartet ein bereits laufendes ShapeRenderer.begin(ShapeType.Filled) (siehe Main.render())
    public void renderCards(ShapeRenderer shapeRenderer) {
        List<PermanentUpgrade> upgrades = upgradePool.getAllUpgrades();
        for (int i = 0; i < cardBounds.length; i++) {
            PermanentUpgrade upgrade = upgrades.get(i);
            Rectangle bounds = cardBounds[i];

            Color color;
            if (metaProgress.purchasedUpgradeIds.contains(upgrade.getId())) {
                color = Color.DARK_GRAY;
            } else if (metaProgress.getCurrency(upgrade.getCurrencyType()) < upgrade.getCost()) {
                color = Color.RED;
            } else {
                color = i == selectedIndex ? Color.GOLD : Color.GREEN;
            }

            shapeRenderer.setColor(color);
            shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        shapeRenderer.setColor(closeButtonHovered ? Color.GOLD : Color.GRAY);
        shapeRenderer.rect(closeButtonBounds.x, closeButtonBounds.y, closeButtonBounds.width, closeButtonBounds.height);
    }

    // Erwartet ein bereits laufendes SpriteBatch.begin() (siehe Main.render())
    public void renderText(SpriteBatch batch, BitmapFont font) {
        List<PermanentUpgrade> upgrades = upgradePool.getAllUpgrades();
        for (int i = 0; i < cardBounds.length; i++) {
            PermanentUpgrade upgrade = upgrades.get(i);
            Rectangle bounds = cardBounds[i];

            boolean owned = metaProgress.purchasedUpgradeIds.contains(upgrade.getId());
            String statusText = owned ? "Gekauft" : String.format("%.0f", upgrade.getCost());
            String text = upgrade.getDescription() + "\n" + statusText;

            float textY = bounds.y + bounds.height - TEXT_PADDING;
            font.draw(batch, text, bounds.x + TEXT_PADDING, textY, bounds.width - 2 * TEXT_PADDING, Align.center, true);
        }

        font.draw(
            batch, "X", closeButtonBounds.x, closeButtonBounds.y + closeButtonBounds.height - 4f,
            closeButtonBounds.width, Align.center, false
        );
    }
}
