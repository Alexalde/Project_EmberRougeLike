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

// Pause-Menü während eines laufenden Runs (siehe GDD/Task #62) - nach demselben Bauplan wie
// DebugMenuUI/UpgradeShopUI: Bounds/Hover/Klick-Logik komplett getrennt von renderShapes()/
// renderText(), damit sich die reine ShapeRenderer-Platzhalter-Optik SPÄTER durch echte Sprites/
// ein 9-Patch-Panel ersetzen lässt, ohne die Zustandslogik anzufassen (Nutzer-Wunsch: modular für
// späteren visuellen Ersatz, erstmal nur Funktion). Geöffnet/geschlossen über
// GameSettings.pauseMenuToggleKey (siehe Main.render(), gleiches Prinzip wie
// GameSettings.debugMenuToggleKey) - "Fortsetzen" ist nur die Maus-Entsprechung derselben Aktion.
public class PauseMenuUI {

    private static final float PANEL_WIDTH = 220f;
    private static final float BUTTON_HEIGHT = 32f;
    private static final float BUTTON_GAP = 12f;
    private static final float TITLE_HEIGHT = 28f;
    private static final float PADDING = 16f;

    private final Rectangle panelBounds;
    private final Rectangle resumeButtonBounds;
    private final Rectangle giveUpButtonBounds;

    private boolean resumeButtonHovered;
    private boolean giveUpButtonHovered;
    private boolean closeRequested;
    private boolean giveUpRequested;

    public PauseMenuUI() {
        float panelHeight = TITLE_HEIGHT + 2 * BUTTON_HEIGHT + BUTTON_GAP + 2 * PADDING;
        float panelX = (GameConfig.WORLD_WIDTH - PANEL_WIDTH) / 2f;
        float panelY = (GameConfig.WORLD_HEIGHT - panelHeight) / 2f;
        this.panelBounds = new Rectangle(panelX, panelY, PANEL_WIDTH, panelHeight);

        float buttonX = panelX + PADDING;
        float buttonWidth = PANEL_WIDTH - 2 * PADDING;
        float resumeY = panelY + panelHeight - PADDING - TITLE_HEIGHT - BUTTON_HEIGHT;
        float giveUpY = resumeY - BUTTON_GAP - BUTTON_HEIGHT;

        this.resumeButtonBounds = new Rectangle(buttonX, resumeY, buttonWidth, BUTTON_HEIGHT);
        this.giveUpButtonBounds = new Rectangle(buttonX, giveUpY, buttonWidth, BUTTON_HEIGHT);
    }

    // "uiMousePosition" ist bereits in uiCamera-Koordinaten umgerechnet (siehe Main.render())
    public void update(Vector2 uiMousePosition) {
        boolean leftClick = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);

        resumeButtonHovered = resumeButtonBounds.contains(uiMousePosition);
        if (resumeButtonHovered && leftClick) {
            closeRequested = true;
        }

        giveUpButtonHovered = giveUpButtonBounds.contains(uiMousePosition);
        if (giveUpButtonHovered && leftClick) {
            giveUpRequested = true;
        }
    }

    public boolean isCloseRequested() {
        return closeRequested;
    }

    // Liefert die Anfrage genau einmal zurück (danach wieder false) - gleiches Prinzip wie
    // UpgradeShopUI.consumePurchaseRequest(), verhindert mehrfaches Verarbeiten über mehrere Frames
    public boolean consumeGiveUpRequest() {
        boolean requested = giveUpRequested;
        giveUpRequested = false;
        return requested;
    }

    // Erwartet ein bereits laufendes ShapeRenderer.begin(ShapeType.Filled) (siehe Main.render())
    public void renderShapes(ShapeRenderer shapeRenderer) {
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(panelBounds.x, panelBounds.y, panelBounds.width, panelBounds.height);

        shapeRenderer.setColor(resumeButtonHovered ? Color.GOLD : Color.GRAY);
        shapeRenderer.rect(resumeButtonBounds.x, resumeButtonBounds.y, resumeButtonBounds.width, resumeButtonBounds.height);

        // ROT als Warnfarbe - "Run aufgeben" ist eine endgültige, nicht rückgängig zu machende
        // Aktion (identisch zum Tod, siehe Main.startGame())
        shapeRenderer.setColor(giveUpButtonHovered ? Color.GOLD : Color.RED);
        shapeRenderer.rect(giveUpButtonBounds.x, giveUpButtonBounds.y, giveUpButtonBounds.width, giveUpButtonBounds.height);
    }

    // Erwartet ein bereits laufendes SpriteBatch.begin() (siehe Main.render())
    public void renderText(SpriteBatch batch, BitmapFont font) {
        font.draw(
            batch, "PAUSE", panelBounds.x, panelBounds.y + panelBounds.height - 4f,
            panelBounds.width, Align.center, false
        );
        font.draw(
            batch, "Fortsetzen", resumeButtonBounds.x, resumeButtonBounds.y + resumeButtonBounds.height - 8f,
            resumeButtonBounds.width, Align.center, false
        );
        font.draw(
            batch, "Run aufgeben", giveUpButtonBounds.x, giveUpButtonBounds.y + giveUpButtonBounds.height - 8f,
            giveUpButtonBounds.width, Align.center, false
        );
    }
}
