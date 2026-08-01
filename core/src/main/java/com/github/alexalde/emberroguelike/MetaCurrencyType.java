package com.github.alexalde.emberroguelike;

// Persistente Meta-Währungen (siehe GDD 3.2/Quest 9) - anders als der Zahlen-Charakterbogen
// PlayerStats überleben diese Werte einen Run-Neustart (siehe MetaProgress).
public enum MetaCurrencyType {
    // Permanente Stat-Upgrades im Hub (siehe PermanentUpgrade)
    UPGRADE,
    // Freischaltungen (z.B. später Start-/Zweitwaffen) - Ausgabe-Weg dafür noch nicht gebaut,
    // Währung selbst aber schon verdienbar/speicherbar (siehe ROADMAP-Backlog)
    UNLOCK
}
