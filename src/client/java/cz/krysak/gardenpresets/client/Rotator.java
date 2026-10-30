package cz.krysak.gardenpresets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class Rotator {
    private static boolean active = false;
    private static long startNanos;
    private static long durationNanos;
    private static float startYaw, startPitch, deltaYaw, targetPitch;
    private static float lastYaw, lastPitch;
    private static String presetName = "";

    private Rotator() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void start(LocalPlayer player, Preset preset, int durationMs) {
        startYaw = player.getYRot();
        startPitch = player.getXRot();
        deltaYaw = Mth.wrapDegrees(preset.yaw - startYaw); // shortest way around
        targetPitch = Preset.clampPitch(preset.pitch);
        durationNanos = durationMs * 1_000_000L;
        startNanos = System.nanoTime();
        lastYaw = startYaw;
        lastPitch = startPitch;
        presetName = preset.name;
        active = true;
    }

    public static void cancel(String message) {
        if (!active) return;
        active = false;
        message(message);
    }

    /** Time based, so it is safe to call every frame AND every tick. */
    public static void update(Minecraft client) {
        if (!active) return;
        LocalPlayer player = client.player;
        if (player == null) {
            active = false;
            return;
        }

        boolean movedByPlayer = Math.abs(Mth.wrapDegrees(player.getYRot() - lastYaw)) > 0.02f
                || Math.abs(player.getXRot() - lastPitch) > 0.02f;
        if (movedByPlayer) {
            active = false;
            message("Rotation cancelled (manual movement).");
            return;
        }

        double t = (System.nanoTime() - startNanos) / (double) durationNanos;
        boolean done = t >= 1.0;
        if (done) t = 1.0;
        float e = (float) (t * t * (3.0 - 2.0 * t)); // smoothstep

        player.setYRot(startYaw + deltaYaw * e);
        player.setXRot(startPitch + (targetPitch - startPitch) * e);
        player.setOldRot(); // no tick-interpolation lag -> exact per-frame view
        lastYaw = player.getYRot();
        lastPitch = player.getXRot();

        if (done) {
            active = false;
            message("Preset '" + presetName + "' reached.");
        }
    }

    private static void message(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendOverlayMessage(Component.literal(text));
        }
    }
}