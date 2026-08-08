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

    private static final float TELEGRAPH_WIDTH = 4f;

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
            // Der Balken deckt per Definition die GESAMTE andere Achse ab (volle Raumbreite/
            // -höhe) - ein reiner 1D-Abstand auf der Quer-Achse reicht als Treffer-Check
            float playerCoordinate = horizontal ? player.getCenter().y : player.getCenter().x;
            if (Math.abs(playerCoordinate - fixedCoordinate) <= beamWidth / 2f + player.getHurtboxRadius()) {
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

    @Override
    public void draw(ShapeRenderer shapeRenderer) {
        Vector2 start;
        Vector2 end;
        if (horizontal) {
            start = new Vector2(0f, fixedCoordinate);
            end = new Vector2(room.getPixelWidth(), fixedCoordinate);
        } else {
            start = new Vector2(fixedCoordinate, 0f);
            end = new Vector2(fixedCoordinate, room.getPixelHeight());
        }

        if (!activeStarted) {
            float progress = MathUtils.clamp(1f - (windupRemaining / windupDuration), 0f, 1f);
            shapeRenderer.setColor(new Color(Color.YELLOW).lerp(Color.RED, progress));
            shapeRenderer.rectLine(start, end, TELEGRAPH_WIDTH);
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
