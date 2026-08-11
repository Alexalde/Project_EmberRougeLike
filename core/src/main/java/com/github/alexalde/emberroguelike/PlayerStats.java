package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.List;

// Reiner Zahlen-Charakterbogen des Spielers (siehe GDD 2.1) - bewusst von Player selbst getrennt,
// damit spätere Items nur auf PlayerStats zugreifen müssen, nicht auf ganz Player. Öffentliche
// Felder statt Getter/Setter-Paare (gleiches Muster wie GameSettings/DebugSettings) - Items
// verändern diese Werte direkt.
public class PlayerStats {

    // Multiplikator auf JEDEN Waffen-Basisschaden - 1 = unverändert
    public float damageMultiplier = 1f;
    // Skaliert Angriffs-Cooldowns/-Timing der aktiven Waffe - 1 = unverändert
    public float attackSpeedMultiplier = 1f;
    // Wahrscheinlichkeit (0-1) für einen kritischen Treffer - 0 = nie
    public float critChance = 0f;
    // Zusätzlicher Multiplikator bei einem Crit (wirkt nur, wenn critChance > 0 einen Treffer auslöst)
    public float critDamageMultiplier = 1.5f;
    // Zusätzliche Projektile pro Schuss (z.B. Bogen) - 0 = unverändert (nur der Basis-Schuss)
    public int projectileCount = 0;
    // Wirkt NUR auf den Dash-Cooldown (nicht auf Waffen-Cooldowns - das ist attackSpeedMultipliers
    // Job, siehe GDD 2.1) - 0 = unverändert
    public float cooldownReduction = 0f;
    // Magnet-Reichweite für Pickups (siehe Pickup.update(), Quest 8) - innerhalb dieser Distanz
    // fliegen Orbs von selbst zum Spieler, unabhängig vom kleinen, festen Kontakt-Radius
    public float pickupRange = 60f;

    // Vorerst ungenutzte Platzhalter-Felder (siehe GDD 2.1) - für spätere Items/Level-Ups
    // vorgesehen, damit Quest 8 nicht sofort wieder neue Felder nachziehen muss
    public float moveSpeed = 0f;
    public float lifeSteal = 0f;
    public float range = 0f;
    // Multiplikator auf die Projektil-Fluggeschwindigkeit (z.B. Bogen) - 1 = unverändert
    public float projectileSpeedMultiplier = 1f;
    // Multiplikator auf JEDE eingesammelte XP-Menge (siehe XpPickup) - 1 = unverändert
    public float xpMultiplier = 1f;
    // Multiplikator auf JEDE eingesammelte RunCurrency-Menge (siehe RunCurrencyPickup,
    // Player.addRunCurrency()), gleiches Prinzip wie xpMultiplier - 1 = unverändert
    public float runCurrencyMultiplier = 1f;
    // Zusätzliche Abprall-/Ricochet-Ladungen für Projektile, die diese Eigenschaft haben (über
    // Items ODER spezifische Waffen, siehe ROADMAP-Backlog) - 0 = kein Abprall
    public int bounceCount = 0;

    // Erweiterungspunkt für typ-/bedingungsabhängige Effekte (siehe DamageModifier) - bleibt
    // vorerst leer, kein Item braucht das aktuell
    private final List<DamageModifier> modifiers = new ArrayList<>();

    public List<DamageModifier> getModifiers() {
        return modifiers;
    }

    // Verrechnet einen Waffen-Basiswert mit den globalen Stats: Multiplikator, dann ein
    // Crit-Roll, dann alle registrierten Modifier (siehe DamageModifier) - in dieser Reihenfolge,
    // damit Modifier auf den bereits critical getroffenen Wert reagieren können, nicht umgekehrt
    public float computeDamage(float baseDamage, DamageType type) {
        float damage = baseDamage * damageMultiplier;

        if (MathUtils.random() < critChance) {
            damage *= critDamageMultiplier;
        }

        for (DamageModifier modifier : modifiers) {
            damage = modifier.modify(damage, type);
        }

        return damage;
    }
}
