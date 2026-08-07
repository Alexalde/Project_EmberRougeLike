package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

// Ein einzelnes Boss-Angriffsmuster (siehe GDD 6/Quest 10) - eigenständige Mini-Zustandsmaschine:
// start() (Windup beginnt) -> update() (Windup -> Auflösung/Spawn -> ggf. aktive Phase) ->
// isFinished(). Boss hält höchstens EIN activeAttack gleichzeitig und bleibt bewegungslos,
// solange einer läuft (das ist bewusst KEIN Teil dieses Interfaces - jedes aktuelle und geplante
// Muster will das ohnehin, keine Pro-Muster-Konfiguration für einen Fall, der noch nicht existiert).
//
// Implementierungen sind LANGLEBIG und werden über mehrere Auslösungen hinweg wiederverwendet
// (siehe BossAttackPool) - start() muss deshalb JEDEN internen Zustand vollständig zurücksetzen.
public interface BossAttack {

    // Beginnt den Angriff - "origin" ist die Boss-Position ZUM AUSLÖSE-ZEITPUNKT (Implementierungen
    // die eine feste Richtung/Position brauchen, frieren sie hier ein, siehe z.B. TelegraphedAoeAttack)
    void start(Vector2 origin, Player player, Room room);

    void update(float deltaTime, Player player, Room room);

    boolean isFinished();

    // Erwartet ein bereits laufendes ShapeRenderer.begin(ShapeType.Filled) (siehe Boss.draw())
    void draw(ShapeRenderer shapeRenderer);

    // Reine Abfrage, KEINE Seiteneffekte - der Pool ruft das potenziell für jedes Muster im
    // selben Frame auf, bevor er sich für eines entscheidet (siehe BossAttackPool.pickTriggerable()).
    // Bewusst OHNE BossPhase-Parameter - Phasen-Zugehörigkeit ist seit dem Pity-Gewichtungs-Umbau
    // (siehe BossAttackPool) reine Pool-Struktur (welcher Phase-Liste ein Muster überhaupt
    // angehört), keine Prüfung mehr, die jede Implementierung einzeln duplizieren müsste. Hier
    // geht es nur noch um situative Gates wie Reichweite.
    boolean canTrigger(Vector2 origin, Player player, Room room);

    // Soll-Anteil an der Zufallsauswahl innerhalb seiner Phase(n) (siehe BossAttackPool -
    // Pity-Gewichtung: der tatsächlich verwendete Gewichtungswert wächst/schrumpft dynamisch,
    // dieser hier ist nur der statische Ausgangs-/Rückfallwert)
    float getBaseWeight();

    // Cooldown, der nach isFinished() greift, bevor der Boss das NÄCHSTE Muster auslösen darf
    float getCooldownAfterFinish();

    // Fürs Debug-Log (siehe DebugSettings.logDamage)
    String getName();
}
