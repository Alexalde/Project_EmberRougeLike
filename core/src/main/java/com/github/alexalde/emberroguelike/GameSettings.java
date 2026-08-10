package com.github.alexalde.emberroguelike;

public class GameSettings {

    public static int moveUpKeyPrimary = DefaultSettings.MOVE_UP_KEY_PRIMARY;
    public static int moveUpKeySecondary = DefaultSettings.MOVE_UP_KEY_SECONDARY;
    public static int moveDownKeyPrimary = DefaultSettings.MOVE_DOWN_KEY_PRIMARY;
    public static int moveDownKeySecondary = DefaultSettings.MOVE_DOWN_KEY_SECONDARY;
    public static int moveLeftKeyPrimary = DefaultSettings.MOVE_LEFT_KEY_PRIMARY;
    public static int moveLeftKeySecondary = DefaultSettings.MOVE_LEFT_KEY_SECONDARY;
    public static int moveRightKeyPrimary = DefaultSettings.MOVE_RIGHT_KEY_PRIMARY;
    public static int moveRightKeySecondary = DefaultSettings.MOVE_RIGHT_KEY_SECONDARY;

    public static int dashKey = DefaultSettings.DASH_KEY;

    public static int focusKey = DefaultSettings.FOCUS_KEY;
    public static boolean showHitboxWhileFocused = DefaultSettings.SHOW_HITBOX_WHILE_FOCUSED;

    public static int meleeAttackButton = DefaultSettings.MELEE_ATTACK_BUTTON;
    public static int rangedAttackButton = DefaultSettings.RANGED_ATTACK_BUTTON;

    public static boolean autoSwing = DefaultSettings.AUTO_SWING;

    public static int fullscreenToggleKey = DefaultSettings.FULLSCREEN_TOGGLE_KEY;

    public static int restartKey = DefaultSettings.RESTART_KEY;

    public static int interactKey = DefaultSettings.INTERACT_KEY;

    public static int debugMenuToggleKey = DefaultSettings.DEBUG_MENU_TOGGLE_KEY;

    public static int bossDebugSpawnKey = DefaultSettings.DEBUG_BOSS_SPAWN_KEY;

    public static void resetToDefaults() {
        moveUpKeyPrimary = DefaultSettings.MOVE_UP_KEY_PRIMARY;
        moveUpKeySecondary = DefaultSettings.MOVE_UP_KEY_SECONDARY;
        moveDownKeyPrimary = DefaultSettings.MOVE_DOWN_KEY_PRIMARY;
        moveDownKeySecondary = DefaultSettings.MOVE_DOWN_KEY_SECONDARY;
        moveLeftKeyPrimary = DefaultSettings.MOVE_LEFT_KEY_PRIMARY;
        moveLeftKeySecondary = DefaultSettings.MOVE_LEFT_KEY_SECONDARY;
        moveRightKeyPrimary = DefaultSettings.MOVE_RIGHT_KEY_PRIMARY;
        moveRightKeySecondary = DefaultSettings.MOVE_RIGHT_KEY_SECONDARY;

        dashKey = DefaultSettings.DASH_KEY;

        focusKey = DefaultSettings.FOCUS_KEY;
        showHitboxWhileFocused = DefaultSettings.SHOW_HITBOX_WHILE_FOCUSED;

        meleeAttackButton = DefaultSettings.MELEE_ATTACK_BUTTON;
        rangedAttackButton = DefaultSettings.RANGED_ATTACK_BUTTON;

        autoSwing = DefaultSettings.AUTO_SWING;

        fullscreenToggleKey = DefaultSettings.FULLSCREEN_TOGGLE_KEY;

        restartKey = DefaultSettings.RESTART_KEY;

        interactKey = DefaultSettings.INTERACT_KEY;

        debugMenuToggleKey = DefaultSettings.DEBUG_MENU_TOGGLE_KEY;

        bossDebugSpawnKey = DefaultSettings.DEBUG_BOSS_SPAWN_KEY;
    }
}
