package com.github.alexalde.emberroguelike;

public class AttackSpeedBoostItem implements Item {

    private final float amount;

    public AttackSpeedBoostItem(float amount) {
        this.amount = amount;
    }

    @Override
    public void apply(PlayerStats stats) {
        stats.attackSpeedMultiplier += amount;
    }

    @Override
    public String getDescription() {
        return String.format("+%.0f%% Angriffstempo", amount * 100);
    }
}
