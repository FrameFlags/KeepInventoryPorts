package asd.itamio.keepinventory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;

/** Enforces keepInventory=true on every level, rechecking every 20 ticks. */
public final class KeepInventoryMod implements ModInitializer {
    public static final String MODID = "keepinventory";
    private int tickCounter;

    @Override
    public void onInitialize() {
        // Negative-control gate: the test harness disables product behaviour with -Dkip.probe.only=true.
        if (Boolean.getBoolean("kip.probe.only")) return;
        ServerLifecycleEvents.SERVER_STARTING.register(this::enforceAllLevels);
        ServerLifecycleEvents.SERVER_STARTED.register(this::enforceAllLevels);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter >= 20) {
                tickCounter = 0;
                enforceAllLevels(server);
            }
        });
    }

    private void enforceAllLevels(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            GameRules rules = level.getGameRules();
            if (!rules.getBoolean(GameRules.RULE_KEEPINVENTORY)) {
                // Pass null, never the server: 1.21.8's GameRules setter stays silent this way.
                rules.getRule(GameRules.RULE_KEEPINVENTORY).set(true, null);
            }
        }
    }
}
