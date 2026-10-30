package cz.krysak.gardenpresets.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Settings {
    private static final Logger LOGGER = LoggerFactory.getLogger("krysak-garden-presets");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE =
            FabricLoader.getInstance().getConfigDir().resolve("krysak-garden-presets-settings.json");

    public static final int MIN_DURATION_MS = 100;
    public static final int MAX_DURATION_MS = 5000;

    public static int durationMs = 600;

    private static class Data {
        int durationMs = 600;
    }

    private Settings() {
    }

    public static void load() {
        if (!Files.exists(FILE)) return;
        try {
            Data data = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), Data.class);
            if (data == null) return;
            durationMs = Math.max(MIN_DURATION_MS, Math.min(MAX_DURATION_MS, data.durationMs));
        } catch (Exception e) {
            LOGGER.error("Could not read settings, using defaults", e);
        }
    }

    public static void save() {
        Data data = new Data();
        data.durationMs = durationMs;
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.error("Could not save settings", e);
        }
    }
}