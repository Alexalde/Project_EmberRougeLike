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

// Per Tastendruck ein-/ausblendbares Debug-Werkzeug (siehe DebugValue) - anders als
// RewardChoiceUI/UpgradeShopUI eine VERTIKALE Liste statt nebeneinanderliegender Karten (zu viele
// Einträge für eine Reihe). Jede Zeile hat einen [-]/[+]-Button, dazu ein globaler
// "Alles zurücksetzen"-Button und ein "X"-Schließen-Button - schließen geht zusätzlich über
// Escape und die Menü-Toggle-Taste selbst (siehe Main.render()).
public class DebugMenuUI {

    private static final float PANEL_WIDTH = 340f;
    private static final float ROW_HEIGHT = 20f;
    private static final float TITLE_HEIGHT = 20f;
    private static final float BOTTOM_BAR_HEIGHT = 24f;
    private static final float BUTTON_WIDTH = 20f;
    private static final float BUTTON_HEIGHT = 18f;
    private static final float PADDING = 8f;
    private static final float RESET_BUTTON_WIDTH = 140f;

    private final List<DebugValue> values;
    private final Runnable onValueChanged;
    private final Runnable onResetAll;

    private final Rectangle panelBounds;
    private final Rectangle closeButtonBounds;
    private final Rectangle resetAllButtonBounds;
    private final Rectangle[] decrementBounds;
    private final Rectangle[] incrementBounds;

    private boolean closeButtonHovered;
    private boolean resetAllButtonHovered;
    private final boolean[] decrementHovered;
    private final boolean[] incrementHovered;
    private boolean closeRequested;

    public DebugMenuUI(List<DebugValue> values, Runnable onValueChanged, Runnable onResetAll) {
        this.values = values;
        this.onValueChanged = onValueChanged;
        this.onResetAll = onResetAll;

        float panelHeight = TITLE_HEIGHT + values.size() * ROW_HEIGHT + BOTTOM_BAR_HEIGHT + 3 * PADDING;
        float panelX = (GameConfig.WORLD_WIDTH - PANEL_WIDTH) / 2f;
        float panelY = (GameConfig.WORLD_HEIGHT - panelHeight) / 2f;
        this.panelBounds = new Rectangle(panelX, panelY, PANEL_WIDTH, panelHeight);

        float titleY = panelY + panelHeight - TITLE_HEIGHT;
        this.closeButtonBounds = new Rectangle(
            panelX + PANEL_WIDTH - PADDING - BUTTON_WIDTH, titleY + (TITLE_HEIGHT - BUTTON_HEIGHT) / 2f, BUTTON_WIDTH, BUTTON_HEIGHT
        );

        this.decrementBounds = new Rectangle[values.size()];
        this.incrementBounds = new Rectangle[values.size()];
        this.decrementHovered = new boolean[values.size()];
        this.incrementHovered = new boolean[values.size()];

        for (int i = 0; i < values.size(); i++) {
            float rowY = titleY - (i + 1) * ROW_HEIGHT;
            float buttonY = rowY + (ROW_HEIGHT - BUTTON_HEIGHT) / 2f;
            incrementBounds[i] = new Rectangle(panelX + PANEL_WIDTH - PADDING - BUTTON_WIDTH, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT);
            decrementBounds[i] = new Rectangle(incrementBounds[i].x - 4f - BUTTON_WIDTH, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT);
        }

        this.resetAllButtonBounds = new Rectangle(
            panelX + (PANEL_WIDTH - RESET_BUTTON_WIDTH) / 2f, panelY + PADDING, RESET_BUTTON_WIDTH, BOTTOM_BAR_HEIGHT - PADDING
        );
    }

    // "uiMousePosition" ist bereits in uiCamera-Koordinaten umgerechnet (siehe Main.render())
    public void update(Vector2 uiMousePosition) {
        boolean leftClick = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);

        closeButtonHovered = closeButtonBounds.contains(uiMousePosition);
        if (closeButtonHovered && leftClick) {
            closeRequested = true;
        }

        resetAllButtonHovered = resetAllButtonBounds.contains(uiMousePosition);
        if (resetAllButtonHovered && leftClick) {
            onResetAll.run();
        }

        for (int i = 0; i < values.size(); i++) {
            decrementHovered[i] = decrementBounds[i].contains(uiMousePosition);
            incrementHovered[i] = incrementBounds[i].contains(uiMousePosition);

            if (leftClick && decrementHovered[i]) {
                values.get(i).decrement();
                onValueChanged.run();
            } else if (leftClick && incrementHovered[i]) {
                values.get(i).increment();
                onValueChanged.run();
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            closeRequested = true;
        }
    }

    public boolean isCloseRequested() {
        return closeRequested;
    }

    // Erwartet ein bereits laufendes ShapeRenderer.begin(ShapeType.Filled) (siehe Main.render())
    public void renderRows(ShapeRenderer shapeRenderer) {
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(panelBounds.x, panelBounds.y, panelBounds.width, panelBounds.height);

        shapeRenderer.setColor(closeButtonHovered ? Color.GOLD : Color.GRAY);
        shapeRenderer.rect(closeButtonBounds.x, closeButtonBounds.y, closeButtonBounds.width, closeButtonBounds.height);

        for (int i = 0; i < values.size(); i++) {
            Rectangle dec = decrementBounds[i];
            Rectangle inc = incrementBounds[i];

            shapeRenderer.setColor(decrementHovered[i] ? Color.GOLD : Color.GRAY);
            shapeRenderer.rect(dec.x, dec.y, dec.width, dec.height);

            shapeRenderer.setColor(incrementHovered[i] ? Color.GOLD : Color.GRAY);
            shapeRenderer.rect(inc.x, inc.y, inc.width, inc.height);
        }

        shapeRenderer.setColor(resetAllButtonHovered ? Color.GOLD : Color.RED);
        shapeRenderer.rect(resetAllButtonBounds.x, resetAllButtonBounds.y, resetAllButtonBounds.width, resetAllButtonBounds.height);
    }

    // Erwartet ein bereits laufendes SpriteBatch.begin() (siehe Main.render())
    public void renderText(SpriteBatch batch, BitmapFont font) {
        font.draw(batch, "DEBUG-MENU", panelBounds.x, panelBounds.y + panelBounds.height - 4f, panelBounds.width, Align.center, false);
        font.draw(batch, "X", closeButtonBounds.x, closeButtonBounds.y + closeButtonBounds.height - 3f, closeButtonBounds.width, Align.center, false);

        for (int i = 0; i < values.size(); i++) {
            DebugValue value = values.get(i);
            Rectangle dec = decrementBounds[i];
            Rectangle inc = incrementBounds[i];

            String text = value.getLabel() + ": " + value.getFormattedValue();
            font.draw(batch, text, panelBounds.x + PADDING, dec.y + dec.height - 3f, dec.x - panelBounds.x - PADDING, Align.left, true);

            font.draw(batch, "-", dec.x, dec.y + dec.height - 3f, dec.width, Align.center, false);
            font.draw(batch, "+", inc.x, inc.y + inc.height - 3f, inc.width, Align.center, false);
        }

        font.draw(
            batch, "Alles zuruecksetzen", resetAllButtonBounds.x, resetAllButtonBounds.y + resetAllButtonBounds.height - 4f,
            resetAllButtonBounds.width, Align.center, false
        );
    }
}
