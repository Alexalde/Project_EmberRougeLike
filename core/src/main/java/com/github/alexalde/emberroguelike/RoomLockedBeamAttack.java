package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

// Bullet-Hell-Beam, an der RAUMGEOMETRIE statt am Boss verankert (siehe GDD 6/Quest 10, Touhou/
// Calamity-Infernum "Wand fegt durch den Raum"-Stil) - Gegensatz zu PlayerLockedBeamAttack: die
// Ausdehnung ist die volle Raumbreite/-höhe (room.getPixelWidth()/getPixelHeight()), unabhängig
// von der Boss-Position ("origin" wird bewusst NICHT für die Platzierung verwendet, das ist der
// Sinn von "raumgebunden"). Bleibt trotzdem fair/ausweichbar: die QUER-Koordinate (Y bei
// horizontal, X bei vertikal) wird beim Windup-Start am aktuellen Spieler-Wert eingefroren, statt
// rein zufällig zu sein - ein raumspannender Angriff dreht sich nicht um Nähe, deshalb KEIN
// Reichweiten-Gate in canTrigger() (nur Phase).
public class RoomLockedBeamAttack implements BossAttack {

    private final boolean horizontal;
    private final float windupDuration;
    private final float activeDuration;
    private final float beamWidth;
    private final float damagePerTick;
    private final float tickInterval;
    private final float cooldownDuration;
    private final float baseWeight;

    // Die beim Windup-Start eingefrorene Y- (horizontal) bzw. X-Koordinate (vertikal)
    private float fixedCoordinate;
    private Room room;
    private float windupRemaining;
    private float activeRemaining;
    private boolean activeStarted;
    private float tickTimer;
    private boolean finished;

    public RoomLockedBeamAttack(
        boolean horizontal, float windupDuration, float activeDuration, float beamWidth,
        float damagePerTick, float tickInterval, float cooldownDuration, float baseWeight
    ) {
        this.horizontal = horizontal;
        this.windupDuration = windupDuration;
        this.activeDuration = activeDuration;
        this.beamWidth = beamWidth;
        this.damagePerTick = damagePerTick;
        this.tickInterval = tickInterval;
        this.cooldownDuration = cooldownDuration;
        this.baseWeight = baseWeight;
    }

    @Override
    public void start(Vector2 origin, Player player, Room room) {
        this.room = room;
        this.fixedCoordinate = horizontal ? player.getCenter().y : player.getCenter().x;
        this.windupRemaining = windupDuration;
        this.activeStarted = false;
        this.finished = false;
    }

    @Override
    public void update(float deltaTime, Player player, Room room) {
        if (finished) {
            return;
        }

        if (!activeStarted) {
            windupRemaining -= deltaTime;
            if (windupRemaining <= 0) {
                activeStarted = true;
                activeRemaining = activeDuration;
                tickTimer = 0f;
            }
            return;
        }

        activeRemaining -= deltaTime;
        tickTimer -= deltaTime;
        if (tickTimer <= 0) {
            // Trefferzone als Shape (siehe Task #85) - LineShape mit Intersector.
            // distanceSegmentPoint deckt den raumspannenden Fall korrekt ab (siehe LineShape-
            // Klassenkommentar), kein separater 1D-Achsen-Check mehr nötig
            Vector2[] endpoints = getLineEndpoints();
            Shape hitShape = new LineShape(endpoints[0], endpoints[1], beamWidth);
            if (hitShape.overlapsCircle(player.getCenter(), player.getHurtboxRadius())) {
                if (DebugSettings.logDamage) {
                    System.out.println("Boss trifft mit " + getName() + "!");
                }
                player.takeDamage(damagePerTick);
            }
            tickTimer = tickInterval;
        }

        if (activeRemaining <= 0) {
            finished = true;
        }
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    // Gemeinsam von update() (Treffer-Check) und draw() (Telegraph/aktiver Strahl) genutzt -
    // beide brauchen dieselben zwei Punkte, die die volle Raumbreite/-höhe abdecken
    private Vector2[] getLineEndpoints() {
        if (horizontal) {
            return new Vector2[] { new Vector2(0f, fixedCoordinate), new Vector2(room.getPixelWidth(), fixedCoordinate) };
        }
        return new Vector2[] { new Vector2(fixedCoordinate, 0f), new Vector2(fixedCoordinate, room.getPixelHeight()) };
    }

    @Override
    public void draw(ShapeRenderer shapeRenderer) {
        Vector2[] endpoints = getLineEndpoints();
        Vector2 start = endpoints[0];
        Vector2 end = endpoints[1];

        // Volle Länge UND volle echte beamWidth von Anfang an sichtbar (Basisfarbe), Füllung
        // wächst stattdessen in der BREITE (Nutzer-Entscheidung Task #77 - siehe
        // BossAttackTelegraph/PlayerLockedBeamAttack)
        if (!activeStarted) {
            float progress = MathUtils.clamp(1f - (windupRemaining / windupDuration), 0f, 1f);
            BossAttackTelegraph.drawFillingLineWidth(shapeRenderer, start, end, beamWidth, progress);
        } else {
            shapeRenderer.setColor(Color.WHITE);
            shapeRenderer.rectLine(start, end, beamWidth);
        }
    }

    @Override
    public boolean canTrigger(Vector2 origin, Player player, Room room) {
        // Kein Reichweiten-Gate - ein raumspannender Angriff dreht sich nicht um Nähe. Phasen-
        // Zugehörigkeit ist jetzt Pool-Struktur (siehe BossAttackPool), deshalb immer true.
        return true;
    }

    @Override
    public float getBaseWeight() {
        return baseWeight;
    }

    @Override
    public float getCooldownAfterFinish() {
        return cooldownDuration;
    }

    @Override
    public AttackCategory getCategory() {
        return AttackCategory.BEAM;
    }

    @Override
    public String getName() {
        return horizontal ? "Raum-Beam (horizontal)" : "Raum-Beam (vertikal)";
    }
}
