package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

// Muster 2 (siehe GDD 6/Quest 10) - nur ab PHASE_TWO, GRÖSSERE Auslöse-Reichweite als
// MeleeSlamAttack (macht Phase 2 spürbar anders, nicht nur eine Farbänderung), längerer Windup,
// größerer Radius/Schaden/Cooldown. Migriert aus dem ursprünglichen Boss.TELEGRAPHING-Zustand
// (Quest 10.5) ins BossAttack-System (Quest 10.9) - Zahlenwerte unverändert.
public class TelegraphedAoeAttack implements BossAttack {

    // Siehe MeleeSlamAttack für die Begründung - kurzer weißer Impact-Blitz statt ersatzlosem
    // Verschwinden im selben Frame, in dem der Schaden abgerechnet wird
    private static final float IMPACT_FLASH_DURATION = 0.1f;

    private final float triggerRange;
    private final float windupDuration;
    private final float slamRadius;
    private final float damage;
    private final float cooldownDuration;
    private final float baseWeight;

    private Vector2 origin;
    private float windupRemaining;
    private boolean impactResolved;
    private float impactFlashRemaining;
    private boolean finished;

    public TelegraphedAoeAttack(float triggerRange, float windupDuration, float slamRadius, float damage, float cooldownDuration, float baseWeight) {
        this.triggerRange = triggerRange;
        this.windupDuration = windupDuration;
        this.slamRadius = slamRadius;
        this.damage = damage;
        this.cooldownDuration = cooldownDuration;
        this.baseWeight = baseWeight;
    }

    @Override
    public void start(Vector2 origin, Player player, Room room) {
        // Eingefroren beim Windup-Start - gegen DIESE Position wird später aufgelöst, nicht die
        // (ohnehin unveränderte) aktuelle Boss-Position, macht die "an dieser Stelle
        // ausweichen"-Regel unmissverständlich
        this.origin = origin.cpy();
        this.windupRemaining = windupDuration;
        this.impactResolved = false;
        this.impactFlashRemaining = 0f;
        this.finished = false;
    }

    @Override
    public void update(float deltaTime, Player player, Room room) {
        if (finished) {
            return;
        }

        if (!impactResolved) {
            windupRemaining -= deltaTime;
            if (windupRemaining <= 0) {
                if (origin.dst(player.getCenter()) <= slamRadius) {
                    if (DebugSettings.logDamage) {
                        System.out.println("Boss trifft mit " + getName() + "!");
                    }
                    player.takeDamage(damage);
                }
                impactResolved = true;
                impactFlashRemaining = IMPACT_FLASH_DURATION;
            }
            return;
        }

        impactFlashRemaining -= deltaTime;
        if (impactFlashRemaining <= 0) {
            finished = true;
        }
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void draw(ShapeRenderer shapeRenderer) {
        if (!impactResolved) {
            // Volle Trefferzone sofort sichtbar, Füllung von innen nach außen zeigt Windup-
            // Fortschritt (Nutzer-Entscheidung Task #77 - siehe BossAttackTelegraph)
            float progress = MathUtils.clamp(1f - (windupRemaining / windupDuration), 0f, 1f);
            BossAttackTelegraph.drawFillingCircle(shapeRenderer, origin, slamRadius, progress);
        } else {
            shapeRenderer.setColor(Color.WHITE);
            shapeRenderer.circle(origin.x, origin.y, slamRadius);
        }
    }

    @Override
    public boolean canTrigger(Vector2 origin, Player player, Room room) {
        // Phasen-Zugehörigkeit ist jetzt Pool-Struktur (siehe BossAttackPool) - hier nur noch
        // die situative Reichweiten-Prüfung
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
        return AttackCategory.SLAM;
    }

    @Override
    public String getName() {
        return "AOE-Slam";
    }
}
