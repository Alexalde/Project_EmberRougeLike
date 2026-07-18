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
