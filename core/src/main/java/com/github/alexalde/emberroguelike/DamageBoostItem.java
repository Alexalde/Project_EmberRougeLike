package com.github.alexalde.emberroguelike;

public class DamageBoostItem implements Item {

    private final float amount;

    public DamageBoostItem(float amount) {
        this.amount = amount;
    }

    @Override
    public void apply(PlayerStats stats) {
        stats.damageMultiplier += amount;
    }

    @Override
    public String getDescription() {
        return String.format("+%.0f%% Schaden", amount * 100);
    }
}
