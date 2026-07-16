package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

// Wählt den Frame über einen Fortschritts-Anteil (0..1) statt einer festen Echtzeit-frameDuration
// aus (siehe GDD 5.3) - dadurch synchronisiert sich die Animationslänge automatisch mit der
// jeweiligen Gameplay-Dauer (z.B. attackVisualDuration), ohne einen separaten
// "Animationsgeschwindigkeit"-Parameter einzuführen.
public class Animation {

    private final TextureRegion[] frames;

    public Animation(TextureRegion[] frames) {
        this.frames = frames;
    }

    public TextureRegion getFrame(float progress) {
        // Clamp ist Pflicht, nicht nur Vorsicht: bei progress==1.0 ergäbe (int)(progress *
        // frames.length) sonst genau frames.length - ein Index außerhalb des Arrays. Das trifft
        // schon die einfachste 1-Frame-Animation (Idle) sofort, kein hypothetischer Edge-Case.
        int index = MathUtils.clamp((int) (progress * frames.length), 0, frames.length - 1);
        return frames[index];
    }
}
