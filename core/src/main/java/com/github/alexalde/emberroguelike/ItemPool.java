package com.github.alexalde.emberroguelike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Feste Liste aller verfügbaren Items (siehe GDD 2.1/3.1) - eine Instanz pro Item-Typ genügt für
// den aktuellen Vertical Slice, weitere Items/Stufen kommen bei Bedarf dazu.
public class ItemPool {

    private final List<Item> allItems;

    public ItemPool() {
        this.allItems = new ArrayList<>();
        allItems.add(new DamageBoostItem(0.15f));
        allItems.add(new AttackSpeedBoostItem(0.15f));
        allItems.add(new CritChanceBoostItem(0.05f));
        allItems.add(new CritDamageBoostItem(0.25f));
        allItems.add(new ProjectileCountBoostItem(1));
    }

    // Zieht "count" verschiedene Items OHNE Zurücklegen (keine doppelte Auswahl innerhalb
    // derselben Belohnungsrunde) - Shuffle-und-Abschneiden reicht für den kleinen Pool locker aus
    public List<Item> pickRandom(int count) {
        List<Item> shuffled = new ArrayList<>(allItems);
        Collections.shuffle(shuffled);
        return new ArrayList<>(shuffled.subList(0, Math.min(count, shuffled.size())));
    }
}
