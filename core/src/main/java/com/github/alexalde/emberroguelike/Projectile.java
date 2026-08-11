package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Projectile {

    // Wie weit ein Abprall (siehe bouncesRemaining/PlayerStats.bounceCount) nach dem nächsten
    // gültigen Ziel sucht - ohne diese Grenze könnte ein Abprall quer durch den ganzen Raum
    // "teleportieren", statt sich wie ein Kettenblitz zu einem WIRKLICH nahen Gegner
    // anzufühlen. Grober erster Wert, kein finales Balancing.
    private static final float BOUNCE_RANGE = 150f;

    private Vector2 position;
    private Vector2 direction;
    private float speed;
    private float damage;
    private float maxRange;
    private float distanceTraveled;
    private int hitsRemaining;
    private int bouncesRemaining;

    // Bereits getroffene Ziele DIESES Projektils (siehe GDD 2.1/Task #54, PlayerStats.bounceCount) -
    // verhindert, dass ein Abprall zwischen denselben zwei Gegnern hin- und herpingpongt, statt
    // sich zu progressiv NEUEN Zielen durchzuarbeiten. Nebeneffekt: verhindert auch, dass ein
    // ruhendes Ziel innerhalb der Trefferzone über mehrere Frames hinweg mehrfach getroffen wird.
    private final Set<Damageable> alreadyHitTargets = new HashSet<>();

    // Eigener Kollisionsradius, passend zur tatsächlichen Größe der (Platzhalter-)Grafik -
    // wird von Bow übergeben, nicht hier selbst aus der Textur berechnet (siehe Bow-Konstruktor)
    private float hitboxRadius;

    // Textur wird nicht selbst geladen/verwaltet - gehört Bow, wird nur referenziert
    // (viele kurzlebige Projektile sollen sich eine einzige Textur teilen, nicht je eine eigene laden)
    private Texture texture;

    public Projectile(
        Vector2 origin, Vector2 direction, float speed, float damage, float maxRange,
        int hitsRemaining, float hitboxRadius, Texture texture, int bounceCount
    ) {
        this.position = origin.cpy();
        this.direction = direction.cpy();
        this.speed = speed;
        this.damage = damage;
        this.maxRange = maxRange;
        this.distanceTraveled = 0f;
        this.hitsRemaining = hitsRemaining;
        this.hitboxRadius = hitboxRadius;
        this.texture = texture;
        this.bouncesRemaining = bounceCount;
    }

    public void update(float deltaTime, List<Damageable> targets) {
        float step = speed * deltaTime;
        position.x += direction.x * step;
        position.y += direction.y * step;
        distanceTraveled += step;

        // Trefferzone als Shape (siehe Task #85) statt Damageable.isHitBy()
        Shape hitShape = new CircleShape(position, hitboxRadius);
        for (Damageable target : targets) {
            if (hitsRemaining <= 0) {
                break;
            }
            if (!alreadyHitTargets.contains(target) && target.isAlive() && hitShape.overlapsCircle(target.getCenter(), target.getHurtboxRadius())) {
                target.takeDamage(damage);
                alreadyHitTargets.add(target);

                // Abprall verbraucht bewusst KEINE hitsRemaining-Ladung (getrennte Ressource,
                // siehe PlayerStats.bounceCount-Kommentar) - findet sich kein gültiges nächstes
                // Ziel, verhält sich der Treffer wie ein normaler Treffer ohne Abprall
                Damageable bounceTarget = bouncesRemaining > 0 ? findBounceTarget(targets, target) : null;
                if (bounceTarget != null) {
                    bouncesRemaining--;
                    direction = bounceTarget.getCenter().cpy().sub(position).nor();
                } else {
                    hitsRemaining--;
                }
                break;
            }
        }
    }

    // Nächstgelegenes, noch nicht getroffenes, lebendes Ziel innerhalb von BOUNCE_RANGE - null,
    // wenn keins in Reichweite ist (dann bleibt es beim normalen Treffer ohne Abprall)
    private Damageable findBounceTarget(List<Damageable> targets, Damageable justHit) {
        Damageable nearest = null;
        float nearestDistance = BOUNCE_RANGE;

        for (Damageable candidate : targets) {
            if (candidate == justHit || alreadyHitTargets.contains(candidate) || !candidate.isAlive()) {
                continue;
            }
            float distance = position.dst(candidate.getCenter());
            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    public boolean isExpired() {
        return hitsRemaining <= 0 || distanceTraveled >= maxRange;
    }

    public void draw(SpriteBatch batch) {
        // Platzhalter-Textur zeigt unrotiert nach oben (+Y = 90°) - daher der Versatz von -90°,
        // damit die Rotation die tatsächliche Flugrichtung trifft
        float rotation = direction.angleDeg() - 90f;

        // Pixel-Snapping wie bei Player.draw() - siehe dortiger Kommentar. Position wird VOR dem
        // Abzug des halben Texturmaßes gerundet, sonst könnte krummes Runden den Mittelpunkt
        // (das Rotationszentrum) minimal verschieben
        float drawX = MathUtils.round(position.x);
        float drawY = MathUtils.round(position.y);

        batch.draw(
            texture,
            drawX - texture.getWidth() / 2f, drawY - texture.getHeight() / 2f,
            texture.getWidth() / 2f, texture.getHeight() / 2f,
            texture.getWidth(), texture.getHeight(),
            1f, 1f,
            rotation,
            0, 0, texture.getWidth(), texture.getHeight(),
            false, false
        );
    }

    // Zeigt den echten Kollisionskreis (siehe DebugSettings.renderHitboxes), losgelöst von der Grafik
    public void drawHitboxDebug(ShapeRenderer shapeRenderer) {
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.circle(position.x, position.y, hitboxRadius);
    }

}
