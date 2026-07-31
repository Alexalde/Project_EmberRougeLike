package com.github.alexalde.emberroguelike;

public class ProjectileCountBoostItem implements Item {

    private final int amount;

    public ProjectileCountBoostItem(int amount) {
        this.amount = amount;
    }

    @Override
    public void apply(PlayerStats stats) {
        stats.projectileCount += amount;
    }

    @Override
    public String getDescription() {
        return "+" + amount + " Projektil" + (amount == 1 ? "" : "e");
    }
}
