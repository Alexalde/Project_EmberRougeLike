package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

// Erster Boss (siehe GDD 6/Quest 10) - bewusst eine EIGENSTÄNDIGE Klasse, KEINE Enemy-Vererbung
// (Nutzer-Entscheidung: eigener Platz für Phasen-/Angriffsmuster-Logik, ohne Enemy damit zu
// überladen). Implementiert stattdessen Damageable, damit Sword/Bow/Projectile ihn treffen können,
// ohne von Enemy zu wissen (siehe Damageable.java).
//
// Terrain-Kollision (moveWithCollision/collidesWithRoom) ist eine bewusste KOPIE von Enemys
// privater Logik, keine gemeinsame Extraktion - die "eigene Klasse"-Entscheidung steht schon fest,
// eine Extraktion nur für diesen einen Punkt würde Enemys funktionierenden Code unnötig anfassen.
// Bei einem DRITTEN beweglichen Entitätstyp mit derselben Kollisionslogik wäre eine gemeinsame
// Extraktion der richtige nächste Schritt (gleiches Prinzip wie CurrencyPickup vs. Item).
public class Boss implements Damageable {

    private Vector2 position;

    private float health;
    private float maxHealth;

    // Größer als Enemys 32x32 - soll optisch klar als Boss erkennbar sein
    private static final int SPRITE_WIDTH = 64;
    private static final int SPRITE_HEIGHT = 64;
    private static final float HURTBOX_RADIUS = 32f;

    // Immer die volle Sprite-Fläche - anders als Player/Enemy gibt es noch kein Spritesheet mit
    // einem "Hitbox"-Tag, das einen kleineren Ausschnitt liefern könnte (siehe Klassenkommentar)
    private final Rectangle hitboxBounds = new Rectangle(0, 0, SPRITE_WIDTH, SPRITE_HEIGHT);

    private float moveSpeed;
    private float xpValue;

    private float hurtTimeRemaining;
    private float deathTimeRemaining;
    private static final float HURT_FLASH_DURATION = 0.15f;
    private static final float DEATH_ANIMATION_DURATION = 0.6f;

    // Phasenwechsel bei HP-Schwelle (siehe GDD 6/Quest 10) - einmaliger, einseitiger Wechsel
    // (Boss heilt hier nicht, keine Rückwärts-Transition nötig). Grober erster Wert, kein
    // finales Balancing.
    private enum BossPhase {
        PHASE_ONE,
        PHASE_TWO
    }

    private BossPhase phase;
    private static final float PHASE_TWO_HP_THRESHOLD = 0.5f;

    // TELEGRAPHING wird erst in Quest 10.5 (Muster 2) genutzt - bereits jetzt mit angelegt,
    // damit der Enum-Typ danach nicht nochmal angefasst werden muss
    private enum BossState {
        CHASING,
        MELEE_ATTACKING,
        TELEGRAPHING
    }

    private BossState state;
    // Gemeinsames Cooldown-Gate für JEDES Angriffsmuster (anders als Enemy, das nur EIN Muster
    // hat) - tickt unconditional in CHASING weiter, nicht nur während eines laufenden Angriffs
    private float attackCooldownRemaining;

    private float meleeAttackRange;
    private float meleeWindupDuration;
    private float meleeWindupRemaining;
    private float meleeDamage;
    private float meleeCooldownDuration;

    public Boss(float startX, float startY) {
        this.position = new Vector2(startX, startY);
        this.maxHealth = 300f;
        this.health = maxHealth;
        this.moveSpeed = 50f;
        this.xpValue = 100f;

        this.phase = BossPhase.PHASE_ONE;
        this.state = BossState.CHASING;
        this.attackCooldownRemaining = 0f;

        this.meleeAttackRange = 48f;
        this.meleeWindupDuration = 0.4f;
        this.meleeDamage = 15f;
        this.meleeCooldownDuration = 1.5f;
    }

    public void update(float deltaTime, Player player, Room room) {
        if (hurtTimeRemaining > 0) {
            hurtTimeRemaining -= deltaTime;
        }
        if (deathTimeRemaining > 0) {
            deathTimeRemaining -= deltaTime;
        }

        if (!isAlive()) {
            return;
        }

        if (phase == BossPhase.PHASE_ONE && health <= maxHealth * PHASE_TWO_HP_THRESHOLD) {
            phase = BossPhase.PHASE_TWO;
        }

        if (attackCooldownRemaining > 0) {
            attackCooldownRemaining -= deltaTime;
        }

        Vector2 myCenter = getCenter();
        Vector2 playerCenter = player.getCenter();
        float distance = myCenter.dst(playerCenter);

        switch (state) {
            case CHASING:
                if (attackCooldownRemaining <= 0 && distance <= meleeAttackRange) {
                    state = BossState.MELEE_ATTACKING;
                    meleeWindupRemaining = meleeWindupDuration;
                } else {
                    Vector2 direction = playerCenter.cpy().sub(myCenter);
                    if (direction.len() > 0) {
                        direction.nor();
                    }
                    moveWithCollision(direction.x * moveSpeed * deltaTime, direction.y * moveSpeed * deltaTime, room);
                }
                break;
            case MELEE_ATTACKING:
                // Bewusst bewegungslos während der Vorwarnzeit (siehe GDD 6/Quest 10) - konsistente,
                // leicht nachvollziehbare Regel, gilt später genauso für Muster 2 (Quest 10.5)
                meleeWindupRemaining -= deltaTime;
                if (meleeWindupRemaining <= 0) {
                    if (myCenter.dst(player.getCenter()) <= meleeAttackRange) {
                        if (DebugSettings.logDamage) {
                            System.out.println("Boss trifft mit Nahkampf-Schlag!");
                        }
                        player.takeDamage(meleeDamage);
                    }
                    attackCooldownRemaining = meleeCooldownDuration;
                    state = BossState.CHASING;
                }
                break;
            case TELEGRAPHING:
                // Kommt in Quest 10.5 (Muster 2) - noch kein Zustand, der aktuell erreicht wird
                break;
        }
    }

    // Kopie von Enemy.moveWithCollision() (siehe Klassenkommentar)
    private void moveWithCollision(float deltaX, float deltaY, Room room) {
        float newX = position.x + deltaX;
        if (!collidesWithRoom(newX, position.y, room)) {
            position.x = newX;
        }

        float newY = position.y + deltaY;
        if (!collidesWithRoom(position.x, newY, room)) {
            position.y = newY;
        }
    }

    // Kopie von Enemy.collidesWithRoom() (siehe Klassenkommentar)
    private boolean collidesWithRoom(float x, float y, Room room) {
        return room.isBlocked(x + hitboxBounds.x, y + hitboxBounds.y)
            || room.isBlocked(x + hitboxBounds.x + hitboxBounds.width - 1, y + hitboxBounds.y)
            || room.isBlocked(x + hitboxBounds.x + hitboxBounds.width - 1, y + hitboxBounds.y + hitboxBounds.height - 1)
            || room.isBlocked(x + hitboxBounds.x, y + hitboxBounds.y + hitboxBounds.height - 1);
    }

    @Override
    public void takeDamage(float amount) {
        this.health -= amount;
        hurtTimeRemaining = HURT_FLASH_DURATION;
        if (!isAlive()) {
            deathTimeRemaining = DEATH_ANIMATION_DURATION;
        }
        if (DebugSettings.logDamage) {
            System.out.println("Boss getroffen! Schaden: " + amount + ", verbleibendes Leben: " + health);
        }
    }

    @Override
    public boolean isAlive() {
        return health > 0;
    }

    // Analog Enemy.isRemovable() - "tot UND Sterbeanimation fertig"
    public boolean isRemovable() {
        return !isAlive() && deathTimeRemaining <= 0;
    }

    public float getXpValue() {
        return xpValue;
    }

    @Override
    public Vector2 getCenter() {
        return new Vector2(position.x + SPRITE_WIDTH / 2f, position.y + SPRITE_HEIGHT / 2f);
    }

    @Override
    public boolean isHitByArc(Vector2 origin, Vector2 direction, float range, float halfAngleDegrees) {
        Vector2 toThis = getCenter().sub(origin);
        float distance = toThis.len();

        if (distance > range + HURTBOX_RADIUS) {
            return false;
        }
        if (distance == 0) {
            return true;
        }

        toThis.nor();
        float minDot = (float) Math.cos(Math.toRadians(halfAngleDegrees));
        return direction.dot(toThis) >= minDot;
    }

    @Override
    public boolean isHitBy(Vector2 point, float otherRadius) {
        return getCenter().dst(point) <= HURTBOX_RADIUS + otherRadius;
    }

    // Reiner ShapeRenderer-Platzhalter (siehe Klassenkommentar) - kurzer heller Blitz bei
    // Treffern, sonst Körperfarbe je nach Phase (dunkelrot in Phase 1, kräftigeres Rot ab Phase
    // 2 - sofort sichtbares Feedback, dass der Phasenwechsel ausgelöst hat). Erwartet ein
    // bereits laufendes ShapeRenderer.begin(ShapeType.Filled) (siehe Main.render())
    public void draw(ShapeRenderer shapeRenderer) {
        Vector2 center = getCenter();
        if (hurtTimeRemaining > 0) {
            shapeRenderer.setColor(Color.WHITE);
        } else {
            shapeRenderer.setColor(phase == BossPhase.PHASE_TWO ? Color.RED : Color.MAROON);
        }
        shapeRenderer.circle(center.x, center.y, SPRITE_WIDTH / 2f);
    }

    // Gleiche GRÜN/GELB-Konvention wie bei Enemy - erwartet ein laufendes
    // ShapeRenderer.begin(ShapeType.Line) (siehe DebugSettings.renderHitboxes)
    public void drawHitboxDebug(ShapeRenderer shapeRenderer) {
        Vector2 center = getCenter();
        shapeRenderer.setColor(Color.GREEN);
        shapeRenderer.circle(center.x, center.y, HURTBOX_RADIUS);
    }

    public void drawTerrainCollisionDebug(ShapeRenderer shapeRenderer) {
        shapeRenderer.setColor(Color.YELLOW);
        float drawX = MathUtils.round(position.x);
        float drawY = MathUtils.round(position.y);
        shapeRenderer.rect(drawX + hitboxBounds.x, drawY + hitboxBounds.y, hitboxBounds.width, hitboxBounds.height);
    }

    // Kein echtes Texturen/Aseprite-Ressourcen-Cleanup nötig (siehe Klassenkommentar) - leerer
    // Rumpf für Symmetrie mit Enemy/Player und falls später doch echte Kunst dazukommt
    public void dispose() {
    }
}
