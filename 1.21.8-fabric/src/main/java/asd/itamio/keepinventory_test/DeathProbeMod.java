package asd.itamio.keepinventory_test;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class DeathProbeMod implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                DeathProbe.onJoin(handler.getPlayer(), server.getTickCount()));
        ServerTickEvents.END_SERVER_TICK.register(server -> DeathProbe.onTick(server, server.getTickCount()));
    }
}
