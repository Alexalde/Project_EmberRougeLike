package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.HashMap;
import java.util.Map;

// Lädt ein von Aseprite exportiertes Sprite-Sheet (PNG + JSON, "Export Sprite Sheet" mit
// aktiviertem "Data File", "Trim" DEAKTIVIERT) und baut daraus eine Animation PRO TAG (siehe
// Aseprite-Tags, z.B. "Idle-South", "Walk-North") - ersetzt schrittweise die laufzeit-
// generierten Pixmap-Platzhalter, sobald ein Tag mit echter Kunst existiert (siehe Player-
// Konstruktor: fehlt ein Tag noch, bleibt der bisherige Platzhalter aktiv).
public class AsepriteSpriteSheet {

    private final Texture texture;
    private final Map<String, Animation> animationsByTag;
    // Alle Frames in Original-Reihenfolge, unabhängig von Tags - siehe getAllFramesAnimation()
    private final TextureRegion[] allFrames;
    private final float[] allDurationsSeconds;

    public AsepriteSpriteSheet(String pngPath, String jsonPath) {
        this.texture = new Texture(pngPath);

        JsonValue root = new JsonReader().parse(Gdx.files.internal(jsonPath));
        JsonValue framesJson = root.get("frames");

        // "frames" ist im JSON ein OBJEKT (Schlüssel = Frame-Name), keine Liste - aber die
        // Reihenfolge der Kind-Elemente entspricht der echten Frame-Reihenfolge (JsonValue
        // behält die Dokument-Reihenfolge bei), und frameTags[].from/to referenzieren genau
        // diese Positionen
        this.allFrames = new TextureRegion[framesJson.size];
        this.allDurationsSeconds = new float[framesJson.size];

        int i = 0;
        for (JsonValue frameEntry = framesJson.child; frameEntry != null; frameEntry = frameEntry.next) {
            JsonValue rect = frameEntry.get("frame");
            allFrames[i] = new TextureRegion(texture, rect.getInt("x"), rect.getInt("y"), rect.getInt("w"), rect.getInt("h"));
            allDurationsSeconds[i] = frameEntry.getInt("duration") / 1000f;
            i++;
        }

        this.animationsByTag = new HashMap<>();
        JsonValue frameTags = root.get("meta").get("frameTags");
        for (JsonValue tag = frameTags.child; tag != null; tag = tag.next) {
            String name = tag.getString("name");
            int from = tag.getInt("from");
            int to = tag.getInt("to");
            int frameCount = to - from + 1;

            TextureRegion[] tagFrames = new TextureRegion[frameCount];
            float[] tagDurations = new float[frameCount];
            System.arraycopy(allFrames, from, tagFrames, 0, frameCount);
            System.arraycopy(allDurationsSeconds, from, tagDurations, 0, frameCount);

            animationsByTag.put(name, new Animation(tagFrames, tagDurations));
        }
    }

    // null, wenn dieser Tag (noch) nicht existiert - Aufrufer soll dann auf einen
    // Pixmap-Platzhalter zurückfallen
    public Animation getAnimation(String tagName) {
        return animationsByTag.get(tagName);
    }

    // Alle Frames der Datei in Original-Reihenfolge, unabhängig von Tags - für Sprite-Sheets,
    // die als GANZES animiert werden (z.B. das Dual-Grid-Tileset: jeder Frame ist ein
    // vollständiges Bild mit allen 16 Kacheln drin), statt separater Tags pro Unter-Animation.
    // Roh-Arrays statt einer fertigen Animation, weil der Aufrufer daraus erst noch
    // Unter-Bereiche herausschneidet (siehe DualGridTerrainRenderer).
    public TextureRegion[] getAllFrameRegions() {
        return allFrames;
    }

    public float[] getAllFrameDurations() {
        return allDurationsSeconds;
    }

    public void dispose() {
        texture.dispose();
    }
}
