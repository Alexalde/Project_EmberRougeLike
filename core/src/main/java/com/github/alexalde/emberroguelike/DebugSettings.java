package com.github.alexalde.emberroguelike;

public class DebugSettings {
    public static boolean renderHitboxes = true;

    // Eigene Flags pro Kategorie statt einem globalen "Debug-Modus" - so kann man gezielt
    // nur die Konsolen-Ausgaben einschalten, die man gerade braucht, statt alles auf einmal
    public static boolean logDamage = true;
    public static boolean logAnimationState = true;
    public static boolean logLevelUp = true;
    public static boolean logInteraction = true;
    public static boolean renderTerrainDebug = true;
    public static boolean renderStats = true;
}
