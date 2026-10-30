package cz.krysak.gardenpresets.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PresetStore {
    private static final Logger LOGGER = LoggerFactory.getLogger("krysak-garden-presets");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE =
            FabricLoader.getInstance().getConfigDir().resolve("krysak-garden-presets.json");

    private static final List<Preset> PRESETS = new ArrayList<>();

    private static class Data {
        int version = 1;
        List<Preset> presets = new ArrayList<>();
    }

    private PresetStore() {
    }

    public static List<Preset> all() {
        return List.copyOf(PRESETS);
    }

    public static Preset findByKey(String name) {
        String key = Preset.keyOf(name);
        for (Preset p : PRESETS) {
            if (p.key().equals(key)) return p;
        }
        return null;
    }

    /** Adds or (if overwrite) replaces a preset with the same key. Returns false if it exists and overwrite is false. */
    public static boolean put(String name, Crop crop, float yaw, float pitch, boolean overwrite) {
        Preset existing = findByKey(name);
        if (existing != null && !overwrite) return false;
        yaw = Preset.normalizeYaw(yaw);
        pitch = Preset.clampPitch(pitch);
        if (existing != null) {
            existing.name = name;
            existing.crop = crop;
            existing.yaw = yaw;
            existing.pitch = pitch;
        } else {
            PRESETS.add(new Preset(UUID.randomUUID().toString(), name, crop, yaw, pitch));
        }
        save();
        return true;
    }

    public static boolean delete(String name) {
        Preset existing = findByKey(name);
        if (existing == null) return false;
        PRESETS.remove(existing);
        save();
        return true;
    }

    public static Preset conflict(String name, String exceptId) {
        String key = Preset.keyOf(name);
        for (Preset p : PRESETS) {
            if (p.key().equals(key) && !p.id.equals(exceptId)) return p;
        }
        return null;
    }

    /** editId == null -> new preset. Returns false if the name is taken and overwriteConflict is false. */
    public static boolean upsert(String editId, String name, Crop crop, float yaw, float pitch, boolean overwriteConflict) {
        Preset other = conflict(name, editId);
        if (other != null) {
            if (!overwriteConflict) return false;
            PRESETS.remove(other);
        }
        yaw = Preset.normalizeYaw(yaw);
        pitch = Preset.clampPitch(pitch);
        Preset target = null;
        if (editId != null) {
            for (Preset p : PRESETS) if (p.id.equals(editId)) target = p;
        }
        if (target == null) {
            PRESETS.add(new Preset(UUID.randomUUID().toString(), name, crop, yaw, pitch));
        } else {
            target.name = name;
            target.crop = crop;
            target.yaw = yaw;
            target.pitch = pitch;
        }
        save();
        return true;
    }

    public static boolean deleteById(String id) {
        boolean removed = PRESETS.removeIf(p -> p.id.equals(id));
        if (removed) save();
        return removed;
    }

    public static void load() {
        PRESETS.clear();
        if (!Files.exists(FILE)) return;
        try {
            String json = Files.readString(FILE, StandardCharsets.UTF_8);
            Data data = GSON.fromJson(json, Data.class);
            if (data == null || data.presets == null) throw new IllegalStateException("empty or invalid file");
            for (Preset p : data.presets) {
                if (p == null || p.name == null || p.name.isBlank()) {
                    LOGGER.warn("Skipping invalid preset entry");
                    continue;
                }
                if (p.id == null) p.id = UUID.randomUUID().toString();
                if (p.crop == null) p.crop = Crop.OTHER;
                p.yaw = Preset.normalizeYaw(p.yaw);
                p.pitch = Preset.clampPitch(p.pitch);
                PRESETS.add(p);
            }
        } catch (Exception e) {
            // Never overwrite a damaged file: keep a copy next to it and start empty.
            LOGGER.error("Preset file is damaged, backing it up", e);
            try {
                Path backup = FILE.resolveSibling(FILE.getFileName() + ".corrupt-" + System.currentTimeMillis());
                Files.move(FILE, backup, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.warn("Damaged file moved to {}", backup);
            } catch (IOException io) {
                LOGGER.error("Could not back up damaged file", io);
            }
            PRESETS.clear();
        }
    }

    private static void save() {
        Data data = new Data();
        data.presets = new ArrayList<>(PRESETS);
        Path tmp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(tmp, GSON.toJson(data), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOGGER.error("Could not save presets", e);
        }
    }
}