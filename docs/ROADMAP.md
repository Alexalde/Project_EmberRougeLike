# Roadmap — Quests

Meilensteine für EmberRougeLike, abgeleitet aus `docs/GDD.md`. Jede Quest ist ein spielbarer/testbarer Zuwachs, keine reine Code-Aufräumarbeit. Reihenfolge folgt den Prioritäten aus dem GDD (Movement/Dash zuerst, dann Combat, dann Räume, dann Progression, Bosse und Prozeduralität zuletzt).

## ✅ Quest 0: Erwachen
Grundbewegung (8-Richtungen, normalisiert, Screen-Clamping). **Bereits erledigt.**

## ✅ Quest 1: Der erste Schritt zur Seite (Dash)
Dash mit fixer Distanz, Cooldown und Invincibility-Frames (siehe GDD 1.2). **Bereits erledigt** — fühlt sich laut Playtest gut an.
- Vorbereitend: Das aktuelle libGDX-Default-Logo wird durch eine einfache flache PNG-Platzhalter-Grafik für den Spieler ersetzt (siehe GDD 5) — kein echtes Sprite, aber ein echter Entity-Platzhalter statt des Engine-Logos.
- Fertig wenn: Spieler kann dashen, ist während des Dashs kurz unverwundbar, Cooldown verhindert Spam.
- Backlog (nicht blockierend): visuelle Cooldown-Anzeige für den Dash — bewusst auf später verschoben.

## ⬜ Quest 2: Erste Klinge
Eine Nahkampfwaffe mit eigenem Angriffsmuster, Hitbox-Treffererkennung, ein Dummy-Gegner zum Testen.
- Fertig wenn: Spieler kann angreifen, Treffer wird erkannt, Dummy-Gegner nimmt Schaden.

## ⬜ Quest 3: Erster Pfeil
Eine Fernkampfwaffe (Projektil-System), Wechsel zwischen Nah-/Fernkampf ist hier noch nicht nötig (nur 1 Waffenslot laut GDD) — stattdessen z.B. Testumgebung mit beiden Waffentypen separat.
- Fertig wenn: Projektil kann abgefeuert werden, trifft Gegner, wird sauber entsorgt (Objektlebenszyklus!).

## ⬜ Quest 4: Der erste Raum
Ein einzelner, in Tiled gebauter Raum mit Gegnern, Tür-Lock bis alle Gegner besiegt sind (siehe GDD 4).
- Fertig wenn: Raum lädt aus Tiled-Datei, Tür öffnet sich erst nach Clear.

## ⬜ Quest 5: Gegner, die zurückschlagen
Einfaches Gegner-KI-Grundgerüst (Verfolgen, Angreifen), Schadenssystem für den Spieler (Leben, Game Over).
- Fertig wenn: Mindestens 1 Gegnertyp verfolgt/greift an, Spieler kann sterben.

## ⬜ Quest 6: Der Pfad durch mehrere Räume
Mehrere handgebaute Räume, verbunden zu einem kleinen Level, Übergänge zwischen Räumen.
- Fertig wenn: Spieler kann durch mind. 3-4 verbundene, handgebaute Räume laufen.

## ⬜ Quest 7: Aus der Asche geschmiedet (eigener Art-Pass)
Der Kern-Loop (Movement, Dash, Combat, Räume) steht — Zeit, alle bisherigen Platzhalter-Grafiken durch eigene Pixelart zu ersetzen (siehe GDD 5).
- Fertig wenn: Spieler, Gegner, Waffen/Projektile und Tiles nutzen eigene Sprites statt Platzhalter-Grafiken.

## ⬜ Quest 8: Beute und Level-Ups
In-Run-Progression: Raum-Belohnungen und Level-Up-System (siehe GDD 3.1).
- Fertig wenn: Spieler bekommt nach Raum-Clear Belohnungsauswahl, kann durch XP/Ressourcen aufsteigen.

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
