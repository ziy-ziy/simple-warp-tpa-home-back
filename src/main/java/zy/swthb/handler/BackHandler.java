package zy.swthb.handler;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Back 传送点管理器
 * 记录玩家最后一次死亡或传送前的位置，供 /back 命令使用。
 * <p>
 * 数据只保存在内存中，不持久化到磁盘。
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
