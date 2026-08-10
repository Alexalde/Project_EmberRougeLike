package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

// Muster 1 (siehe GDD 6/Quest 10) - immer verfügbar (kein Phasen-Gate), kurze Reichweite, kurzer
// Windup. Migriert aus dem ursprünglichen Boss.MELEE_ATTACKING-Zustand (Quest 10.4) ins
// BossAttack-System (Quest 10.9) - Zahlenwerte unverändert.
public class MeleeSlamAttack implements BossAttack {

    // Kurzer weißer Impact-Blitz nach dem Windup (Nutzer-Feedback) - reiner ShapeRenderer-
    // Platzhalter, bis Sprites/VFX das ersetzen (siehe Klassenkommentar), aber ohne DEN gäbe es
    // gar kein Treffer-Feedback: der Schaden wird instant beim Windup-Ende abgerechnet, ohne
    // diese kurze Phase würde der Kreis in genau diesem Frame ersatzlos verschwinden
    private static final float IMPACT_FLASH_DURATION = 0.1f;

    private final float range;
    private final float windupDuration;
    private final float damage;
    private final float cooldownDuration;
    private final float baseWeight;

    private Vector2 origin;
    private float windupRemaining;
    private boolean impactResolved;
    private float impactFlashRemaining;
    private boolean finished;

    public MeleeSlamAttack(float range, float windupDuration, float damage, float cooldownDuration, float baseWeight) {
        this.range = range;
        this.windupDuration = windupDuration;
        this.damage = damage;
        this.cooldownDuration = cooldownDuration;
        this.baseWeight = baseWeight;
    }

    @Override
    public void start(Vector2 origin, Player player, Room room) {
        // Eingefroren beim Windup-Start (jetzt explizit, statt nur implizit korrekt weil der Boss
        // während des Windups ohnehin steht - macht dieses Muster konsistent mit
        // TelegraphedAoeAttack, das origin schon immer explizit einfriert)
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
                // Trefferzone als Shape (siehe Task #85)
                Shape hitShape = new CircleShape(origin, range);
                if (hitShape.overlapsCircle(player.getCenter(), player.getHurtboxRadius())) {
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
            BossAttackTelegraph.drawFillingCircle(shapeRenderer, origin, range, progress);
        } else {
            shapeRenderer.setColor(Color.WHITE);
            shapeRenderer.circle(origin.x, origin.y, range);
        }
    }

    @Override
    public boolean canTrigger(Vector2 origin, Player player, Room room) {
        return origin.dst(player.getCenter()) <= range;
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
        return "Nahkampf-Schlag";
    }
}
