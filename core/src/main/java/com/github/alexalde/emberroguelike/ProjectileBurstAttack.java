package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

// EINE parametrisierte Klasse für "viele leicht anpassbare Typen" (siehe GDD 6/Quest 10,
// Präzedenzfall CurrencyPickup) statt einer Unterklasse pro Muster - deckt gezielten
// Einzelschuss, gezielten Fächer und nicht gezielten Radial-Burst über Konstruktor-Parameter ab
// (siehe BossAttackPool für die konkreten Konfigurationen).
public class ProjectileBurstAttack implements BossAttack {

    // Kleiner, fixer Vorschau-Radius am Ursprung während des Windups - anders als bei
    // Melee/AOE/Beam ist die "Trefferzone" hier nicht EIN Bereich, sondern viele einzelne,
    // sich bewegende Projektile, ein einzelner wachsender Warnkreis wäre hier irreführend
    private static final float WINDUP_INDICATOR_RADIUS = 12f;

    private final String name;
    private final int projectileCount;
    private final float spreadAngleDegrees;
    private final boolean aimedAtPlayer;
    private final float projectileSpeed;
    private final float projectileDamage;
    private final float projectileRange;
    private final float projectileHitboxRadius;
    private final float windupDuration;
    private final float triggerRange;
    private final float cooldownDuration;
    private final float baseWeight;

    private Vector2 origin;
    private Vector2 baseDirection;
    private float windupRemaining;
    private boolean spawned;
    private List<HostileProjectile> activeProjectiles;
    private boolean finished;

    public ProjectileBurstAttack(
        String name, int projectileCount, float spreadAngleDegrees, boolean aimedAtPlayer,
        float projectileSpeed, float projectileDamage, float projectileRange, float projectileHitboxRadius,
        float windupDuration, float triggerRange, float cooldownDuration, float baseWeight
    ) {
        // Eine NICHT gezielte, aber schmale Fächerrichtung wäre willkürlich (der Boss ist ein
        // reiner Platzhalter-Kreis ohne eigene Blickrichtung) - bei einem vollen 360°-Burst
        // spielt die Basisrichtung dagegen ohnehin keine Rolle. Diese Kombination wird deshalb
        // gar nicht erst zugelassen, statt sie mit einer willkürlichen festen Richtung zu bauen.
        if (!aimedAtPlayer && spreadAngleDegrees < 360f) {
            throw new IllegalArgumentException("Nicht gezielte Bursts brauchen spreadAngleDegrees >= 360 (sonst willkürliche Basisrichtung)");
        }

        this.name = name;
        this.projectileCount = projectileCount;
        this.spreadAngleDegrees = spreadAngleDegrees;
        this.aimedAtPlayer = aimedAtPlayer;
        this.projectileSpeed = projectileSpeed;
        this.projectileDamage = projectileDamage;
        this.projectileRange = projectileRange;
        this.projectileHitboxRadius = projectileHitboxRadius;
        this.windupDuration = windupDuration;
        this.triggerRange = triggerRange;
        this.cooldownDuration = cooldownDuration;
        this.baseWeight = baseWeight;
    }

    @Override
    public void start(Vector2 origin, Player player, Room room) {
        this.origin = origin.cpy();
        this.baseDirection = aimedAtPlayer ? player.getCenter().sub(origin) : new Vector2(1f, 0f);
        if (baseDirection.len() > 0) {
            baseDirection.nor();
        } else {
            baseDirection.set(1f, 0f);
        }
        this.windupRemaining = windupDuration;
        this.spawned = false;
        this.activeProjectiles = new ArrayList<>();
        this.finished = false;
    }

    @Override
    public void update(float deltaTime, Player player, Room room) {
        if (finished) {
            return;
        }

        if (!spawned) {
            windupRemaining -= deltaTime;
            if (windupRemaining <= 0) {
                spawnProjectiles();
                spawned = true;
            }
            return;
        }

        for (HostileProjectile projectile : activeProjectiles) {
            projectile.update(deltaTime, player);
        }
        activeProjectiles.removeIf(HostileProjectile::isExpired);

        if (activeProjectiles.isEmpty()) {
            finished = true;
        }
    }

    // Offene Fächer (spreadAngleDegrees < 360): Enden liegen exakt bei ±halber Fächerbreite.
    // Voller Radial-Burst (>= 360): kein doppeltes Projektil an der 0°/360°-Naht.
    private void spawnProjectiles() {
        if (projectileCount == 1) {
            activeProjectiles.add(new HostileProjectile(origin, baseDirection, projectileSpeed, projectileDamage, projectileRange, projectileHitboxRadius));
            return;
        }

        boolean fullCircle = spreadAngleDegrees >= 360f;
        float angleStep = fullCircle ? spreadAngleDegrees / projectileCount : spreadAngleDegrees / (projectileCount - 1);
        float startAngle = fullCircle ? 0f : -spreadAngleDegrees / 2f;

        for (int i = 0; i < projectileCount; i++) {
            Vector2 direction = baseDirection.cpy().rotateDeg(startAngle + i * angleStep);
            activeProjectiles.add(new HostileProjectile(origin, direction, projectileSpeed, projectileDamage, projectileRange, projectileHitboxRadius));
        }
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void draw(ShapeRenderer shapeRenderer) {
        if (!spawned) {
            float progress = MathUtils.clamp(1f - (windupRemaining / windupDuration), 0f, 1f);
            shapeRenderer.setColor(new Color(Color.YELLOW).lerp(Color.RED, progress));
            shapeRenderer.circle(origin.x, origin.y, WINDUP_INDICATOR_RADIUS * progress);
        }
        for (HostileProjectile projectile : activeProjectiles) {
            projectile.draw(shapeRenderer);
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
    public String getName() {
        return name;
    }
}
