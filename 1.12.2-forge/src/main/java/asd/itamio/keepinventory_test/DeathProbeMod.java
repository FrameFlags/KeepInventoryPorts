package asd.itamio.keepinventory_test;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

/** Test-only server-side probe. Never shipped in the product jar. */
@Mod(modid = DeathProbeMod.MODID, name = DeathProbeMod.NAME, version = DeathProbeMod.VERSION,
        serverSideOnly = true, acceptableRemoteVersions = "*")
public final class DeathProbeMod {
    public static final String MODID = "keepinventory_test";
    public static final String NAME = "Keep Inventory death probe (test only)";
    public static final String VERSION = "1.0.0";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new DeathProbe());
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        DeathProbe.serverStarting(event.getServer());
    }
}
