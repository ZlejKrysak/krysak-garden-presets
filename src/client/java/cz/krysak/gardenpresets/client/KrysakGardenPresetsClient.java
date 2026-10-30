package cz.krysak.gardenpresets.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class KrysakGardenPresetsClient implements ClientModInitializer {

	private static boolean openScreenRequested = false;

	public static void requestScreen() {
		openScreenRequested = true;
	}

	@Override
	public void onInitializeClient() {
		Settings.load();
		PresetStore.load();

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				KrysakCommands.register(dispatcher));

		// Runs every rendered frame -> perfectly smooth camera movement.
		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath("krysak-garden-presets", "rotation"),
				(graphics, deltaTracker) -> Rotator.update(Minecraft.getInstance()));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			Rotator.update(client); // fallback (e.g. when HUD is hidden with F1)
			if (openScreenRequested) {
				openScreenRequested = false;
				client.setScreen(new KrysakScreen());
			}
		});
	}
}