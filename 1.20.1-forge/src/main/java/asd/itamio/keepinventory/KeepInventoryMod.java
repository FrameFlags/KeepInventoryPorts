package asd.itamio.keepinventory;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Enforces keepInventory=true on every level, rechecking every 20 ticks. */
@Mod(KeepInventoryMod.MODID)
public final class KeepInventoryMod {
    public static final String MODID = "keepinventory";
    private int tickCounter;

    public KeepInventoryMod() {
        // Negative-control gate: the test harness disables product behaviour with -Dkip.probe.only=true.
        if (Boolean.getBoolean("kip.probe.only")) return;
        MinecraftForge.EVENT_BUS.register(this);
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
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++tickCounter >= 20) {
            tickCounter = 0;
            enforceAllLevels(event.getServer());
        }
    }

    private void enforceAllLevels(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            if (!level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
                level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, null);
            }
        }
    }
}
