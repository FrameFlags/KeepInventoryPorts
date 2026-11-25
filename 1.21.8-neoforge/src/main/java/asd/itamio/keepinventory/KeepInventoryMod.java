package asd.itamio.keepinventory;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Enforces keepInventory=true on every level, rechecking every 20 ticks. */
@Mod(KeepInventoryMod.MODID)
public final class KeepInventoryMod {
    public static final String MODID = "keepinventory";
    private int tickCounter;

    public KeepInventoryMod(IEventBus modEventBus) {
        // Negative-control gate: the test harness disables product behaviour with -Dkip.probe.only=true.
        if (Boolean.getBoolean("kip.probe.only")) return;
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        enforceAllLevels(event.getServer());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        enforceAllLevels(event.getServer());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) return;
        if (++tickCounter >= 20) {
            tickCounter = 0;
            enforceAllLevels(server);
        }
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
