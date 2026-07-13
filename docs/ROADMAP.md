# Roadmap — Quests

Meilensteine für EmberRougeLike, abgeleitet aus `docs/GDD.md`. Jede Quest ist ein spielbarer/testbarer Zuwachs, keine reine Code-Aufräumarbeit. Reihenfolge folgt den Prioritäten aus dem GDD (Movement/Dash zuerst, dann Combat, dann Räume, dann Progression, Bosse und Prozeduralität zuletzt).

## ✅ Quest 0: Erwachen
Grundbewegung (8-Richtungen, normalisiert, Screen-Clamping). **Bereits erledigt.**

## ✅ Quest 1: Der erste Schritt zur Seite (Dash)
Dash mit fixer Distanz, Cooldown und Invincibility-Frames (siehe GDD 1.2). **Bereits erledigt** — fühlt sich laut Playtest gut an.
- Vorbereitend: Das aktuelle libGDX-Default-Logo wird durch eine einfache flache PNG-Platzhalter-Grafik für den Spieler ersetzt (siehe GDD 5) — kein echtes Sprite, aber ein echter Entity-Platzhalter statt des Engine-Logos.
- Fertig wenn: Spieler kann dashen, ist während des Dashs kurz unverwundbar, Cooldown verhindert Spam.
- Backlog (nicht blockierend): visuelle Cooldown-Anzeige für den Dash — bewusst auf später verschoben.

## ✅ Quest 2: Erste Klinge
Eine Nahkampfwaffe mit eigenem Angriffsmuster, Hitbox-Treffererkennung, ein Dummy-Gegner zum Testen. **Bereits erledigt.**
- Fertig wenn: Spieler kann angreifen, Treffer wird erkannt, Dummy-Gegner nimmt Schaden.
- Umgesetzt: `Sword` per Komposition (`Player` hat-ein `Sword`), Maus-Aim mit Y-Flip-Korrektur, Distanz-basierte Hitbox/Hurtbox-Trennung gegen `Enemy`-Dummy.
- Bewusst vereinfacht (siehe Roadmap-Kommentar in `Player.update()`): `Enemy` wird aktuell als einzelner Parameter durchgereicht, nicht als Liste — wird generalisiert, sobald Quest 4-6 mehrere Gegner gleichzeitig braucht.

## ✅ Quest 3: Erster Pfeil
Eine Fernkampfwaffe (Projektil-System), Wechsel zwischen Nah-/Fernkampf ist hier noch nicht nötig (nur 1 Waffenslot laut GDD) — stattdessen Testumgebung mit beiden Waffentypen gleichzeitig (Schwert links, Bogen rechts). **Bereits erledigt.**
- Fertig wenn: Projektil kann abgefeuert werden, trifft Gegner, wird sauber entsorgt (Objektlebenszyklus!).
- Umgesetzt: `Projectile` als eigenständige, sich über Zeit bewegende Entity (Position, Richtung, Geschwindigkeit, Lebensdauer/Reichweite, `hitsRemaining` als Vorbereitung für spätere Piercing-Effekte). `Bow` verwaltet eine `List<Projectile>` (sicheres Entfernen via `removeIf`), teilt sich eine einzige Textur statt pro Pfeil neu zu laden.
- `Weapon`-Interface aus `Sword` und `Bow` extrahiert (`update(deltaTime, target)`, `tryAttack(origin, direction, target)`) — bewusst nur die Methoden, die wirklich polymorph gleich genutzt werden; Zeichnen bleibt getrennt, da Sword nur Debug-Hitbox zeigt, Bow aber echte Pfeil-Sprites.

## ✅ Quest 4: Der erste Raum
Ein einzelner, in Tiled gebauter Raum mit Gegnern, Tür-Lock bis alle Gegner besiegt sind (siehe GDD 4). **Bereits erledigt.**
- Fertig wenn: Raum lädt aus Tiled-Datei, Tür öffnet sich erst nach Clear.
- Umgesetzt: `Room` lädt/rendert Tiled-Maps (`TmxMapLoader`/`OrthogonalTiledMapRenderer`), liest `EnemySpawn`/`Door`-Objekte aus der Objekt-Ebene. `Door` (rot/grün eingefärbte Platzhalter-Textur) verriegelt, solange die Gegner-Liste nicht leer ist.
- Nebenbei: Feste 640×360-Basisauflösung mit ganzzahlig skaliertem, letterboxtem Viewport (`GameConfig`), sauber auf 1080p/1440p/4K skalierbar; Fullscreen-Taste (`F11`); Gegner-Liste statt Einzel-Parameter über alle Waffen hinweg generalisiert.
- Gelernt: libGDX' `TmxMapLoader` flippt Objekt-Y-Koordinaten beim Laden bereits selbst — kein zusätzlicher manueller Y-Flip nötig (anders als bei der Maus-Position).

## ✅ Quest 5: Gegner, die zurückschlagen
Einfaches Gegner-KI-Grundgerüst (Verfolgen, Angreifen), Schadenssystem für den Spieler (Leben, Game Over). **Bereits erledigt.**
- Fertig wenn: Mindestens 1 Gegnertyp verfolgt/greift an, Spieler kann sterben.
- Umgesetzt: `EnemyState` (CHASING/ATTACKING) mit Hysterese (unterschiedliche Schwellenwerte zum Betreten/Verlassen von ATTACKING, verhindert Zittern an der Grenze). `Player` hat `health`/`isAlive()`/`takeDamage()` (respektiert `isInvincible()` vom Dash). `Main.startGame()` als wiederverwendbarer Aufbau, Game-Over-Zustand friert die Logik ein, `R` startet neu.
- Bewusst zurückgestellt: globale Spieler-Invincibility-Frames nach Treffer (verhindert aktuell nicht, dass mehrere Gegner im selben Frame gleichzeitig treffen) — Balancing-Frage für später.

## ⬜ Quest 6: Der Pfad durch mehrere Räume
Mehrere handgebaute Räume, verbunden zu einem kleinen Level, Übergänge zwischen Räumen.
- Fertig wenn: Spieler kann durch mind. 3-4 verbundene, handgebaute Räume laufen.

## ⬜ Quest 7: Aus der Asche geschmiedet (eigener Art-Pass)
Der Kern-Loop (Movement, Dash, Combat, Räume) steht — Zeit, alle bisherigen Platzhalter-Grafiken durch eigene Pixelart zu ersetzen (siehe GDD 5).
- Fertig wenn: Spieler, Gegner, Waffen/Projektile und Tiles nutzen eigene Sprites statt Platzhalter-Grafiken.
- Technischer Vorlauf (vermerkt 2026-07-13):
  - **Pixel-Snapping:** Sprite-Zeichenposition beim `draw()` auf das nächste ganze logische Pixel runden (z.B. `Math.round(position.x)`), unabhängig von der eigentlichen (weiterhin fließkomma-genauen) Bewegungsberechnung — verhindert Wackeln/Shimmer an Sprite-Rändern bei skaliertem Nearest-Neighbor-Rendering.
  - **Dual-Grid System für Terrain-Übergänge:** Render-Gitter um eine halbe Kachel gegen das Daten-Gitter versetzt; jede Render-Kachel wird aus den 4 angrenzenden Daten-Zellen bestimmt (2⁴ = 16 Kombinationen, 16 Kachel-Grafiken reichen für lückenlose Übergänge, statt bis zu 47 bei klassischem Blob-Autotiling). Kein natives Tiled-Feature (anders als "Wang Sets") — müsste als eigener Rendering-Layer in unserem Code gebaut werden. Besonders wertvoll für Quest 11 (prozedurale Generierung), da Terrain-Typen dort zur Laufzeit von einem Algorithmus entschieden werden, nicht von Hand in Tiled vorgezeichnet.
  - **Tile-Extrusion/Bleeding:** Kleiner Pixel-Rand um jede Kachel im Tileset-Bild, verhindert sichtbare Nahtlinien zwischen Kacheln bei skaliertem Rendering.

## ⬜ Quest 8: Beute und Level-Ups
In-Run-Progression: Raum-Belohnungen und Level-Up-System (siehe GDD 3.1).
- Fertig wenn: Spieler bekommt nach Raum-Clear Belohnungsauswahl, kann durch XP/Ressourcen aufsteigen.
- Technischer Vorlauf: Stat-Konstanten (z.B. `DASH_DISTANCE`, `DASH_SPEED`, später Waffen-Werte) müssen von `static final` Klassenkonstanten zu veränderlichen Instanzfeldern werden, damit Items sie zur Laufzeit modifizieren können (`final` verbietet jede spätere Neuzuweisung).

## ⬜ Quest 9: Der Hub
Meta-Progression zwischen Runs: permanente Währung + Hub-Menü für dauerhafte Upgrades, erste freischaltbare Inhalte (siehe GDD 3.2).
- Fertig wenn: Währung überlebt einen Run-Neustart, mind. 1 dauerhaftes Upgrade ist kaufbar.

## ⬜ Quest 10: Der erste Boss
Ein Boss-Encounter mit eigenem Angriffsmuster/Phasen.
- Fertig wenn: Boss besiegbar, hat mind. 2 unterscheidbare Angriffsmuster.

## ⬜ Quest 11: Das Chaos ordnet sich (Prozedurale Generierung)
Erste Version der prozeduralen Raumkombination, aufbauend auf dem Pool handgebauter Räume aus Quest 4–6 (Ansatz wird dann final entschieden, siehe GDD 4).
- Fertig wenn: Ein Run besteht aus zufällig zusammengesetzten Räumen aus dem bestehenden Raum-Pool.

---
Diese Liste ist ein lebendes Dokument — Reihenfolge/Inhalt kann sich anpassen, sobald wir tiefer in einzelne Quests einsteigen.
