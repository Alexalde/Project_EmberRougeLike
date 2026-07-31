package com.github.alexalde.emberroguelike;

// Eine wählbare Belohnung (Raum-Clear ODER Level-Up, siehe GDD 3.1) - verändert PlayerStats
// dauerhaft für den Rest des Runs. Eigene Klasse pro Stat statt einer generischen, parametrisierten
// Klasse (siehe GDD 2.1) - passend zum Weapon/Sword/Bow-Präzedenzfall, Polymorphie über eigene
// Typen statt Lambdas.
public interface Item {
    // Für die Auswahl-UI (siehe RewardChoiceUI)
    String getDescription();

    void apply(PlayerStats stats);
}
