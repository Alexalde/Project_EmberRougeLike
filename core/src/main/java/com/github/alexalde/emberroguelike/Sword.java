package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.math.Vector2;

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

    private void attack(Vector2 origin, Vector2 direction, Enemy target) {
        Vector2 attackPoint = origin.cpy().add(direction.cpy().scl(range));
        if (target.isHitBy(attackPoint)) {
            target.takeDamage((int) damage);
        }
    }

    public boolean tryAttack(Vector2 origin, Vector2 direction, Enemy target) {
        if (attackCooldownRemaining <= 0){
            attackCooldownRemaining = attackCooldownDuration;
            attack(origin, direction, target);
            return true;
        }
        return false;
    }

}
