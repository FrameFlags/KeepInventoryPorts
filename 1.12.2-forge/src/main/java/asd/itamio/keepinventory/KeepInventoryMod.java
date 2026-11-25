package asd.itamio.keepinventory;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Enforces keepInventory=true on every world, re-checking every 20 ticks.
 *
 * <p>Uses the legacy 1.12.2 GameRules API only: setOrCreateGameRule is silent and
 * never broadcasts a chat message. There is no client code, config or item logic.
 */
@Mod(modid = KeepInventoryMod.MODID, name = KeepInventoryMod.NAME, version = KeepInventoryMod.VERSION,
        acceptableRemoteVersions = "*")
public final class KeepInventoryMod {
    public static final String MODID = "keepinventory";
    public static final String NAME = "Keep Inventory — Itamio Port";
    public static final String VERSION = "1.0.0";

    private static final String RULE = "keepInventory";
    private static final int INTERVAL = 20;

    private MinecraftServer server;
    private int tickCounter;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (disabled()) return;
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        if (disabled()) return;
        server = event.getServer();
        enforceAll(server);
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (disabled()) return;
        World world = event.getWorld();
        if (world != null && !world.isRemote) enforce(world);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (disabled() || event.phase != TickEvent.Phase.END) return;
        if (++tickCounter < INTERVAL) return;
        tickCounter = 0;
        enforceAll(server);
    }

    private static void enforceAll(MinecraftServer minecraftServer) {
        if (minecraftServer == null || minecraftServer.worlds == null) return;
        for (WorldServer world : minecraftServer.worlds) {
            if (world != null) enforce(world);
        }
    }

    private static void enforce(World world) {
        // Legacy 1.12.2 API: no sender parameter, hence no chat broadcast.
        world.getGameRules().setOrCreateGameRule(RULE, "true");
    }

    /** Defensive gate used by tests to disable the product without removing the jar. */
    private static boolean disabled() {
        return Boolean.getBoolean("kip.probe.only");
    }
}
