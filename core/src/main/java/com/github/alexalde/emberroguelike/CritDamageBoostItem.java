package com.github.alexalde.emberroguelike;

public class CritDamageBoostItem implements Item {

    private final float amount;

    public CritDamageBoostItem(float amount) {
        this.amount = amount;
    }

    @Override
    public void apply(PlayerStats stats) {
        stats.critDamageMultiplier += amount;
    }

    @Override
    public String getDescription() {
        return String.format("+%.0f%% Kritischer Schaden", amount * 100);
    }
}
