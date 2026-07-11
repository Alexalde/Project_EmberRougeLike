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
- Offene Punkte für später: konkrete Waffenliste, Angriffs-Timing/Cooldowns pro Waffe, ob/wie Fähigkeiten (Skills) von Waffen getrennt werden, Treffererkennung (Hitbox-Ansatz).

## 3. Progression

### 3.1 In-Run-Progression

- **Raum-Belohnungen:** Bestimmte Räume (z.B. Schatzräume, Nach-Kampf-Räume) geben Belohnungen, ggf. mit Auswahl zwischen mehreren Optionen.
- **Level-Ups:** Sammeln von Erfahrung/In-Run-Ressourcen schaltet während des Runs Stat-Boosts oder Fähigkeiten frei.
- Einfache Drops (z.B. Heiltränke) sind ergänzend denkbar, aber kein separat priorisiertes System.

### 3.2 Meta-Progression (zwischen Runs)

- **Kombination aus beidem:** Permanente Währung, die zwischen Runs in einem Hub in dauerhafte Upgrades investiert werden kann, **und** freischaltbarer Content über Meilensteine (z.B. neue Waffen nach bestimmten Erfolgen).
- Darf komplexer werden, ist aber gegenüber dem Kern-Gameplay-Gefühl (Movement/Dash/Combat) niedriger priorisiert — Details (genaue Upgrade-Liste, Hub-Struktur) folgen, sobald das Kern-Gameplay steht.

## 4. Level- & Raumkonzept

- **Raumbasierte Struktur mit Clear-Bedingung:** Ein Level besteht aus einzelnen, diskreten Räumen. Türen/Übergänge sind gesperrt, bis alle Gegner im aktuellen Raum besiegt sind — dann öffnet sich der Weg zum nächsten Raum. (Wie Ember Knights/Isaac, nicht ein zusammenhängendes offenes Level.)
- **Level-Tooling:** Räume werden mit **Tiled** gebaut.
- **Prototyp-Phase:** Erst handgebaute Räume/Level, keine Prozeduralität.
- **Prozedurale Generierung (später):** Der konkrete Ansatz (z.B. zufällige Kombination handgebauter Raum-Vorlagen vs. algorithmisch generierter Rauminhalt) wird bewusst erst entschieden, wenn genug handgebaute Räume existieren, um eine informierte Wahl zu treffen. Raumbasierte Struktur wurde aber genau deshalb gewählt, weil sie sich gut für spätere prozedurale Kombination eignet.

## 5. Grafik & Assets

- **Platzhalter-Ansatz:** Solange es noch keine echten Sprites gibt, bekommt jeder Entity-Typ (Spieler, Gegner, Projektile, ...) eine eigene **flache PNG-Platzhalter-Grafik** (einfaches farbiges Quadrat/Kreis o.ä.) statt des aktuellen libGDX-Default-Logos. Vorteil: Das echte Sprite ersetzt später einfach die Datei am selben Asset-Pfad, ohne dass Code angefasst werden muss.
- **Eigene Pixelart (dedizierter Art-Pass):** Kommt erst, **nachdem der Kern-Gameplay-Loop steht** (grob nach Quest 6 "Pfad durch mehrere Räume", siehe `ROADMAP.md`). Begründung: Vorher ändern sich Waffen-Movesets, Dash-Timing und Animationslängen noch häufig — fertige Sprites/Animationen davor zu bauen bedeutet oft doppelte Arbeit bei späteren Anpassungen.
- **Animationsgeschwindigkeit an Stats koppeln (Konzept für Quest 7):** Statt Sprite-Animationen mit einer festen `frameDuration` an Echtzeit zu binden, Frame-Auswahl über einen **Fortschritts-Anteil** steuern: `progress = 1f - (zeitRemaining / gesamtDauer)`, dann `frameIndex = (int)(progress * frameCount)`. Da Stats wie `attackVisualDuration`/`attackCooldownDuration` bereits veränderliche Instanzfelder sind (siehe Abschnitt 2), synchronisiert sich die Animationslänge dadurch automatisch mit einem späteren `attackSpeed`-Stat, ohne einen separaten "Animationsgeschwindigkeit"-Parameter einführen zu müssen. Bereits so vorbereitet in `Sword` (`attackVisualTimeRemaining`/`attackVisualDuration`), auch wenn es noch keine echte Sprite-Animation gibt.

## 6. Scope-Abgrenzung & Non-Goals

Explizit **nicht** Teil des frühen Scopes/MVP:

- **Kein Koop-Multiplayer.** Das Spiel ist als Singleplayer geplant. Architektur soll spätere Koop-Erweiterung aber nicht grundsätzlich ausschließen (keine tief verankerten Singleplayer-Annahmen, wo vermeidbar).
- **Kein Elementar-System.** Nice-to-have, keine frühe Priorität.
- **Keine prozedurale Generierung** in der Prototyp-Phase (siehe Abschnitt 4) — wird ausprobiert, sobald genug handgebaute Räume existieren.
- **Nur 1 Waffenslot**, kein Echtzeit-Wechselsystem zwischen mehreren Waffen (siehe Abschnitt 2).
- **Wenige Gegner- und Raumtypen im MVP** — Vielfalt hier ist explizit für spätere Phasen geplant, nicht für den MVP.
- **Boss-Fights sind Pflicht, aber spätere Phase**, nicht Teil des MVP.
- **Kein Android-/Web-Port.** Nur Desktop (Windows/Linux/macOS via lwjgl3).
- **Sound/Musik ohne frühen Feinschliff.** Im Scope, aber kein früher Fokus.
