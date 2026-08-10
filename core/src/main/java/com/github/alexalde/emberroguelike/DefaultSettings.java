package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.Input;

// Unveränderliche Werksvorgaben - GameSettings startet damit und kann darauf zurückgesetzt werden
public class DefaultSettings {

    public static final int MOVE_UP_KEY_PRIMARY = Input.Keys.W;
    public static final int MOVE_UP_KEY_SECONDARY = Input.Keys.UP;
    public static final int MOVE_DOWN_KEY_PRIMARY = Input.Keys.S;
    public static final int MOVE_DOWN_KEY_SECONDARY = Input.Keys.DOWN;
    public static final int MOVE_LEFT_KEY_PRIMARY = Input.Keys.A;
    public static final int MOVE_LEFT_KEY_SECONDARY = Input.Keys.LEFT;
    public static final int MOVE_RIGHT_KEY_PRIMARY = Input.Keys.D;
    public static final int MOVE_RIGHT_KEY_SECONDARY = Input.Keys.RIGHT;

    public static final int DASH_KEY = Input.Keys.SPACE;

    // Touhou-artiger "Focus"-Modus (siehe GDD 6/Quest 10, Task #84) - gehalten statt gedrückt,
    // verlangsamt die Bewegung fürs präzise Ausweichen in engen Bullet-Hell-Passagen
    public static final int FOCUS_KEY = Input.Keys.SHIFT_LEFT;
    // Spieler-Setting, kein Debug-Setting (siehe DebugSettings vs. GameSettings-Konvention) -
    // aktuell nur als Default wirksam, echtes Umschalten kommt erst mit dem Options-/Pause-Menü
    // (Task #62), gleiches Prinzip wie AUTO_SWING heute schon
    public static final boolean SHOW_HITBOX_WHILE_FOCUSED = true;

    public static final int MELEE_ATTACK_BUTTON = Input.Buttons.LEFT;
    public static final int RANGED_ATTACK_BUTTON = Input.Buttons.RIGHT;

    public static final boolean AUTO_SWING = false;

    public static final int FULLSCREEN_TOGGLE_KEY = Input.Keys.F11;

    public static final int RESTART_KEY = Input.Keys.R;

    // Für Terminal-Interaktion im Hub (siehe GDD 3.2/Quest 9) - anders als bei Door kein
    // Auto-Trigger, sondern ein bewusster Tastendruck in Reichweite
    public static final int INTERACT_KEY = Input.Keys.E;

    // Öffnet/schließt das Debug-Menü (siehe DebugMenuUI) - bisher unbenutzte Taste
    public static final int DEBUG_MENU_TOGGLE_KEY = Input.Keys.F1;

    // Öffnet/schließt das Pause-Menü (siehe PauseMenuUI, Task #62) - Standard-Konvention für
    // Pause-Menüs, bisher unbenutzte Taste auf globaler Ebene (DebugMenuUI/UpgradeShopUI nutzen
    // Escape nur INTERN zum Selbst-Schließen, keine Kollision mit diesem globalen Toggle)
    public static final int PAUSE_MENU_TOGGLE_KEY = Input.Keys.ESCAPE;

    // Spawnt einen Boss neben dem Spieler im aktuellen Raum (siehe GDD 6/Quest 10) - erlaubt
    // Testen ohne Tiled-Änderung, bleibt dauerhaft als Debug-Werkzeug bestehen (auch in Räumen
    // ohne echten BossSpawn-Objekt nützlich)
    public static final int DEBUG_BOSS_SPAWN_KEY = Input.Keys.F2;
}
