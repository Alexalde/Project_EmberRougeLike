package com.github.alexalde.emberroguelike;

// Erweiterungspunkt für typ-/bedingungsabhängige Schadens-Effekte (siehe GDD 2.1), z.B. ein
// Debuff, der nur mit einem bestimmten DamageType skaliert. Bewusst als eigenes Interface statt
// fester Felder/Maps - neue Effekte kommen als neue Klasse dazu, ohne PlayerStats.computeDamage()
// anzufassen (Open-Closed-Prinzip). Noch KEINE konkrete Implementierung (kein Item braucht das
// aktuell) - PlayerStats.modifiers bleibt vorerst eine leere Liste.
public interface DamageModifier {
    // "damage" ist der bereits mit damageMultiplier (und ggf. Crit) verrechnete Zwischenwert -
    // gibt den (möglicherweise weiter angepassten) Endwert zurück. Implementierungen, die auf
    // diesen konkreten Treffer nicht zutreffen, geben "damage" unverändert zurück.
    float modify(float damage, DamageType type);
}
