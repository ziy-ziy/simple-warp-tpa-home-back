package zy.swthb.handler;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Back 传送点管理器
 * 记录玩家最后一次死亡或传送前的位置，供 /back 命令使用。
 * <p>
 * 数据只保存在内存中，不持久化到磁盘。
 * 玩家下线后自动清理对应 back 点，避免内存泄漏。
 */
public class BackHandler {

    /** 位置类型 */
    public enum LocationType {
        DEATH,
        TELEPORT
    }

    /** 一个 Back 传送点 */
    public record BackPoint(
            LocationType type,
            String world,
            double x, double y, double z,
            float yaw, float pitch
    ) {}

    private static final Map<UUID, BackPoint> BACK_POINTS = new ConcurrentHashMap<>();
    private static boolean eventsRegistered = false;

    /**
     * 注册下线和服务器关闭事件，自动清理玩家的 back 点
     * <p>
     * 注意：不监听维度切换事件，因为 /back 本身支持跨维度返回
     */
    public static void registerEvents() {
        if (eventsRegistered) return;
        eventsRegistered = true;

        // 玩家下线后清理 back 点
        ServerPlayerEvents.LEAVE.register(player -> {
            BACK_POINTS.remove(player.getUUID());
        });

        // 服务器关闭时清理所有 back 点
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            BACK_POINTS.clear();
        });
    }

    /**
     * 保存当前玩家的位置作为 back 点
     */
    public static void saveBackPoint(ServerPlayer player, LocationType type) {
        BACK_POINTS.put(player.getUUID(), new BackPoint(
                type,
                player.level().dimension().identifier().toString(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot()
        ));
    }

    /**
     * 获取玩家的 back 点，使用后不清除（可反复传送）
     */
    public static BackPoint getBackPoint(ServerPlayer player) {
        return BACK_POINTS.get(player.getUUID());
    }

}
