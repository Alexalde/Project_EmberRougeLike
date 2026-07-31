package com.github.alexalde.emberroguelike;

// Schadenstyp pro Waffe/Treffer (siehe GDD 2.1) - wächst bei Bedarf um weitere Typen, kein
// Anspruch auf Vollständigkeit jetzt. Ermöglicht später typ-abhängige Effekte (z.B. ein Debuff,
// der nur mit FIRE-Schaden skaliert), ohne die Grundstruktur nachträglich umbauen zu müssen.
public enum DamageType {
    PHYSICAL,
    FIRE,
    POISON
}
