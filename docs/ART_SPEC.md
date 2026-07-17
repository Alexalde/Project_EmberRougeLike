# Kunst-Spezifikation für Quest 7.10 (Aseprite)

Technische Vorgaben für den echten Kunst-Tausch in Quest 7 — die Systeme (Animation, Dual-Grid) sind bereits fertig und funktionieren mit Platzhaltern (Quest 7.3–7.9). Diese Datei legt genau fest, was gezeichnet werden muss, in welcher Auflösung und Anordnung, damit die fertigen Dateien am selben Asset-Pfad die Platzhalter ersetzen können (siehe GDD 5, Platzhalter-Ansatz).

## Grundraster

- **Spieler-Frames: 48×48 Pixel** (geändert 2026-07-17, ursprünglich 32×32 - mehr Detailspielraum fürs Charakter-Design gewünscht). Bewusst größer als die Kachelgröße (32×32, siehe unten) - beide sind im Code vollständig entkoppelt (`Player.SPRITE_WIDTH/HEIGHT` betrifft nur `Player`, nirgendwo sonst verwendet).
- **Gegner-/Terrain-/Wand-Frames bleiben 32×32 Pixel** (`Enemy.SPRITE_WIDTH/HEIGHT`, `DualGridTerrainRenderer.TILE_SIZE` unverändert).
- **Nur eine Blickrichtung pro Zeile nötig** (nach rechts schauend empfohlen) — horizontale Spiegelung für "nach links" passiert bereits automatisch im Code (`Player.draw()`, seit Quest 7.3). Kein doppelter Zeichenaufwand für Links/Rechts.
- Export als PNG mit Alpha-Kanal (transparenter Hintergrund außerhalb der Figur).

## 1. Spieler-Sprite-Sheet (`player_spritesheet.png`)

Ein Sprite-Sheet, **7 Zeilen × 3 Spalten**, jede Zelle **48×48px** → Gesamtgröße **144×336px**.

Der Spieler hat für Idle/Walk zwei Blickrichtungs-Varianten (Süden/Norden, seit Quest 7.10-Vorbereitung 2026-07-16): Süden deckt zusammen mit der horizontalen Spiegelung Ost/Südost/Süd/Südwest/West ab, Norden deckt Nordost/Nord/Nordwest ab. Attack/Hurt/Death bleiben bei nur einer Variante (kein Richtungs-Wechsel für diese Zustände im aktuellen Scope).

| Zeile | Zustand | Frames | Genutzte Spalten |
|---|---|---|---|
| 0 (oben) | Idle (Süden) | 1 | Spalte 0, Spalten 1-2 leer/transparent lassen |
| 1 | Idle (Norden) | 1 | Spalte 0, Spalten 1-2 leer |
| 2 | Walk (Süden) | 2 | Spalten 0-1, Spalte 2 leer |
| 3 | Walk (Norden) | 2 | Spalten 0-1, Spalte 2 leer |
| 4 | Attack | 2 | Spalten 0-1, Spalte 2 leer |
| 5 | Hurt | 1 | Spalte 0, Spalten 1-2 leer |
| 6 (unten) | Death | 3 | Spalten 0-2 (volle Zeile) |

Zeilenreihenfolge ist wichtig — sie muss zur Lade-Reihenfolge im Code passen (`TextureRegion.split()` liest zeilenweise von oben).

## 2. Gegner-Sprite-Sheet (`enemy_spritesheet.png`)

Der Gegner hat (Stand 2026-07-16) noch **keine** Norden-Variante, nur die ursprünglichen 5 Zustände — 5 Zeilen × 3 Spalten, 96×160px, gleiches Reihenfolge-Prinzip wie beim Spieler (Idle/Walk/Attack/Hurt/Death), nur der 1 aktuell umgesetzte Gegner-Typ, eigener Stil/Farbgebung zur Unterscheidung vom Spieler.

## 3. Dual-Grid-Terrain-Tileset (`terrain_dualgrid.png`)

Ein Tileset, **4 Zeilen × 4 Spalten**, jede Kachel 32×32px → Gesamtgröße **128×128px**. Jede der 16 Kacheln entspricht einer Bitmask 0–15 (siehe `DualGridTerrainRenderer`), Position im Raster = `Zeile = mask / 4`, `Spalte = mask % 4`.

Jede Kachel ist geviertelt (oben-links/oben-rechts/unten-links/unten-rechts), pro Viertel entweder **Boden** oder **Lücke/Wasser** je nach gesetztem Bit:

| Mask | Bits (bottomLeft/bottomRight/topLeft/topRight) | Beschreibung |
|---|---|---|
| 0 | - / - / - / - | alles Boden |
| 1 | ✓ / - / - / - | nur unten-links Lücke |
| 2 | - / ✓ / - / - | nur unten-rechts Lücke |
| 3 | ✓ / ✓ / - / - | untere Hälfte Lücke |
| 4 | - / - / ✓ / - | nur oben-links Lücke |
| 5 | ✓ / - / ✓ / - | linke Hälfte Lücke |
| 6 | - / ✓ / ✓ / - | Diagonale (oben-links + unten-rechts) Lücke |
| 7 | ✓ / ✓ / ✓ / - | nur oben-rechts Boden |
| 8 | - / - / - / ✓ | nur oben-rechts Lücke |
| 9 | ✓ / - / - / ✓ | Diagonale (oben-rechts + unten-links) Lücke |
| 10 | - / ✓ / - / ✓ | rechte Hälfte Lücke |
| 11 | ✓ / ✓ / - / ✓ | nur oben-links Boden |
| 12 | - / - / ✓ / ✓ | obere Hälfte Lücke |
| 13 | ✓ / - / ✓ / ✓ | nur unten-rechts Boden |
| 14 | - / ✓ / ✓ / ✓ | nur unten-links Boden |
| 15 | ✓ / ✓ / ✓ / ✓ | alles Lücke/Wasser |

Für nahtlose Übergänge zwischen benachbarten Kacheln sollten sich die Ränder der Boden- und Lücken-Zeichnung an den Kachel-Grenzen treffen (z.B. eine Wasserkante, die an der Kachel-Mitte beginnt/endet).

**Tile-Extrusion** (1px Rand-Bleeding um jede Kachel gegen Nahtlinien bei skaliertem Rendering, siehe ROADMAP Quest 7) wird beim Zuschneiden im Code nachgezogen (Slicing-Logik muss dafür beim Kunst-Tausch angepasst werden, aktuell lädt `DualGridTerrainRenderer` noch 16 laufzeit-generierte Platzhalter statt eines echten Tilesets - das Zuschneiden aus einem Sheet ist selbst noch zu bauen, keine reine Asset-Ersetzung).

## 4. Wände (`tileset_placeholder.png`, wird ersetzt)

Einfach, nicht autogekachelt (siehe Scope-Entscheidung) — eine einzelne Wand-Kachel reicht, ersetzt Kachel-ID 2 im bestehenden 2-Kacheln-Tileset (64×32px, 2×32×32-Raster). Kachel-ID 1 (bisher Boden) wird für `testmap.tmx` nicht mehr gebraucht, bleibt aber für die anderen, noch nicht auf Dual-Grid umgestellten Räume relevant (siehe ROADMAP Quest 7.9 Non-Goal).

## 5. Healthbar (`healthbar_frame.png` / `healthbar_fill.png`)

Verfeinerter, aber nicht finaler Platzhalter (siehe Vertical-Slice-Scope). Aktuelle Platzhalter-Maße in `Healthbar.java`: Rahmen 100×12px, 2px Rand. Reale Kunst kann diese Maße beibehalten oder der Code wird entsprechend angepasst — das ist beim Kunst-Tausch selbst zu entscheiden, keine feste Vorgabe.

## Datei-Ablage

Alle Dateien direkt unter `assets/` (wie die bisherigen Platzhalter), Namen wie oben genannt, damit der Code-Umbau in Quest 7.10 nur Konstanten-/Ladepfad-Änderungen braucht, keine Strukturänderungen.
