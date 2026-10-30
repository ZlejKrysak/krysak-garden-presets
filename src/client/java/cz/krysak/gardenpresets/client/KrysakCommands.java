package cz.krysak.gardenpresets.client;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class KrysakCommands {
    private KrysakCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("presets").executes(ctx -> {
            KrysakGardenPresetsClient.requestScreen();
            return 1;
        }));
    }
}