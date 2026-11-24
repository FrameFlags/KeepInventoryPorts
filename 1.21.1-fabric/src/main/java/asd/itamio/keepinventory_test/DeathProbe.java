package asd.itamio.keepinventory_test;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Passive in-game probe: never sets KEEP_INVENTORY to true. */
public final class DeathProbe {
    private DeathProbe() {}

    private static final String MARKER = "KIP_PROBE_MARKER_7F3A";
    private static final String PREFIX = "KEEPINVENTORY_TEST_RESULT:";
    private static final List<String> reasons = new ArrayList<>();
    private static UUID targetId;
    private static ServerPlayer trackedPlayer;
    private static int phase = 0;
    private static int startTick;
    private static int recheckTick;
    private static int deathTick;
    private static String before = "";
    private static boolean finished = false;
    private static boolean productPresent;
    private static boolean deathSeen;
    private static boolean keepAtDeath;
    private static int deathsBefore;

    public static void onJoin(ServerPlayer player, int currentTick) {
        if (targetId == null && player != null) {
            targetId = player.getUUID();
            trackedPlayer = player;
            startTick = currentTick;
            start(player, currentTick);
        }
    }

    private static void start(ServerPlayer player, int currentTick) {
        if (targetId != null || player == null) return;
        targetId = player.getUUID();
        trackedPlayer = player;
        startTick = currentTick;
        MinecraftServer server = player.level().getServer();
        if (server == null) { fail("no_server", "", ""); return; }
        boolean initial = server.overworld().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        productPresent = initial;
        deathsBefore = player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
        if (!initial) reasons.add("rule_false_at_join");
        ItemStack marker = new ItemStack(Items.PAPER);
        marker.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(MARKER));
        player.getInventory().setItem(0, marker);
        before = signature(player);
        server.overworld().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(false, server);
        recheckTick = currentTick + 21;
        phase = 1;
        System.out.println("[KIP_PROBE] step=begin initialRule=" + initial + " before=" + before);
    }

    public static void onTick(MinecraftServer server, int currentTick) {
        if (finished || targetId == null || server == null) {
            if (!finished && targetId == null && currentTick - startTick > 600 && currentTick > 600) {
                fail("no_player_join", "", "");
            }
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(targetId);
        if (player == null) {
            if (phase == 2 && !deathSeen && trackedPlayer != null) {
                deathSeen = true;
                deathTick = currentTick;
                server.getPlayerList().respawn(trackedPlayer, keepAtDeath, net.minecraft.world.entity.Entity.RemovalReason.KILLED);
                System.out.println("[KIP_PROBE] step=respawn tick=" + currentTick);
            } else if (phase == 2 && deathSeen && currentTick - deathTick > 600) {
                fail("no_respawn", before, "");
            }
            return;
        }
        if (phase == 0) {
            if (player != null && player.tickCount >= 100) {
                start(player, currentTick);
            } else if (currentTick > 900) {
                fail("no_player_ready", "", "");
            }
            return;
        }
        if (phase == 1 && currentTick >= recheckTick) {
            boolean restored = server.overworld().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
            if (!restored) reasons.add("rule_not_restored");
            keepAtDeath = restored;
            double x = player.getX(), y = player.getY(), z = player.getZ();
            player.teleportTo((ServerLevel) player.level(), x + 1000, y + 300, z + 1000, Set.of(), player.getYRot(), player.getXRot());
            player.die(player.level().damageSources().generic());
            deathTick = currentTick;
            phase = 2;
            System.out.println("[KIP_PROBE] step=death restored=" + restored + " tick=" + deathTick);
        } else if (phase == 2 && !deathSeen
                && player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS)) > deathsBefore) {
            deathSeen = true;
            deathTick = currentTick;
            if (player == trackedPlayer) {
                server.getPlayerList().respawn(trackedPlayer, keepAtDeath, net.minecraft.world.entity.Entity.RemovalReason.KILLED);
            }
            System.out.println("[KIP_PROBE] step=death_confirmed tick=" + currentTick);
        } else if (phase == 2 && deathSeen && player != trackedPlayer && currentTick - deathTick > 5
                && !player.isDeadOrDying() && player.getHealth() > 0f) {
            String after = signature(player);
            if (!after.contains(MARKER)) reasons.add("item_lost_on_death");
            emit(after, currentTick);
        } else if (phase == 2 && !deathSeen && currentTick - deathTick > 200) {
            reasons.add("no_death");
            emit(signature(player), currentTick);
        } else if (phase == 2 && deathSeen && currentTick - deathTick > 600) {
            fail("no_respawn", before, signature(player));
        }
    }

    private static String signature(ServerPlayer player) {
        StringBuilder sb = new StringBuilder();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack == null || stack.isEmpty()) continue;
            String name = stack.getHoverName().getString();
            sb.append(name).append('x').append(stack.getCount()).append(';');
        }
        return sb.toString();
    }

    private static void emit(String after, int respawnTick) {
        String verdict = reasons.isEmpty() ? "PASS" : "FAIL";
        String line = PREFIX + " " + verdict + " product=" + productPresent
                + " reasons=" + String.join(",", reasons)
                + " before=" + before + " after=" + after
                + " death=" + deathTick + " respawn=" + respawnTick;
        write(line);
        finished = true;
        System.out.println("[KIP_PROBE] " + line);
    }

    private static void fail(String reason, String beforeSig, String afterSig) {
        reasons.add(reason);
        before = beforeSig.isEmpty() ? before : beforeSig;
        String line = PREFIX + " FAIL product=false reasons=" + String.join(",", reasons)
                + " before=" + before + " after=" + afterSig + " death=" + deathTick + " respawn=-1";
        write(line);
        finished = true;
        System.out.println("[KIP_PROBE] " + line);
    }

    private static void write(String line) {
        try {
            Files.writeString(Path.of("kip-result.txt"), line + "\n");
            Files.writeString(Path.of("run/kip-result.txt"), line + "\n");
        } catch (Exception ignored) {
            // Log line is the fallback signal in CI.
        }
    }
}
