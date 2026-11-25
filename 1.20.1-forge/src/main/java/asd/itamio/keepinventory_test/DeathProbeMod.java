package asd.itamio.keepinventory_test;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Registers the passive in-game probe on the Forge EVENT_BUS. */
@Mod(DeathProbeMod.MOD_ID)
public final class DeathProbeMod {
    public static final String MOD_ID = "keepinventory_test";

    private int tickCounter;

    public DeathProbeMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DeathProbe.onJoin(player, tickCounter);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        DeathProbe.onTick(event.getServer(), ++tickCounter);
    }
}
