package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

public class Animation {

    private final TextureRegion[] frames;

    // Individuelle Frame-Dauern in Sekunden (z.B. aus Aseprite-Export), null wenn diese
    // Animation nur progress-basiert genutzt wird (siehe getFrame()). Bewahrt z.B. Ease-In/Out-
    // Timing eines Idle-Loops (kürzere erste/letzte Frames), statt es durch gleich lange
    // Frames plattzubügeln.
    private final float[] frameDurationsSeconds;
    private final float totalDurationSeconds;

    public Animation(TextureRegion[] frames) {
        this(frames, null);
    }

    public Animation(TextureRegion[] frames, float[] frameDurationsSeconds) {
        this.frames = frames;
        this.frameDurationsSeconds = frameDurationsSeconds;

        float total = 0f;
        if (frameDurationsSeconds != null) {
            for (float duration : frameDurationsSeconds) {
                total += duration;
            }
        }
        this.totalDurationSeconds = total;
    }

    // Fortschritts-basiert (0..1) - für an einen Gameplay-Timer gekoppelte Zustände (Attack/
    // Hurt/Death, siehe GDD 5.3). Nimmt bewusst gleich lange Frames an, weil die GESAMTLÄNGE
    // hier ohnehin vom Gameplay-Timer vorgegeben wird, nicht von individuellen Frame-Dauern.
    public TextureRegion getFrame(float progress) {
        // Clamp ist Pflicht, nicht nur Vorsicht: bei progress==1.0 ergäbe (int)(progress *
        // frames.length) sonst genau frames.length - ein Index außerhalb des Arrays.
        int index = MathUtils.clamp((int) (progress * frames.length), 0, frames.length - 1);
        return frames[index];
    }

    // Echtzeit-basierte Frame-Auswahl für reine Optik-Loops (Idle/Walk), mit individuellen
    // Frame-Dauern statt einheitlicher Fortschritts-Formel. "elapsedSeconds" ist eine frei
    // laufende, nie zurückgesetzte Uhr (siehe Player.animationTime).
    public TextureRegion getFrameByElapsedTime(float elapsedSeconds) {
        // Modulo, damit der Loop nach einer vollen Runde wieder von vorne beginnt
        float remaining = elapsedSeconds % totalDurationSeconds;

        for (int i = 0; i < frames.length; i++) {
            remaining -= frameDurationsSeconds[i];
            if (remaining < 0) {
                return frames[i];
            }
        }

        // Nur durch Fließkomma-Rundungsfehler erreichbar (remaining minimal > 0 statt exakt 0
        // nach dem letzten Abzug) - letzten Frame zurückgeben statt einer ArrayIndexOutOfBounds
        return frames[frames.length - 1];
    }
}
