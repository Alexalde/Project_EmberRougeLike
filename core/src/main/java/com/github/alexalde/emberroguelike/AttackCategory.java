package com.github.alexalde.emberroguelike;

// Slot-Einteilung für gleichzeitig laufende Boss-Angriffe (siehe GDD 6/Quest 10, Layering) - Boss
// darf höchstens EINEN aktiven Angriff PRO Kategorie gleichzeitig haben, aber verschiedene
// Kategorien dürfen parallel laufen (z.B. Beam + Projektil-Burst gleichzeitig, aber nicht zwei
// Slams gleichzeitig). Ein Composite-Muster (z.B. ein späteres gestaggertes Beam-Gitter) zählt
// dabei als EIN Angriff und belegt nur EINEN Slot, egal wie viele Teil-Elemente es intern verwaltet.
public enum AttackCategory {
    SLAM,
    BEAM,
    PROJECTILE
}
