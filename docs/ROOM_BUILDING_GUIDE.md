# Anleitung: Neuen, verbundenen Raum bauen

Referenzbeispiel: `assets/maps/room_template.tmx`, verbunden mit `assets/maps/testmap.tmx` über die Türen `"north"` (in testmap) und `"south"` (in room_template). Öffne beide in Tiled und vergleiche sie parallel zu dieser Anleitung.

## 1. Neue Map anlegen

Wie beim ersten Testraum: `File → New → New Map...`
- Orientation: Orthogonal, Tile size: 32×32
- Größe frei wählbar — für Verzweigungen gerne auch größer als 20×11 (unser `room_template.tmx` ist z.B. 30×12, damit die scrollende Kamera überhaupt etwas zu tun hat)
- Speichern unter `assets/maps/<name>.tmx`

## 2. Tileset einbinden

Tileset-Panel → `New Tileset...` → `assets/tileset_placeholder.png`, Kachelgröße 32×32. (Kannst auch das bestehende `.tsx` aus `testmap.tmx` wiederverwenden, falls Tiled das anbietet — spart einen Schritt.)

## 3. Ground-Layer + Objects-Ebene

Wie gehabt: Boden-Layer mit Kachel 0 (Boden) füllen, Kachel 1 (Wand) als Rahmen. Neue Objekt-Ebene `"Objects"`.

## 4. Gegner-Spawns

Punkt-Objekte einfügen, Class/Type auf `EnemySpawn` setzen (Dropdown, dank Custom Types) — wie bisher, keine neuen Felder nötig.

## 5. Türen — der neue, wichtige Teil

Für **jede** Tür in diesem Raum:
1. Punkt-Objekt einfügen, Class/Type auf `Door` setzen.
2. **Name-Feld ausfüllen** (im Properties-Panel, nicht "Class"!) — ein eindeutiger Bezeichner für *diese* Tür innerhalb *dieses* Raums, z.B. `"north"`, `"south"`, `"east_1"`. Andere Räume referenzieren diesen Namen später.
3. Zwei Custom Properties ausfüllen (erscheinen automatisch als Felder, weil wir sie der `Door`-Class hinzugefügt haben):
   - **`targetRoom`**: Pfad zur Zielraum-Datei, relativ zum `assets`-Ordner, z.B. `maps/room_template.tmx`
   - **`targetDoorName`**: der `Name` der Tür im Zielraum, an der der Spieler ankommen soll, z.B. `"south"`

**Wichtig:** Die Verknüpfung muss von **beiden Seiten** gepflegt werden — die Tür in Raum A braucht `targetRoom`/`targetDoorName`, die Richtung Raum B zeigen, UND die entsprechende Tür in Raum B braucht die Gegenrichtung zurück nach Raum A. Schau dir `testmap.tmx`s `"north"`-Tür und `room_template.tmx`s `"south"`-Tür als Beispiel für so ein Paar an.

## 6. Für ein verzweigtes Layout

Ein Raum kann mehrere Türen haben (z.B. `"north"`, `"east"`) — jede mit eigenen `targetRoom`/`targetDoorName`-Werten, die zu unterschiedlichen Nachbarräumen zeigen. Plant euch am besten kurz auf Papier/im Kopf, welcher Raum mit welchem über welche Tür verbunden sein soll, bevor ihr lostippt — das erspart nachträgliches Umbenennen.

## 7. Speichern

Fertig — der Code (`Room.java`) liest `targetRoom`/`targetDoorName` bereits automatisch mit ein. Die eigentliche Wechsel-Logik (was beim Kollidieren mit einer entriegelten Tür passiert) folgt in einem separaten Schritt.
