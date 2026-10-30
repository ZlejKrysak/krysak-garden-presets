package cz.krysak.gardenpresets.client;

public enum Crop {
    MELON("Melon"),
    PUMPKIN("Pumpkin"),
    CARROT("Carrot"),
    POTATO("Potato"),
    COCOA_BEANS("Cocoa Beans"),
    WHEAT("Wheat"),
    SUGAR_CANE("Sugar Cane"),
    NETHER_WART("Nether Wart"),
    MUSHROOMS("Mushrooms"),
    OTHER("Other / Custom");

    public final String displayName;

    Crop(String displayName) {
        this.displayName = displayName;
    }

    /** Accepts e.g. "melon", "Cocoa", "cocoa_beans", "sugarcane", "cane", "custom". Returns null if unknown. */
    public static Crop fromInput(String input) {
        String key = input.toLowerCase().replaceAll("[^a-z]", "");
        switch (key) {
            case "cocoa": return COCOA_BEANS;
            case "cane": return SUGAR_CANE;
            case "wart": return NETHER_WART;
            case "mushroom": return MUSHROOMS;
            case "custom": return OTHER;
            default: break;
        }
        for (Crop crop : values()) {
            if (crop.name().replace("_", "").toLowerCase().equals(key)) {
                return crop;
            }
        }
        return null;
    }
}