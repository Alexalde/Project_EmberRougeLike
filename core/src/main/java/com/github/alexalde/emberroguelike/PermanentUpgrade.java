package com.github.alexalde.emberroguelike;

// Ein dauerhaftes, im Hub gekauftes Upgrade (siehe GDD 3.2/Quest 9) - wirkt bei JEDEM neuen Run
// erneut (siehe Player-Konstruktor), da PlayerStats selbst weiterhin Run-lokal bleibt (siehe
// Quest 8) und nicht direkt verändert wird.
public interface PermanentUpgrade {
    // Stabiler Schlüssel für MetaProgress.purchasedUpgradeIds - bewusst ein expliziter String
    // statt z.B. des Klassennamens (wäre bei Refactoring fragil)
    String getId();

    // Für die Shop-Auflistung (siehe UpgradeShopUI, Quest 9.5)
    String getDescription();

    float getCost();

    MetaCurrencyType getCurrencyType();

    // Bewusst Player statt nur PlayerStats als Ziel - manche Upgrades betreffen Player-Felder
    // außerhalb von PlayerStats (z.B. maxHealth)
    void apply(Player player);
}
