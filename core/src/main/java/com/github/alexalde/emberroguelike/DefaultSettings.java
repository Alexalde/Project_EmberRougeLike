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

    public static final int MELEE_ATTACK_BUTTON = Input.Buttons.LEFT;
    public static final int RANGED_ATTACK_BUTTON = Input.Buttons.RIGHT;

    public static final boolean AUTO_SWING = false;
}
