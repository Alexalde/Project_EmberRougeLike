# Kunst-Spezifikation für Quest 7.10 (Aseprite)

Technische Vorgaben für den echten Kunst-Tausch in Quest 7 — die Systeme (Animation, Dual-Grid) sind bereits fertig und funktionieren mit Platzhaltern (Quest 7.3–7.9). Diese Datei legt genau fest, was gezeichnet werden muss, in welcher Auflösung und Anordnung, damit die fertigen Dateien am selben Asset-Pfad die Platzhalter ersetzen können (siehe GDD 5, Platzhalter-Ansatz).

## Grundraster

- **Spieler-Frames: 48×48 Pixel** (geändert 2026-07-17, ursprünglich 32×32 - mehr Detailspielraum fürs Charakter-Design gewünscht). Bewusst größer als die Kachelgröße (32×32, siehe unten) - beide sind im Code vollständig entkoppelt (`Player.SPRITE_WIDTH/HEIGHT` betrifft nur `Player`, nirgendwo sonst verwendet).
- **Gegner-/Terrain-/Wand-Frames bleiben 32×32 Pixel** (`Enemy.SPRITE_WIDTH/HEIGHT`, `DualGridTerrainRenderer.TILE_SIZE` unverändert).
- **Nur eine Blickrichtung pro Zeile nötig** (nach rechts schauend empfohlen) — horizontale Spiegelung für "nach links" passiert bereits automatisch im Code (`Player.draw()`, seit Quest 7.3). Kein doppelter Zeichenaufwand für Links/Rechts.
- Export als PNG mit Alpha-Kanal (transparenter Hintergrund außerhalb der Figur).

## 1. Spieler-Sprite-Sheet (`player_spritesheet.png` + `player_spritesheet.json`)

**Aktualisiert 2026-07-18: kein festes Zeilen/Spalten-Raster mehr.** Stattdessen: eine Aseprite-Datei mit EINER fortlaufenden Timeline, in der jede Animation als eigener, benannter **Tag** markiert ist (Frame-Menü > Tags). Export über `File > Export Sprite Sheet` mit aktiviertem **"Data File"** (JSON) und **deaktiviertem "Trim"**. Beide exportierten Dateien (`player_spritesheet.png`, `player_spritesheet.json`) liegen unter `assets/` — `AsepriteSpriteSheet.java` liest die JSON-Datei und baut daraus pro Tag eine `Animation`.

Frame-Anzahl pro Tag ist bewusst NICHT mehr festgelegt (z.B. Idle hat aktuell 14 Frames) - das übernimmt jetzt Aseprite selbst, inklusive individueller Frame-Dauer pro Bild (z.B. Ease-In/Out-Timing), die für Idle/Walk direkt fürs Abspielen genutzt wird (siehe `Animation.getFrameByElapsedTime()`).

**Exakte Tag-Namen** (müssen genau so geschrieben sein, Groß-/Kleinschreibung und Bindestrich zählen):

| Tag-Name | Zustand |
|---|---|
| `Idle-South` | Idle, Blickrichtung Süden (deckt mit Spiegelung auch Ost/Südost/Südwest/West ab) |
| `Idle-North` | Idle, Blickrichtung Norden (deckt mit Spiegelung auch Nordost/Nordwest ab) |
| `Walk-South` | Walk, Süden |
| `Walk-North` | Walk, Norden |
| `Attack` | Angriff (keine Richtungs-Variante im aktuellen Scope) |
| `Hurt` | Getroffen (keine Richtungs-Variante) |
| `Death` | Sterben (keine Richtungs-Variante) |

Fehlt ein Tag (noch) in der Datei, bleibt für diesen Zustand automatisch der bisherige Pixmap-Platzhalter aktiv (siehe `Player`-Konstruktor, `orPlaceholder()`) - du kannst also Tag für Tag nachliefern, ohne dass der Rest wartet.

## 2. Gegner-Sprite-Sheet (`enemy_spritesheet.png`)

Der Gegner hat (Stand 2026-07-16) noch **keine** Norden-Variante, nur die ursprünglichen 5 Zustände — 5 Zeilen × 3 Spalten, 96×160px, gleiches Reihenfolge-Prinzip wie beim Spieler (Idle/Walk/Attack/Hurt/Death), nur der 1 aktuell umgesetzte Gegner-Typ, eigener Stil/Farbgebung zur Unterscheidung vom Spieler.

## 3. Dual-Grid-Terrain-Tileset (`terrain_dualgrid.png` + `terrain_dualgrid.json`)

**Aktualisiert 2026-07-19: animiert, ohne Tags.** Eine Aseprite-Datei, Canvas **128×128px** (4×4-Raster à 32×32px), Position im Raster = `Zeile = mask / 4`, `Spalte = mask % 4`, passend zu den 16 Bitmask-Werten (siehe `DualGridTerrainRenderer`). Anders als beim Spieler-Sheet gibt es hier **keine Tags** — jeder Frame in der Timeline ist ein VOLLSTÄNDIGES 128×128-Bild mit allen 16 Kacheln zugleich (z.B. 4 Frames für eine Wasser-Ripple-Animation). Export genauso wie beim Spieler (`File > Export Sprite Sheet`, Data File an, Trim aus) - die beiden Dateien liegen unter `assets/`. `DualGridTerrainRenderer` schneidet aus jedem Sheet-Frame die 16 Kachel-Unterbereiche einmalig heraus und spielt sie über einen gemeinsamen Zeit-Zähler synchron ab (alle 16 Kacheln animieren zusammen, nicht unabhängig versetzt).

Jede Kachel ist geviertelt (oben-links/oben-rechts/unten-links/unten-rechts), pro Viertel entweder **Boden** oder **Lücke/Wasser** je nach gesetztem Bit:

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

**Tile-Extrusion** (1px Rand-Bleeding um jede Kachel gegen Nahtlinien bei skaliertem Rendering, siehe ROADMAP Quest 7) ist mit der aktuellen Zuschneide-Logik in `DualGridRenderer` noch NICHT berücksichtigt (schneidet exakt an den 32px-Grenzen, ohne Rand-Puffer) - bei sichtbaren Nahtlinien im echten Spiel müsste das Zuschneiden entsprechend angepasst werden.

## 4. Wände (`walls_dualgrid.png` + `walls_dualgrid.json`)

**Aktualisiert 2026-07-19: Scope-Änderung, Wände sind jetzt auch Dual-Grid** (kehrt die frühere "einfache, nicht-autogekachelte Wände"-Entscheidung um, siehe ROADMAP) - eigenes, komplett unabhängiges Dual-Grid-System von Boden/Terrain (eigenes Daten-Gitter `"WallData"` in Tiled, eigenes Tileset), damit Wand-Kunst nicht pro Boden-Typ neu angepasst werden muss. Exakt derselbe Aufbau wie beim Terrain-Tileset (Abschnitt 3): 128×128px Canvas, 4×4-Raster (`Zeile = mask/4`, `Spalte = mask%4`), keine Tags, jeder Frame ein komplettes Bild mit allen 16 Kombinationen. `true`/Bit gesetzt bedeutet hier "Wand" statt "Lücke/Wasser" - Bitmask-Tabelle aus Abschnitt 3 gilt unverändert (nur "Lücke" gedanklich durch "Wand" ersetzen). Muss nicht zwingend animiert sein (ein einzelner Frame reicht, der Code kommt mit beliebiger Frame-Anzahl klar).

Der alte, einfache Wand-Ansatz (`tileset_placeholder.png`, Kachel-ID 2) bleibt als nicht mehr gerenderte Fallback-Daten im `Ground_LEGACY_unused`-Layer von `testmap.tmx` liegen (siehe ROADMAP für geplantes Aufräumen).

## 5. Healthbar (`healthbar_empty.png` / `healthbar_full.png`)

**Aktualisiert 2026-07-20:** Zwei statische Bilder, **jede beliebige Größe funktioniert** (Code liest Breite/Höhe direkt aus der Textur, keine feste Vorgabe) - kein Aseprite-JSON-Export nötig (noch nicht animiert, siehe ROADMAP-Backlog). `healthbar_empty.png` ist der immer voll sichtbare Rahmen/Hintergrund, `healthbar_full.png` die Füllung - wird nicht gestreckt, sondern von links nach rechts aufgedeckt (Textur-Ausschnitt), je nach Lebens-Anteil. Beide Bilder sollten dieselben Pixel-Maße haben. Transparenter Rand in `healthbar_full.png` (falls vorhanden) wird automatisch erkannt und beim Aufdecken korrekt berücksichtigt (siehe `Healthbar.findContentLeftEdge()`/`findContentRightEdge()`) - kein manuelles Anpassen nötig, wenn die Grafik später ersetzt wird.

## Datei-Ablage

Alle Dateien direkt unter `assets/` (wie die bisherigen Platzhalter), Namen wie oben genannt, damit der Code-Umbau in Quest 7.10 nur Konstanten-/Ladepfad-Änderungen braucht, keine Strukturänderungen.
