package com.github.alexalde.emberroguelike;

import java.util.ArrayList;
import java.util.List;

// Feste Liste aller kaufbaren permanenten Upgrades (siehe GDD 3.2/Quest 9) - anders als ItemPool
// KEINE Zufallsauswahl: der Shop zeigt ALLE gleichzeitig (siehe UpgradeShopUI, Quest 9.5), der
// Spieler wählt selbst, was er sich leisten will.
public class PermanentUpgradePool {

    private final List<PermanentUpgrade> allUpgrades;

    public PermanentUpgradePool() {
        this.allUpgrades = new ArrayList<>();
        allUpgrades.add(new MaxHealthUpgrade(25f, 50f));
    }

    public List<PermanentUpgrade> getAllUpgrades() {
        return allUpgrades;
    }

    // null, wenn keine Upgrade mit dieser ID (mehr) existiert (z.B. nach einem Refactoring, das
    // eine ID entfernt hat) - Aufrufer soll diesen Eintrag dann einfach ignorieren
    public PermanentUpgrade findById(String id) {
        for (PermanentUpgrade upgrade : allUpgrades) {
            if (upgrade.getId().equals(id)) {
                return upgrade;
            }
        }
        return null;
    }
}
