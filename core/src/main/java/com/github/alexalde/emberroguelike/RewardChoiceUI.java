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

// Auswahl-Bildschirm für Raum-Belohnungen UND Level-Ups (siehe GDD 3.1/Quest 8) - beides nutzt
// denselben Mechanismus ("wähle 1 von N Stat-Boosts"). Bewusst Maus UND Tastatur gleichberechtigt
// bedienbar (Nutzer-Entscheidung, erleichtert späteren Controller-Support): Maus-Hover setzt
// denselben selectedIndex wie Pfeiltasten-Navigation - beide Eingabewege teilen sich einen
// einzigen Auswahl-Zustand statt getrennter Logik. Zeichnen vorerst nur ShapeRenderer-Rechtecke +
// BitmapFont-Text, kein Kunst-Pass (gleiches Prinzip wie Sidequest 1.4s Debug-Text).
public class RewardChoiceUI {

    private static final float CARD_WIDTH = 160f;
    private static final float CARD_HEIGHT = 200f;
    private static final float CARD_GAP = 20f;
    private static final float TEXT_PADDING = 12f;

    private final List<Item> options;
    private final Rectangle[] cardBounds;
    private int selectedIndex;
    private Item confirmedChoice;

    public RewardChoiceUI(List<Item> options) {
        this.options = options;
        this.selectedIndex = 0;
        this.confirmedChoice = null;

        this.cardBounds = new Rectangle[options.size()];
        float totalWidth = options.size() * CARD_WIDTH + (options.size() - 1) * CARD_GAP;
        float startX = (GameConfig.WORLD_WIDTH - totalWidth) / 2f;
        float cardY = (GameConfig.WORLD_HEIGHT - CARD_HEIGHT) / 2f;

        for (int i = 0; i < options.size(); i++) {
            float cardX = startX + i * (CARD_WIDTH + CARD_GAP);
            cardBounds[i] = new Rectangle(cardX, cardY, CARD_WIDTH, CARD_HEIGHT);
        }
    }

    // "uiMousePosition" ist bereits in uiCamera-Koordinaten umgerechnet (siehe Main.render(),
    // analog zur bestehenden Welt-Maus-Umrechnung). Maus-Hover UND Tastatur schreiben beide
    // denselben selectedIndex - kein separater "wer hat zuletzt gewählt"-Zustand nötig.
    public void update(Vector2 uiMousePosition) {
        for (int i = 0; i < cardBounds.length; i++) {
            if (cardBounds[i].contains(uiMousePosition)) {
                selectedIndex = i;
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    confirmedChoice = options.get(i);
                }
                break;
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            selectedIndex = (selectedIndex - 1 + options.size()) % options.size();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            selectedIndex = (selectedIndex + 1) % options.size();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            confirmedChoice = options.get(selectedIndex);
        }

        // Zahlentasten 1-3 als schneller Direktzugriff, zusätzlich zu Navigation+Bestätigen
        for (int i = 0; i < options.size() && i < 9; i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) {
                selectedIndex = i;
                confirmedChoice = options.get(i);
            }
        }
    }

    // null, solange noch keine Auswahl bestätigt wurde
    public Item getConfirmedChoice() {
        return confirmedChoice;
    }

    // Erwartet ein bereits laufendes ShapeRenderer.begin(ShapeType.Filled) (siehe Main.render())
    public void renderCards(ShapeRenderer shapeRenderer) {
        for (int i = 0; i < cardBounds.length; i++) {
            Rectangle bounds = cardBounds[i];
            shapeRenderer.setColor(i == selectedIndex ? Color.GOLD : Color.DARK_GRAY);
            shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    // Erwartet ein bereits laufendes SpriteBatch.begin() (siehe Main.render())
    public void renderText(SpriteBatch batch, BitmapFont font) {
        for (int i = 0; i < cardBounds.length; i++) {
            Rectangle bounds = cardBounds[i];
            String text = options.get(i).getDescription();
            float textY = bounds.y + bounds.height - TEXT_PADDING;
            font.draw(batch, text, bounds.x + TEXT_PADDING, textY, bounds.width - 2 * TEXT_PADDING, Align.center, true);
        }
    }
}
