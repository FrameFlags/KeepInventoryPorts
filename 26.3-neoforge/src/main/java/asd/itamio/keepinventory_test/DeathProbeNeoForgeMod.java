package asd.itamio.keepinventory_test;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(DeathProbeNeoForgeMod.MODID)
public final class DeathProbeNeoForgeMod {
    public static final String MODID = "keepinventory_test";
    private static int ticks;

    public DeathProbeNeoForgeMod(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (((net.minecraft.server.level.ServerPlayer) event.getEntity()).level().getServer() != null) {
            DeathProbe.onJoin((net.minecraft.server.level.ServerPlayer) event.getEntity(), ticks);
        }
    }

    @SubscribeEvent
    public void onTick(ServerTickEvent.Post event) {
        ticks++;
        DeathProbe.onTick(event.getServer(), ticks);
    }
}
