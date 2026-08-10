package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

// Bullet-Hell-Beam (siehe GDD 6/Quest 10, Touhou/Calamity-Infernum als Referenz) - friert
// UrsPRUNG (Boss-Zentrum) UND RICHTUNG (einmal auf den Spieler gezielt) beim Windup-Start ein,
// zielt danach NICHT mehr nach. Nach dem Windup folgt eine aktive Kanal-Phase, in der der Strahl
// entlang dieser festen Linie ein echter, andauernder Gefahrenbereich ist.
//
// Schaden tickt in einem Intervall statt nur einmal oder jeden Frame - ein Kanal-Angriff ohne
// Spieler-Invincibility-Frames würde bei Pro-Frame-Schaden sofort tödlich sein, eine reine
// Einmal-Prüfung würde die aktive Phase dagegen bedeutungslos machen (nicht von einem Instant-
// Treffer mit Verzögerung unterscheidbar). Der Tick-Timer startet bei 0, damit ein Spieler, der
// beim Aktivieren schon im Strahl steht, sofort getroffen wird (wie in echten Bullet-Hell-Spielen).
public class PlayerLockedBeamAttack implements BossAttack {

    private final float triggerRange;
    private final float windupDuration;
    private final float activeDuration;
    private final float beamLength;
    private final float beamWidth;
    private final float damagePerTick;
    private final float tickInterval;
    private final float cooldownDuration;
    private final float baseWeight;

    private Vector2 origin;
    private Vector2 direction;
    private float windupRemaining;
    private float activeRemaining;
    private boolean activeStarted;
    private float tickTimer;
    private boolean finished;

    public PlayerLockedBeamAttack(
        float triggerRange, float windupDuration, float activeDuration, float beamLength,
        float beamWidth, float damagePerTick, float tickInterval, float cooldownDuration, float baseWeight
    ) {
        this.triggerRange = triggerRange;
        this.windupDuration = windupDuration;
        this.activeDuration = activeDuration;
        this.beamLength = beamLength;
        this.beamWidth = beamWidth;
        this.damagePerTick = damagePerTick;
        this.tickInterval = tickInterval;
        this.cooldownDuration = cooldownDuration;
        this.baseWeight = baseWeight;
    }

    @Override
    public void start(Vector2 origin, Player player, Room room) {
        this.origin = origin.cpy();
        this.direction = player.getCenter().sub(origin);
        if (direction.len() > 0) {
            direction.nor();
        } else {
            direction.set(1f, 0f);
        }
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
            Vector2 endpoint = origin.cpy().add(direction.cpy().scl(beamLength));
            float distance = Intersector.distanceSegmentPoint(origin, endpoint, player.getCenter());
            if (distance <= beamWidth / 2f + player.getHurtboxRadius()) {
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
        Vector2 endpoint = origin.cpy().add(direction.cpy().scl(beamLength));

        // Bewusst NICHT wachsend in der LÄNGE - der Endpunkt ist die eigentliche Ausweich-
        // Information und darf nicht erst spät sichtbar werden. Volle Länge von Anfang an
        // sichtbar (Basisfarbe), Füllung von der Mitte nach beiden Seiten zeigt Windup-
        // Fortschritt (Nutzer-Entscheidung Task #77 - siehe BossAttackTelegraph). Breite ist
        // bewusst schon im Windup die ECHTE beamWidth (Nutzer-Feedback) - anders als beim
        // Projektil-Burst lohnt sich hier die exakte Vorschau, weil der Strahl ein einzelner,
        // stehender Gefahrenbereich ist statt vieler kleiner beweglicher Punkte
        if (!activeStarted) {
            float progress = MathUtils.clamp(1f - (windupRemaining / windupDuration), 0f, 1f);
            BossAttackTelegraph.drawFillingLineFromCenter(shapeRenderer, origin, endpoint, beamWidth, progress);
        } else {
            shapeRenderer.setColor(Color.WHITE);
            shapeRenderer.rectLine(origin, endpoint, beamWidth);
        }
    }

    @Override
    public boolean canTrigger(Vector2 origin, Player player, Room room) {
        return origin.dst(player.getCenter()) <= triggerRange;
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
        return "Spieler-Beam";
    }
}
