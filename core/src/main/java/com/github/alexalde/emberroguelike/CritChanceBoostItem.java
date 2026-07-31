package com.github.alexalde.emberroguelike;

public class CritChanceBoostItem implements Item {

    private final float amount;

    public CritChanceBoostItem(float amount) {
        this.amount = amount;
    }

    @Override
    public void apply(PlayerStats stats) {
        stats.critChance += amount;
    }

    @Override
    public String getDescription() {
        return String.format("+%.0f%% Kritische Trefferchance", amount * 100);
    }
}
