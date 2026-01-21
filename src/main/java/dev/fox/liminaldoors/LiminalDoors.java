package dev.fox.liminaldoors;

import net.fabricmc.api.ModInitializer;

import java.util.Random;

public class LiminalDoors implements ModInitializer {
    public static final String MODID = "liminaldoors";

    public static final double TRIGGER_CHANCE = 0.01;

    public static final int SIZE_XZ = 256;
    public static final int HEIGHT = 6;

    public static final int BASE_X = 2_000_000;
    public static final int BASE_Z = 2_000_000;
    public static final int BASE_Y = 80;

    public static final Random RNG = new Random();

    @Override
    public void onInitialize() {
        // Все на миксинах. Иронично, но эффективно.
    }
}