package zy.swthb.handler;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import zy.swthb.config.ModConfig;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 传送倒计时管理器
 * 为 /tpay、/home、/warp 提供可配置的延迟传送，默认 0 秒（无延迟）。
 * <p>
 * - 倒计时期间玩家移动则取消传送
 * - 倒计时显示在动作栏
 * - 可在 data.json 中通过 teleportDelay 修改秒数
 */
public class TeleportHandler {

    private static final Map<UUID, TeleportTask> PENDING = new ConcurrentHashMap<>();

    private record TeleportTask(
            ServerPlayer player,
            Vec3 startPos,
            int totalTicks,
            int ticksRemaining,
            TeleportTransition transition,
            Runnable onSuccess,
            Runnable onCancel
    ) {
        boolean hasMoved() {
            return player.position().distanceToSqr(startPos) > 0.0625; // 0.25 blocks ^2
        }
    }

    /**
     * 开始一个延迟传送
     *
     * @param player     待传送的玩家
     * @param transition 传送参数
     * @param onSuccess  传送成功后的回调（如发送提示消息）
     * @param onCancel   传送被取消时的回调
     */
    public static void startTeleport(ServerPlayer player,
                                     TeleportTransition transition,
                                     Runnable onSuccess,
                                     Runnable onCancel) {
        // 取消该玩家已有的倒计时
        cancelTeleport(player, false);

        int delay = ModConfig.getInstance().getTeleportDelay();
        if (delay <= 0) {
            // 延迟为 0，直接传送
            player.teleport(transition);
            if (onSuccess != null) onSuccess.run();
            return;
        }

        int totalTicks = delay * 20;
        PENDING.put(player.getUUID(), new TeleportTask(
                player, player.position(), totalTicks, totalTicks,
                transition, onSuccess, onCancel
        ));

        // 发送初始倒计时提示（动作栏）
        player.sendOverlayMessage(
                Component.translatableWithFallback("swthb.teleport.countdown",
                        "传送倒计时：%s 秒...", String.valueOf(delay))
                        .withStyle(ChatFormatting.YELLOW)
        );
    }

    /**
     * 取消玩家的传送倒计时
     *
     * @param player    目标玩家
     * @param notify    是否发送取消提示
     */
    public static void cancelTeleport(ServerPlayer player, boolean notify) {
        TeleportTask task = PENDING.remove(player.getUUID());
        if (task != null && notify) {
            if (task.onCancel() != null) task.onCancel().run();
        }
    }

    /**
     * 每个服务器 tick 调用一次，处理倒计时逻辑
     */
    public static void tick() {
        if (PENDING.isEmpty()) return;

        Iterator<Map.Entry<UUID, TeleportTask>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            TeleportTask task = it.next().getValue();

            // 玩家断开连接 → 移除任务
            if (task.player().hasDisconnected()) {
                it.remove();
                continue;
            }

            // 玩家移动 → 取消传送
            if (task.hasMoved()) {
                it.remove();
                task.player().sendOverlayMessage(Component.literal("")); // 清空动作栏
                task.player().sendSystemMessage(
                        Component.translatableWithFallback("swthb.teleport.cancelled_move",
                                "§c传送已取消，因为你移动了")
                );
                if (task.onCancel() != null) task.onCancel().run();
                continue;
            }

            int remaining = task.ticksRemaining() - 1;

            if (remaining <= 0) {
                // 倒计时结束，执行传送
                it.remove();
                task.player().teleport(task.transition());
                task.player().sendOverlayMessage(Component.literal("")); // 清空动作栏
                if (task.onSuccess() != null) task.onSuccess().run();
            } else {
                // 每秒更新一次动作栏倒计时
                if (remaining % 20 == 0) {
                    int seconds = remaining / 20;
                    task.player().sendOverlayMessage(
                            Component.translatableWithFallback("swthb.teleport.countdown",
                                    "传送倒计时：%s 秒...", String.valueOf(seconds))
                                    .withStyle(ChatFormatting.YELLOW)
                    );
                }
                // 更新剩余 tick
                PENDING.put(task.player().getUUID(), new TeleportTask(
                        task.player(), task.startPos(), task.totalTicks(),
                        remaining, task.transition(), task.onSuccess(), task.onCancel()
                ));
            }
        }
    }
}
