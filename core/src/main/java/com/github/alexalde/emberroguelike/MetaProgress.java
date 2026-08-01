package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;

import java.util.ArrayList;
import java.util.List;

// Persistenter Charakterbogen (Pendant zu PlayerStats, aber Run-übergreifend, siehe GDD 3.2/
// Quest 9) - überlebt sowohl Main.startGame()s Player-Neuerstellung als auch einen App-Neustart
// (eigene JSON-Datei, siehe load()/save()). Öffentliche Felder wie PlayerStats/GameSettings.
public class MetaProgress {

    private static final String SAVE_FILE_PATH = "save.json";

    public float upgradeCurrency = 0f;
    public float unlockCurrency = 0f;
    // IDs gekaufter PermanentUpgrades (siehe PermanentUpgrade.getId()) - wird bei jedem neuen
    // Run erneut auf einen frischen Player angewendet (siehe Player-Konstruktor). Bewusst eine
    // List statt eines Set (siehe Quest 9.1: libGDX' Json liest JSON-Arrays standardmäßig als
    // ArrayList - ein Set-Feld führte beim Laden zu einer ReflectionException). Duplikate werden
    // stattdessen beim Kauf selbst verhindert (siehe UpgradeShopUI/PermanentUpgrade, Quest 9.5).
    public List<String> purchasedUpgradeIds = new ArrayList<>();

    // Zentrale, "immer ansprechbare" Vergabe-Schnittstelle (siehe GDD 3.2) - jede zukünftige
    // Quelle (Pickups, später ein Achievement-System) ruft dieselbe Methode auf, statt die
    // Felder direkt zu manipulieren
    public void addCurrency(MetaCurrencyType type, float amount) {
        if (type == MetaCurrencyType.UPGRADE) {
            upgradeCurrency += amount;
        } else {
            unlockCurrency += amount;
        }
    }

    public float getCurrency(MetaCurrencyType type) {
        return type == MetaCurrencyType.UPGRADE ? upgradeCurrency : unlockCurrency;
    }

    // true, wenn genug Guthaben da war UND der Betrag abgezogen wurde - false, wenn zu wenig
    // Guthaben vorhanden ist (dann bleibt alles unverändert)
    public boolean spendCurrency(MetaCurrencyType type, float amount) {
        if (getCurrency(type) < amount) {
            return false;
        }
        if (type == MetaCurrencyType.UPGRADE) {
            upgradeCurrency -= amount;
        } else {
            unlockCurrency -= amount;
        }
        return true;
    }

    // Lädt einen bestehenden Spielstand, oder liefert einen frischen (Standardwerte), falls noch
    // keine Datei existiert (z.B. beim allerersten Start) - Gdx.files.local() zeigt auf einen
    // SCHREIBBAREN Ordner (anders als Gdx.files.internal() für Assets)
    public static MetaProgress load() {
        FileHandle file = Gdx.files.local(SAVE_FILE_PATH);
        if (!file.exists()) {
            return new MetaProgress();
        }
        return createJson().fromJson(MetaProgress.class, file);
    }

    public void save() {
        Gdx.files.local(SAVE_FILE_PATH).writeString(createJson().prettyPrint(this), false);
    }

    // setTypeName(null): unterdrückt die sonst von libGDX' Json eingestreuten "class"-Hinweise -
    // wir kennen die Typen ohnehin fest aus dem Code, das hält die Datei lesbar ("eigene JSON-
    // Datei" war der Nutzer-Wunsch). setElementType sagt Json, dass die Set-Einträge simple
    // Strings sind (sonst müsste es das beim Laden selbst erraten).
    private static Json createJson() {
        Json json = new Json();
        json.setTypeName(null);
        json.setElementType(MetaProgress.class, "purchasedUpgradeIds", String.class);
        return json;
    }
}
