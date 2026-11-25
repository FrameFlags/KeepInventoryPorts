package asd.itamio.keepinventory_test;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Passive in-game probe: never sets keepInventory to true.
 *
 * <p>Verifies the rule at join, flips it to false, waits 21 ticks for the
 * product's 20-tick loop, gives a uniquely named paper, applies lethal damage,
 * confirms the vanilla death stat, respawns server-side and checks the item.
 */
public final class DeathProbe {
    private static final String MARKER = "KIP_PROBE_MARKER_7F3A";
    private static final String PREFIX = "KEEPINVENTORY_TEST_RESULT:";
    private static final String RULE = "keepInventory";

    private static final List<String> reasons = new ArrayList<String>();
    private static MinecraftServer server;
    private static UUID targetId;
    private static EntityPlayerMP tracked;
    private static int phase;
    private static int recheckTick;
    private static int deathTick;
    private static int deathsBefore;
    private static String before = "";
    private static boolean finished;
    private static boolean productPresent;
    private static boolean deathSeen;

    DeathProbe() {}

    static void serverStarting(MinecraftServer minecraftServer) {
        server = minecraftServer;
    }

    static void onJoin(EntityPlayer player) {
        if (finished || targetId != null || !(player instanceof EntityPlayerMP)) return;
        EntityPlayerMP mp = (EntityPlayerMP) player;
        MinecraftServer srv = mp.server;
        if (srv == null) {
            fail("no_server", "", "");
            return;
        }
        server = srv;
        targetId = mp.getUniqueID();
        tracked = mp;

        boolean initial = srv.getWorld(0).getGameRules().getBoolean(RULE);
        productPresent = initial;
        deathsBefore = mp.getStatFile().readStat(StatList.DEATHS);
        if (!initial) reasons.add("rule_false_at_join");
        // Creative damage immunity would silently skip the whole death path.
        mp.setGameType(GameType.SURVIVAL);

        ItemStack marker = new ItemStack(Items.PAPER);
        marker.setStackDisplayName(MARKER);
        mp.inventory.setInventorySlotContents(0, marker);
        mp.inventoryContainer.detectAndSendChanges();
        before = signature(mp);

        // Legacy 1.12.2 API: no broadcast sender argument.
        for (WorldServer world : srv.worlds) {
            if (world != null) world.getGameRules().setOrCreateGameRule(RULE, "false");
        }
        recheckTick = srv.getTickCounter() + 21;
        phase = 1;
        System.out.println("[KIP_PROBE] step=begin initialRule=" + initial + " before=" + before);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        onJoin(event.player);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || server == null) return;
        int tick = server.getTickCounter();

        if (targetId == null) {
            if (tick > 600) fail("no_player_join", "", "");
            return;
        }
        EntityPlayerMP current = server.getPlayerList().getPlayerByUUID(targetId);

        if (phase == 1 && tick >= recheckTick) {
            boolean restored = server.getWorld(0).getGameRules().getBoolean(RULE);
            if (!restored) reasons.add("rule_not_restored");
            if (!productPresent && restored) reasons.add("rule_true_at_death");

            EntityPlayerMP victim = current != null ? current : tracked;
            if (victim == null) {
                fail("no_victim", before, "");
                return;
            }
            victim.setPositionAndUpdate(victim.posX + 1000.0D, victim.posY + 300.0D, victim.posZ + 1000.0D);
            // Legacy 1.12.2 real death: attackEntityFrom -> damageEntity -> onDeath.
            victim.attackEntityFrom(DamageSource.OUT_OF_WORLD, Float.MAX_VALUE);
            if (victim.getHealth() > 0.0F) reasons.add("kill_failed");
            deathTick = tick;
            phase = 2;
            System.out.println("[KIP_PROBE] step=death restored=" + restored + " tick=" + deathTick);
            return;
        }

        if (phase == 2) {
            if (!deathSeen && tracked != null
                    && tracked.getStatFile().readStat(StatList.DEATHS) > deathsBefore) {
                deathSeen = true;
                deathTick = tick;
                System.out.println("[KIP_PROBE] step=death_confirmed tick=" + tick);
                // Legacy respawn: third arg=false (conqueredEnd), NOT keepInventory.
                // Forcing true here would copy the item even with the product absent.
                EntityPlayerMP respawned = server.getPlayerList().recreatePlayerEntity(tracked, tracked.dimension, false);
                if (respawned != null) tracked = respawned;
                phase = 3;
            } else if (!deathSeen && tick - deathTick > 200) {
                reasons.add("no_death");
                emit(signature(tracked), tick);
            }
            return;
        }

        if (phase == 3 && current != null && tick - deathTick > 2 && current.getHealth() > 0.0F) {
            String after = signature(current);
            if (!after.contains(MARKER)) reasons.add("item_lost_on_death");
            emit(after, tick);
        } else if (phase == 3 && tick - deathTick > 600) {
            fail("no_respawn", before, "");
        }
    }

    private static String signature(EntityPlayerMP player) {
        if (player == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int slot = 0; slot < player.inventory.getSizeInventory(); slot++) {
            ItemStack stack = player.inventory.getStackInSlot(slot);
            if (stack == null || stack.isEmpty()) continue;
            sb.append(stack.getDisplayName()).append('x').append(stack.getCount()).append(';');
        }
        return sb.toString();
    }

    private static void emit(String after, int respawnTick) {
        String verdict = reasons.isEmpty() ? "PASS" : "FAIL";
        String line = PREFIX + " " + verdict + " product=" + productPresent
                + " reasons=" + join(reasons)
                + " before=" + before + " after=" + after
                + " death=" + deathTick + " respawn=" + respawnTick;
        write(line);
        finished = true;
        System.out.println("[KIP_PROBE] " + line);
    }

    private static void fail(String reason, String beforeSig, String afterSig) {
        reasons.add(reason);
        if (!beforeSig.isEmpty()) before = beforeSig;
        String line = PREFIX + " FAIL product=false reasons=" + join(reasons)
                + " before=" + before + " after=" + afterSig
                + " death=" + deathTick + " respawn=-1";
        write(line);
        finished = true;
        System.out.println("[KIP_PROBE] " + line);
    }

    private static String join(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (sb.length() > 0) sb.append(',');
            sb.append(value);
        }
        return sb.toString();
    }

    private static void write(String line) {
        byte[] bytes = (line + "\n").getBytes(StandardCharsets.UTF_8);
        try {
            Path root = Paths.get("kip-result.txt");
            Files.write(root, bytes);
            Path run = Paths.get("run", "kip-result.txt");
            Path parent = run.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.write(run, bytes);
        } catch (IOException ignored) {
            // The log line is the fallback signal in CI.
        }
    }
}
