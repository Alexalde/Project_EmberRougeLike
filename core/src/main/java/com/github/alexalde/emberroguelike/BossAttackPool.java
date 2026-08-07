package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Phasen-getrennte, gewichtete Zufallsauswahl von BossAttack-Instanzen (siehe GDD 6/Quest 10,
// Präzedenzfall ItemPool/PermanentUpgradePool). Jede Phase hat ihre EIGENE, statisch geladene
// Liste an Muster-Instanzen (Nutzer-Entscheidung: "jede Phase soll nur die Angriffe aus dem Pool
// laden, die auch wirklich verwendet werden sollen") - Phasen-Zugehörigkeit ist damit reine
// Pool-Struktur, keine Prüfung mehr innerhalb von BossAttack.canTrigger(). Muster, die in
// mehreren Phasen vorkommen (z.B. der Nahkampf-Schlag), sind dieselbe Instanz, aber mit
// UNABHÄNGIGEM Pity-Zustand pro Phase (eigene Gewichts-Map je Phase).
//
// Pity-Gewichtung (gemeinsam mit dem Nutzer durchgerechnet und per Simulation verifiziert,
// siehe Session-Notizen): jedes Muster hat ein festes baseWeight (Soll-Anteil) und ein
// working weight (tatsächlich für die Zufallsauswahl benutzt). Wird ein Muster gewählt, fällt
// SEIN working weight auf den baseWeight zurück; alle anderen, in dieser Runde TATSÄCHLICH
// triggerbaren Muster wachsen um baseWeight/PITY_DIVISOR (nicht triggerbare bleiben unverändert -
// sie hatten keine faire Chance, sollen also auch keine "Dürre" aufgebaut haben). Divisor=5 wurde
// empirisch als guter Kompromiss ermittelt: bei realistischer Kampflänge (~10-50 Auslösungen pro
// Phase) verhindert er spürbar lange Wiederholungs-Serien, ohne dass exakte baseWeight-Treue bei
// so wenigen Stichproben ohnehin eine Rolle spielen würde.
public class BossAttackPool {

    private static final float PITY_DIVISOR = 5f;

    private final List<BossAttack> phaseOneAttacks;
    private final List<BossAttack> phaseTwoAttacks;
    private final Map<BossAttack, Float> phaseOneWorkingWeights;
    private final Map<BossAttack, Float> phaseTwoWorkingWeights;

    public BossAttackPool() {
        // Grobe erste Werte, kein finales Balancing - alle Muster starten mit demselben
        // baseWeight, echtes Feintuning kommt erst nach Playtests
        MeleeSlamAttack melee = new MeleeSlamAttack(48f, 0.4f, 15f, 1.5f, 10f);
        ProjectileBurstAttack aimedShot = new ProjectileBurstAttack(
            "Gezielter Schuss", 1, 0f, true, 180f, 10f, 350f, 6f, 0.5f, 250f, 1.8f, 10f
        );

        TelegraphedAoeAttack aoeSlam = new TelegraphedAoeAttack(100f, 0.9f, 80f, 25f, 2.5f, 10f);
        PlayerLockedBeamAttack playerBeam = new PlayerLockedBeamAttack(150f, 0.7f, 1.2f, 300f, 16f, 8f, 0.3f, 3f, 10f);
        RoomLockedBeamAttack roomBeamHorizontal = new RoomLockedBeamAttack(true, 1f, 0.8f, 32f, 10f, 0.3f, 3.5f, 10f);
        RoomLockedBeamAttack roomBeamVertical = new RoomLockedBeamAttack(false, 1f, 0.8f, 32f, 10f, 0.3f, 3.5f, 10f);
        ProjectileBurstAttack fan = new ProjectileBurstAttack(
            "Projektil-Fächer", 5, 60f, true, 160f, 8f, 300f, 6f, 0.6f, 250f, 2.5f, 10f
        );
        ProjectileBurstAttack radialBurst = new ProjectileBurstAttack(
            "Radial-Burst", 12, 360f, false, 140f, 8f, 280f, 6f, 0.7f, 500f, 3f, 10f
        );

        phaseOneAttacks = List.of(melee, aimedShot);
        // Phase 2 ist eine Erweiterung, kein Ersatz - die Phase-1-Muster bleiben verfügbar
        // ("Phase 2 wird spürbar voller", nicht "Phase 2 ist komplett anders")
        phaseTwoAttacks = List.of(melee, aimedShot, aoeSlam, playerBeam, roomBeamHorizontal, roomBeamVertical, fan, radialBurst);

        phaseOneWorkingWeights = buildInitialWeights(phaseOneAttacks);
        phaseTwoWorkingWeights = buildInitialWeights(phaseTwoAttacks);
    }

    private Map<BossAttack, Float> buildInitialWeights(List<BossAttack> attacks) {
        Map<BossAttack, Float> weights = new HashMap<>();
        for (BossAttack attack : attacks) {
            weights.put(attack, attack.getBaseWeight());
        }
        return weights;
    }

    // Gewichtete Zufallswahl unter allen aktuell auslösbaren Mustern DER JEWEILIGEN PHASE UND
    // KATEGORIE (siehe AttackCategory/Layering - der Boss ruft das pro freiem Slot separat auf),
    // null wenn keins passt. Aktualisiert die Pity-Gewichte als Seiteneffekt (siehe Klassenkommentar).
    // Die Pity-Konkurrenz bleibt dadurch automatisch auf Muster DERSELBEN Kategorie beschränkt -
    // kein separates Gewichts-Tracking pro Kategorie nötig, "triggerable" ist ja schon gefiltert.
    public BossAttack pickTriggerable(Vector2 origin, Player player, Room room, BossPhase phase, AttackCategory category) {
        List<BossAttack> attacks = phase == BossPhase.PHASE_TWO ? phaseTwoAttacks : phaseOneAttacks;
        Map<BossAttack, Float> workingWeights = phase == BossPhase.PHASE_TWO ? phaseTwoWorkingWeights : phaseOneWorkingWeights;

        List<BossAttack> triggerable = new ArrayList<>();
        for (BossAttack attack : attacks) {
            if (attack.getCategory() == category && attack.canTrigger(origin, player, room)) {
                triggerable.add(attack);
            }
        }
        if (triggerable.isEmpty()) {
            return null;
        }

        float totalWeight = 0f;
        for (BossAttack attack : triggerable) {
            totalWeight += workingWeights.get(attack);
        }
        float roll = MathUtils.random() * totalWeight;
        float cumulative = 0f;
        BossAttack chosen = triggerable.get(triggerable.size() - 1); // Rückfallwert gegen Rundungsfehler
        for (BossAttack attack : triggerable) {
            cumulative += workingWeights.get(attack);
            if (roll < cumulative) {
                chosen = attack;
                break;
            }
        }

        // Pity-Update: das gewählte Muster fällt auf seinen baseWeight zurück, alle anderen
        // GERADE TRIGGERBAREN wachsen - nicht triggerbare bleiben unangetastet
        for (BossAttack attack : triggerable) {
            if (attack == chosen) {
                workingWeights.put(attack, attack.getBaseWeight());
            } else {
                workingWeights.put(attack, workingWeights.get(attack) + attack.getBaseWeight() / PITY_DIVISOR);
            }
        }

        return chosen;
    }
}
