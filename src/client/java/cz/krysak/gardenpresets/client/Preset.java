package cz.krysak.gardenpresets.client;

import java.text.Normalizer;
import java.util.Locale;

public class Preset {
    public String id;
    public String name;
    public Crop crop;
    public float yaw;
    public float pitch;

    public Preset() {
    }

    public Preset(String id, String name, Crop crop, float yaw, float pitch) {
        this.id = id;
        this.name = name;
        this.crop = crop;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    /** Name turned into a command-friendly key: "Melon Left" -> "melonleft". */
    public static String keyOf(String name) {
        String s = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return s.replaceAll("[^a-z0-9]", "");
    }

    public String key() {
        return keyOf(name);
    }

    public static float normalizeYaw(float yaw) {
        float y = ((yaw + 180f) % 360f + 360f) % 360f - 180f;
        return y;
    }

    public static float clampPitch(float pitch) {
        return Math.max(-90f, Math.min(90f, pitch));
    }
}