package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.EnumMap;
import java.util.Map;

// Erster Boss (siehe GDD 6/Quest 10) - bewusst eine EIGENSTÄNDIGE Klasse, KEINE Enemy-Vererbung
// (Nutzer-Entscheidung: eigener Platz für Phasen-/Angriffsmuster-Logik, ohne Enemy damit zu
// überladen). Implementiert stattdessen Damageable, damit Sword/Bow/Projectile ihn treffen können,
// ohne von Enemy zu wissen (siehe Damageable.java).
//
// Angriffsmuster leben seit Quest 10.9 als eigene BossAttack-Implementierungen (siehe
// BossAttackPool) statt Boss-interner Zustandslogik. Seit dem Layering-Umbau (Task #81) kann
// PRO AttackCategory (siehe AttackCategory.java) höchstens ein Muster gleichzeitig aktiv sein,
// verschiedene Kategorien laufen aber parallel (z.B. Beam + Projektil-Burst gleichzeitig).
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
    // finales Balancing. BossPhase ist ein Top-Level-Enum (siehe BossPhase.java), damit
    // BossAttack-Implementierungen außerhalb dieser Klasse darauf zugreifen können.
    private BossPhase phase;
    private static final float PHASE_TWO_HP_THRESHOLD = 0.5f;

    private final BossAttackPool attackPool;
    // Ein Slot PRO AttackCategory (siehe Layering, Task #81) - fehlender Eintrag = Slot frei.
    // EnumMap statt HashMap, da AttackCategory ein Enum mit fester, kleiner Wertemenge ist.
    private final Map<AttackCategory, BossAttack> activeAttacks = new EnumMap<>(AttackCategory.class);
    // Cooldown PRO Kategorie, unabhängig voneinander - tickt unconditional weiter, auch während
    // eine ANDERE Kategorie gerade aktiv ist, nicht nur wenn der Boss komplett untätig ist
    private final Map<AttackCategory, Float> categoryCooldownRemaining = new EnumMap<>(AttackCategory.class);

    public Boss(float startX, float startY) {
        this.position = new Vector2(startX, startY);
        this.maxHealth = 300f;
        this.health = maxHealth;
        this.moveSpeed = 50f;
        this.xpValue = 100f;

        this.phase = BossPhase.PHASE_ONE;
        this.attackPool = new BossAttackPool();
        for (AttackCategory category : AttackCategory.values()) {
            categoryCooldownRemaining.put(category, 0f);
        }
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

        Vector2 myCenter = getCenter();

        // Laufende Angriffe aktualisieren, fertige aus dem Slot entfernen und deren
        // Kategorie-Cooldown starten (jede Kategorie unabhängig von den anderen)
        for (AttackCategory category : AttackCategory.values()) {
            BossAttack active = activeAttacks.get(category);
            if (active == null) {
                continue;
            }
            active.update(deltaTime, player, room);
            if (active.isFinished()) {
                categoryCooldownRemaining.put(category, active.getCooldownAfterFinish());
                activeAttacks.remove(category);
            }
        }

        for (AttackCategory category : AttackCategory.values()) {
            float cooldown = categoryCooldownRemaining.get(category);
            if (cooldown > 0) {
                categoryCooldownRemaining.put(category, cooldown - deltaTime);
            }
        }

        // Für jeden freien, cooldown-bereiten Slot ein neues Muster auslösen
        for (AttackCategory category : AttackCategory.values()) {
            if (activeAttacks.containsKey(category) || categoryCooldownRemaining.get(category) > 0) {
                continue;
            }
            BossAttack chosen = attackPool.pickTriggerable(myCenter, player, room, phase, category);
            if (chosen != null) {
                chosen.start(myCenter, player, room);
                activeAttacks.put(category, chosen);
                if (DebugSettings.logDamage) {
                    System.out.println("Boss startet Angriff: " + chosen.getName());
                }
            }
        }

        // Bewegungslos, solange IRGENDEIN aktiver Angriff das verlangt (siehe
        // BossAttack.requiresStationary()) - ein einzelnes true reicht, egal wie viele andere
        // Kategorien parallel laufen
        boolean mustStayStill = activeAttacks.values().stream().anyMatch(BossAttack::requiresStationary);
        if (mustStayStill) {
            return;
        }

        Vector2 direction = player.getCenter().cpy().sub(myCenter);
        if (direction.len() > 0) {
            direction.nor();
        }
        moveWithCollision(direction.x * moveSpeed * deltaTime, direction.y * moveSpeed * deltaTime, room);
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

    // Siehe Damageable-Klassenkommentar - der Angreifer baut die Trefferzone selbst als Shape
    // und testet sie gegen diesen Radius, statt dass Boss die Geometrie kennen muss
    @Override
    public float getHurtboxRadius() {
        return HURTBOX_RADIUS;
    }

    // Reiner ShapeRenderer-Platzhalter (siehe Klassenkommentar) - kurzer heller Blitz bei
    // Treffern, sonst Körperfarbe je nach Phase (dunkelrot in Phase 1, kräftigeres Rot ab Phase
    // 2 - sofort sichtbares Feedback, dass der Phasenwechsel ausgelöst hat). Erwartet ein
    // bereits laufendes ShapeRenderer.begin(ShapeType.Filled) (siehe Main.render())
    public void draw(ShapeRenderer shapeRenderer) {
        Vector2 center = getCenter();

        // ALLE aktiven Angriffe (über alle Kategorien/Slots) zeichnen ihren eigenen Telegraph/
        // Effekt VOR dem Körper, damit der Körper obenauf bleibt (gleiche Reihenfolge wie bisher)
        for (BossAttack active : activeAttacks.values()) {
            active.draw(shapeRenderer);
        }

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
