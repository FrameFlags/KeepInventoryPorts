package asd.itamio.keepinventory_test;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(DeathProbeForgeMod.MODID)
public final class DeathProbeForgeMod {
    public static final String MODID = "keepinventory_test";
    private static int ticks;

    public DeathProbeForgeMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level().getServer() != null) {
            DeathProbe.onJoin(player, ticks);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) return;
        ticks++;
        DeathProbe.onTick(event.getServer(), ticks);
    }
}
