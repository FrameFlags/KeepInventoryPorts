package asd.itamio.keepinventory;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Enforces keepInventory=true on every level, rechecking every 20 ticks. */
@Mod(KeepInventoryMod.MODID)
public final class KeepInventoryMod {
    public static final String MODID = "keepinventory";
    private int tickCounter;

    public KeepInventoryMod(IEventBus modEventBus) {
        if (Boolean.getBoolean("kip.probe.only")) return;
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
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
            if (!Boolean.TRUE.equals(level.getGameRules().get(GameRules.KEEP_INVENTORY))) {
                level.getGameRules().set(GameRules.KEEP_INVENTORY, true, server);
            }
        }
    }
}
