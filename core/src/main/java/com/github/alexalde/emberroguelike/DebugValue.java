package com.github.alexalde.emberroguelike;

import java.util.function.Consumer;
import java.util.function.Supplier;

// EIN generischer Wrapper statt einer Unterklasse pro Debug-Wert (gleiches Prinzip wie
// CurrencyPickup in Quest 9) - jeder Eintrag unterscheidet sich nur darin, WELCHER Wert
// gelesen/geschrieben wird, nicht im Verhalten. getter/setter greifen über Instanzfelder (z.B.
// Main.player) zu, nicht über eine zum Konstruktionszeitpunkt eingefangene Kopie - dadurch
// bleiben sie auch nach einer Player-Neuerstellung (Tod -> startGame()) gültig.
public class DebugValue {

    private final String label;
    private final Supplier<Float> getter;
    private final Consumer<Float> setter;
    private final float step;
    private final String format;
    private final float defaultValue;

    public DebugValue(String label, Supplier<Float> getter, Consumer<Float> setter, float step, String format) {
        this.label = label;
        this.getter = getter;
        this.setter = setter;
        this.step = step;
        this.format = format;
        this.defaultValue = getter.get();
    }

    public String getLabel() {
        return label;
    }

    public String getFormattedValue() {
        return String.format(format, getter.get());
    }

    public void increment() {
        setter.accept(getter.get() + step);
    }

    public void decrement() {
        setter.accept(getter.get() - step);
    }

    public void reset() {
        setter.accept(defaultValue);
    }
}
