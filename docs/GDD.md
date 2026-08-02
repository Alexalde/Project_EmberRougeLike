# Game Design Document — EmberRougeLike

Lernprojekt: 2D Top-Down Action-Roguelike/Roguelite, orientiert an "Ember Knights". Primäres Ziel ist das intensive Erlernen von Java, OOP und guten Software-Praktiken — das Spiel ist Vehikel für den Lernprozess.

Status: In Erarbeitung, wird Schritt für Schritt gemeinsam ausgebaut.

## 1. Kernmechaniken

### 1.1 Movement

- Bewegung ist **abrupt/sofort** (kein Anlaufen oder Ausrollen) — Charakter reagiert direkt auf Input, wie in Ember Knights.
- 8-Richtungs-Movement, diagonal normalisiert (bereits implementiert in `Player`).
- Basisgeschwindigkeit aktuell: `300f` Einheiten/Sekunde (Platzhalterwert, wird später balanciert).

### 1.2 Dash

- **Typ:** Fixe kurze Distanz in Bewegungs-/Blickrichtung, danach Cooldown bis zum nächsten Dash verfügbar ist (kein zeitbasierter Geschwindigkeits-Boost).
- **Invincibility-Frames:** Ja — während der Dash-Bewegung ist der Spieler kurzzeitig unverwundbar. Macht den Dash zu einem taktischen Ausweich-Tool, nicht nur Mobilität.
- Offene Parameter (werden beim Implementieren festgelegt/balanciert): Dash-Distanz, Dash-Dauer, Cooldown-Dauer, Länge des i-Frame-Fensters relativ zur Dash-Dauer.

## 2. Combat & Waffensystem

- **Waffenslots:** Der Spieler trägt **eine aktive Waffe** zur Zeit (MVP-Vereinfachung). Mehrere Waffenslots mit Echtzeit-Wechsel sind eine mögliche spätere Erweiterung, aber nicht Teil des MVP.
- **Movesets statt reiner Stats:** Jede Waffe hat ein **eigenes Angriffsmuster/Moveset** (z.B. Schwert = schnelle Nahkampf-Combo, Bogen = gezielter Fernkampf-Schuss), nicht nur unterschiedliche Zahlenwerte auf derselben Animation. Das ist bewusst mehr Aufwand, aber Kernbestandteil des Ember-Knights-Gefühls — und guter Lernstoff für polymorphes Verhalten (z.B. ein gemeinsames Interface/abstrakte Klasse für "Waffe", die von jedem konkreten Waffentyp unterschiedlich implementiert wird).
- **Nah- und Fernkampf von Anfang an:** Das MVP deckt beide Kategorien ab (z.B. mindestens eine Nahkampf- und eine Fernkampfwaffe), nicht nur eine Kategorie zuerst.
- Offene Punkte für später: konkrete Waffenliste, ob/wie Fähigkeiten (Skills) von Waffen getrennt werden.

### 2.1 Stat-System (Waffen-Basiswert × Spieler-weite Stats)

**Entschieden UND umgesetzt 2026-07-21** (Quest 8, löst den bisher offenen Punkt "Waffen-Werte/Angriffs-Timing" oben ab): Jede Waffe liefert einen **Basiswert** pro Angriff (`baseDamage`, `baseAttackSpeed`), der Spieler hält **globale Stats**, die diesen Basiswert modifizieren - statt wie bisher (`Sword`/`Bow`) einen fest codierten Einzelwert pro Waffe.

**Formel-Grundgerüst:**
```
finalDamage = weapon.baseDamage * player.damageMultiplier
if (critRoll < player.critChance) {
    finalDamage *= player.critDamageMultiplier
}
```

**Spieler-weite Stats** (komplettes Set gleich mit angelegt, auch wenn manche vorerst ungenutzte Platzhalter bleiben - vermeidet, dass Quest 8 sofort wieder neue Felder nachziehen muss):

| Stat | Bedeutung |
|---|---|
| `damageMultiplier` | Multipliziert jeden Waffen-Basisschaden |
| `attackSpeedMultiplier` | Skaliert Angriffs-Cooldowns/-Timing (analog zu `attackVisualDuration`/`attackCooldownDuration`) |
| `critChance` | Wahrscheinlichkeit (0-1) für einen kritischen Treffer |
| `critDamageMultiplier` | Zusätzlicher Multiplikator bei einem Crit |
| `projectileCount` | Zusätzliche Projektile pro Schuss (z.B. Bogen) |
| `cooldownReduction` | Wirkt auf Dash-Cooldown, später Waffen-/Fähigkeiten-Cooldowns |
| `moveSpeed`, `lifeSteal`, `range` | Vorerst ungenutzte Platzhalter-Felder, für spätere Items/Level-Ups vorgesehen |
| `projectileSpeedMultiplier` | Multiplikator auf die Projektil-Fluggeschwindigkeit (z.B. Bogen) - noch ungenutzt |
| `xpMultiplier` | Multiplikator auf jede eingesammelte XP-Menge (siehe `XpPickup`) - noch ungenutzt |
| `bounceCount` | Zusätzliche Abprall-/Ricochet-Ladungen für Projektile mit dieser Eigenschaft (über Items ODER spezifische Waffen) - noch ungenutzt, braucht eigenes Bounce-Verhalten in `Projectile` (Zielwahl beim Abprall etc.) |

**Schadenstypen von Anfang an typisiert** (nicht nur ein generischer "Damage"-Wert): eigenes `DamageType`-Enum (z.B. `PHYSICAL`, `FIRE`, `POISON` - Liste wächst nach Bedarf, kein abschließender Satz jetzt nötig). Jede Waffe hat einen `DamageType` (z.B. Schwert = `PHYSICAL`). Ermöglicht später typ-abhängige Debuffs/Buffs (z.B. "+50% Schaden gegen brennende Gegner") nativ, ohne späteren Umbau der Grundstruktur.

**Erweiterbarkeit für typ-/bedingungsabhängige Modifikatoren:** Statt fester Felder/Maps für jede denkbare Bedingung eine Liste **polymorpher Modifier-Objekte** auf dem Spieler (z.B. ein `DamageModifier`-Interface, das prüft ob es auf einen konkreten Treffer "zutrifft" und den Schaden entsprechend anpasst) - neue Debuff-/Buff-Typen kommen als neue Klasse dazu, ohne bestehenden Code anzufassen (Open-Closed-Prinzip). Bewusst als fortgeschritteneres OOP-Konzept markiert (Nutzer-Entscheidung 2026-07-21, "am einfachsten später skalierbar" hatte Vorrang vor "einfach jetzt umzusetzen"): wird zunächst direkt implementiert, der Lerninhalt dahinter aber bewusst Stück für Stück in späteren Sessions nachgeholt (als eigener Task vermerkt, gleiches Vorgehen wie bei `Animation.getFrameByElapsedTime()`).

## 3. Progression

### 3.1 In-Run-Progression

- **Raum-Belohnungen:** Bestimmte Räume (z.B. Schatzräume, Nach-Kampf-Räume) geben Belohnungen, ggf. mit Auswahl zwischen mehreren Optionen.
- **Level-Ups:** Sammeln von Erfahrung/In-Run-Ressourcen schaltet während des Runs Stat-Boosts oder Fähigkeiten frei.
- Einfache Drops (z.B. Heiltränke) sind ergänzend denkbar, aber kein separat priorisiertes System.
- **Umgesetzt (Quest 8, 2026-07-21):** Jeder Raum-Clear (nicht nur dedizierte Schatzräume) UND jeder Level-Up lösen dieselbe Auswahl aus 3 zufälligen `Item`s aus (`RewardChoiceUI`, siehe ROADMAP Quest 8) - "Schatzräume" als eigener, gezielt markierter Raumtyp ist eine mögliche spätere Verfeinerung, kein aktueller Bestandteil.
- **XP als Pickup statt Sofort-Gutschrift (präzisiert 2026-07-21):** Gegner geben beim Tod KEINE XP mehr direkt - stattdessen droppen sie einen `XpPickup` an ihrer Position, der erst eingesammelt werden muss (Kontakt mit dem Spieler). Bewusst über eine gemeinsame `Pickup`-Basisklasse gebaut (nicht XP-spezifisch), damit sich später auch 1-2 Meta-Währungen (siehe 3.2) über eine eigene `Pickup`-Unterklasse genauso verhalten, ohne `Pickup` selbst anzufassen.

### 3.2 Meta-Progression (zwischen Runs)

- **Kombination aus beidem:** Permanente Währung, die zwischen Runs in einem Hub in dauerhafte Upgrades investiert werden kann, **und** freischaltbarer Content über Meilensteine (z.B. neue Waffen nach bestimmten Erfolgen).
- Darf komplexer werden, ist aber gegenüber dem Kern-Gameplay-Gefühl (Movement/Dash/Combat) niedriger priorisiert — Details (genaue Upgrade-Liste, Hub-Struktur) folgen, sobald das Kern-Gameplay steht.
- **Umgesetzt (Quest 9, 2026-08-02):** Zwei Währungen (`UPGRADE`, `UNLOCK`), gespeichert in einer eigenen `save.json` (siehe ROADMAP Quest 9 für alle technischen Details). Der Hub ist ein physischer, feindfreier Raum mit einem zentralen Terminal, an dem permanente Stat-Upgrades gekauft werden - aktuell ein einziges (`+25 Max-HP`). `UNLOCK`-Währung ist verdienbar/speicherbar, hat aber noch keine Ausgabemöglichkeit (kein Waffen-Freischalt-System existiert bisher) - bewusst als nächster Schritt zurückgestellt.

## 4. Level- & Raumkonzept

- **Raumbasierte Struktur mit Clear-Bedingung:** Ein Level besteht aus einzelnen, diskreten Räumen. Türen/Übergänge sind gesperrt, bis alle Gegner im aktuellen Raum besiegt sind — dann öffnet sich der Weg zum nächsten Raum. (Wie Ember Knights/Isaac, nicht ein zusammenhängendes offenes Level.)
- **Level-Tooling:** Räume werden mit **Tiled** gebaut.
- **Prototyp-Phase:** Erst handgebaute Räume/Level, keine Prozeduralität.
- **Prozedurale Generierung (später):** Der konkrete Ansatz (z.B. zufällige Kombination handgebauter Raum-Vorlagen vs. algorithmisch generierter Rauminhalt) wird bewusst erst entschieden, wenn genug handgebaute Räume existieren, um eine informierte Wahl zu treffen. Raumbasierte Struktur wurde aber genau deshalb gewählt, weil sie sich gut für spätere prozedurale Kombination eignet.

## 5. Grafik & Assets

- **Platzhalter-Ansatz:** Solange es noch keine echten Sprites gibt, bekommt jeder Entity-Typ (Spieler, Gegner, Projektile, ...) eine eigene **flache PNG-Platzhalter-Grafik** (einfaches farbiges Quadrat/Kreis o.ä.) statt des aktuellen libGDX-Default-Logos. Vorteil: Das echte Sprite ersetzt später einfach die Datei am selben Asset-Pfad, ohne dass Code angefasst werden muss.
- **Eigene Pixelart (dedizierter Art-Pass):** Kommt erst, **nachdem der Kern-Gameplay-Loop steht** (grob nach Quest 6 "Pfad durch mehrere Räume", siehe `ROADMAP.md`). Begründung: Vorher ändern sich Waffen-Movesets, Dash-Timing und Animationslängen noch häufig — fertige Sprites/Animationen davor zu bauen bedeutet oft doppelte Arbeit bei späteren Anpassungen.
- **Werkzeug:** Aseprite (selbst aus dem Quellcode gebaut), Entscheidung ursprünglich am 2026-07-13 für LibreSprite (Aseprite-Fork) getroffen, am 2026-07-16 auf ein selbstgebautes echtes Aseprite aktualisiert — gezielter für Pixelart als Photoshop (Onion-Skinning, Palette-Verwaltung, Sprite-Sheet-Export inkl. Tags/JSON-Export und eingebauter Tile-Extrusion beim Export).
- **Animationsgeschwindigkeit an Stats koppeln (Konzept für Quest 7):** Statt Sprite-Animationen mit einer festen `frameDuration` an Echtzeit zu binden, Frame-Auswahl über einen **Fortschritts-Anteil** steuern: `progress = 1f - (zeitRemaining / gesamtDauer)`, dann `frameIndex = (int)(progress * frameCount)`. Da Stats wie `attackVisualDuration`/`attackCooldownDuration` bereits veränderliche Instanzfelder sind (siehe Abschnitt 2), synchronisiert sich die Animationslänge dadurch automatisch mit einem späteren `attackSpeed`-Stat, ohne einen separaten "Animationsgeschwindigkeit"-Parameter einführen zu müssen. Bereits so vorbereitet in `Sword` (`attackVisualTimeRemaining`/`attackVisualDuration`), auch wenn es noch keine echte Sprite-Animation gibt.
- **Quest-7-Scope-Entscheidung (vermerkt 2026-07-15): Vertical Slice statt Breite.** Bewusst wenige Dinge bekommen Kunst, aber die dahinterliegenden Systeme sollen bereits strukturell so funktionieren wie in der finalen Version (auch wenn die Pixelart selbst noch nicht final ist) — siehe "Vertical Slice"-Prinzip in Abschnitt 6:
  - Nur **Spieler + 1 Enemy-Typ** bekommen echte State-Animationen (Idle/Walk/Attack/Hurt/Death, wenige Frames), verdrahtet über das Progress-basierte Animationssystem oben. **Ausnahme (Entscheidung 2026-07-21):** Player-Attack/Hurt bleiben bewusst zurückgestellt, siehe ROADMAP Quest 7 - die Attack-Pose hängt von der geführten Waffe ab, die im aktuellen Vertical Slice noch Platzhalter ist. Enemy (Slime) hat alle 5 Zustände als echte Kunst.
  - Nur **1 Bodenset mit Dual-Grid** (Boden + eine "Lücken"-Variante wie Wasser/Löcher als zweites Terrain, alle 16 Kombinationen) plus **einfache, nicht-autogekachelte Wände**.
  - **UI-Grundgerüst (Healthbar etc.) wird ebenfalls schon gepixelt** (verfeinerter Platzhalter, nicht final) — früh sichtbar/relevant genug, um nicht bis zu einem späteren Art-Pass zu warten.
  - Waffen/Projektile/Türen bleiben bewusst unverändert als reine Platzhalter, kommen in einem späteren Art-Pass.

## 6. Scope-Abgrenzung & Non-Goals

**Grundprinzip: Vertical Slice statt Breite (vermerkt 2026-07-15).** Ein "Vertical Slice" ist ein schmaler, aber vollständiger und bereits poliert funktionierender Durchstich durch den gesamten Game-Loop — bewusst wenig Inhalt/Umfang, aber das, was existiert, funktioniert strukturell so wie in der finalen Version, statt später nochmal umgebaut werden zu müssen. In der Spielebranche oft für Marketing/Finanzierung genutzt; hier ist der Zweck stattdessen, Systeme (z.B. Animations- oder Dual-Grid-System, siehe Abschnitt 5) einmal end-to-end sauber zu bauen und zu verstehen, bevor der Aufwand durch mehr Inhalt (Gegnertypen, Waffen, Tilesets) multipliziert wird. War implizit schon die Strategie hinter Quests 0-6 (voller, aber schmaler Kern-Loop mit Platzhaltern vor jeglicher inhaltlicher Breite) und wird ab Quest 7 auch explizit so benannt/angewendet — inklusive rekursiv auf einzelne Quests (siehe Quest-7-Scope in Abschnitt 5).

Explizit **nicht** Teil des frühen Scopes/MVP:

- **Kein Koop-Multiplayer.** Das Spiel ist als Singleplayer geplant. Architektur soll spätere Koop-Erweiterung aber nicht grundsätzlich ausschließen (keine tief verankerten Singleplayer-Annahmen, wo vermeidbar).
- **Kein Elementar-System.** Nice-to-have, keine frühe Priorität.
- **Keine prozedurale Generierung** in der Prototyp-Phase (siehe Abschnitt 4) — wird ausprobiert, sobald genug handgebaute Räume existieren.
- **Nur 1 Waffenslot**, kein Echtzeit-Wechselsystem zwischen mehreren Waffen (siehe Abschnitt 2).
- **Wenige Gegner- und Raumtypen im MVP** — Vielfalt hier ist explizit für spätere Phasen geplant, nicht für den MVP.
- **Boss-Fights sind Pflicht, aber spätere Phase**, nicht Teil des MVP.
- **Kein Android-/Web-Port.** Nur Desktop (Windows/Linux/macOS via lwjgl3).
- **Sound/Musik ohne frühen Feinschliff.** Im Scope, aber kein früher Fokus.
