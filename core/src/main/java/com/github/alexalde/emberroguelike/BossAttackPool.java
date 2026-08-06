package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

// Feste Liste LANGLEBIGER, zustandsbehafteter BossAttack-Instanzen (siehe GDD 6/Quest 10,
// Präzedenzfall ItemPool/PermanentUpgradePool) - dieselben Objekte werden über mehrere
// Auslösungen hinweg wiederverwendet, nicht pro Auslösung neu erzeugt (jedes BossAttack.start()
// muss deshalb seinen kompletten internen Zustand zurücksetzen).
public class BossAttackPool {

    private final List<BossAttack> allAttacks;

    public BossAttackPool() {
        allAttacks = new ArrayList<>();
        allAttacks.add(new MeleeSlamAttack(48f, 0.4f, 15f, 1.5f));
        allAttacks.add(new TelegraphedAoeAttack(100f, 0.9f, 80f, 25f, 2.5f));

        // Beam-Muster (siehe GDD 6/Quest 10, Quest 10.10) - beide nur ab PHASE_TWO
        allAttacks.add(new PlayerLockedBeamAttack(150f, 0.7f, 1.2f, 300f, 16f, 8f, 0.3f, 3f));
        allAttacks.add(new RoomLockedBeamAttack(true, 1f, 0.8f, 32f, 10f, 0.3f, 3.5f));
        allAttacks.add(new RoomLockedBeamAttack(false, 1f, 0.8f, 32f, 10f, 0.3f, 3.5f));

        // Projektil-Muster (siehe GDD 6/Quest 10, Quest 10.11) - EINE parametrisierte Klasse,
        // drei Konfigurationen. Gezielter Einzelschuss bewusst schon in PHASE_ONE verfügbar
        // (einfache Fernkampf-Variante von Anfang an), Fächer/Radial-Burst erst ab PHASE_TWO -
        // gleiches "Phase 2 wird spürbar voller"-Prinzip wie bei den Beams.
        allAttacks.add(new ProjectileBurstAttack(
            "Gezielter Schuss", 1, 0f, true, 180f, 10f, 350f, 6f, 0.5f, 250f, false, 1.8f
        ));
        allAttacks.add(new ProjectileBurstAttack(
            "Projektil-Fächer", 5, 60f, true, 160f, 8f, 300f, 6f, 0.6f, 250f, true, 2.5f
        ));
        allAttacks.add(new ProjectileBurstAttack(
            "Radial-Burst", 12, 360f, false, 140f, 8f, 280f, 6f, 0.7f, 500f, true, 3f
        ));
    }

    // Gleichverteilt zufällige Wahl unter allen aktuell auslösbaren Mustern, null wenn keins
    // passt - ersetzt die frühere feste patternTwoChanceInPhaseTwo-Wahrscheinlichkeit: sind
    // mehrere Muster gleichzeitig auslösbar, IST eine Gleichverteilung über die gefilterte Liste
    // bereits eine faire Zufallswahl zwischen ihnen, kein separates Wahrscheinlichkeits-Feld nötig
    public BossAttack pickTriggerable(Vector2 origin, Player player, Room room, BossPhase phase) {
        List<BossAttack> triggerable = new ArrayList<>();
        for (BossAttack attack : allAttacks) {
            if (attack.canTrigger(origin, player, room, phase)) {
                triggerable.add(attack);
            }
        }
        if (triggerable.isEmpty()) {
            return null;
        }
        return triggerable.get(MathUtils.random(triggerable.size() - 1));
    }
}
