package com.github.alexalde.emberroguelike;

public class Sword {

    private float attackCooldownDuration;
    private float attackCooldownRemaining;
    private float damage;
    private float range;

    public Sword(){
        this.attackCooldownDuration = 0.3f;
        this.attackCooldownRemaining = 0f;
        this.damage = 10f;
        this.range = 20f;
    }

    public void update(float deltaTime) {
        // Cooldown läuft ab
        if (attackCooldownRemaining > 0) {
            attackCooldownRemaining -= deltaTime;
        }

    }

    public void attack(){

    }

    public boolean tryAttack() {
        if (attackCooldownRemaining <= 0){
            attackCooldownRemaining = attackCooldownDuration;
            return true;
        }
        return false;
    }

}
