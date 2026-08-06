package com.github.alexalde.emberroguelike;

// Boss-Kampfphasen (siehe GDD 6/Quest 10) - Top-Level statt in Boss verschachtelt, damit
// BossAttack-Implementierungen außerhalb von Boss.java in canTrigger() darauf zugreifen können.
// Einmaliger, einseitiger Wechsel bei einer HP-Schwelle (siehe Boss.update()) - kein Zurückwechseln.
public enum BossPhase {
    PHASE_ONE,
    PHASE_TWO
}
