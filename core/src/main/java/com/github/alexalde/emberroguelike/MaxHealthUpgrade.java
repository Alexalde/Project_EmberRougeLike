package com.github.alexalde.emberroguelike;

public class MaxHealthUpgrade implements PermanentUpgrade {

    private final float healthAmount;
    private final float cost;

    public MaxHealthUpgrade(float healthAmount, float cost) {
        this.healthAmount = healthAmount;
        this.cost = cost;
    }

    @Override
    public String getId() {
        return "max_health";
    }

    @Override
    public String getDescription() {
        return String.format("+%.0f Max-HP", healthAmount);
    }

    @Override
    public float getCost() {
        return cost;
    }

    @Override
    public MetaCurrencyType getCurrencyType() {
        return MetaCurrencyType.UPGRADE;
    }

    @Override
    public void apply(Player player) {
        player.increaseMaxHealth(healthAmount);
    }
}
