package zy.swthb.command;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import zy.swthb.i18n.Msg;

/**
 * 维度显示名工具类 — 将维度标识符（如 minecraft:overworld）转为本地化的显示名称。
 * <p>
 * 供 HomeCommand 和 WarpCommand 共用，避免重复代码。
 */
public final class DimensionDisplay {

    private DimensionDisplay() {}

    public static Component of(String worldId) {
        return switch (worldId) {
            case "minecraft:overworld" ->
                    Msg.tr("swthb.dim.overworld", "主世界")
                            .withStyle(ChatFormatting.YELLOW);
            case "minecraft:the_nether" ->
                    Msg.tr("swthb.dim.nether", "下界")
                            .withStyle(ChatFormatting.YELLOW);
            case "minecraft:the_end" ->
                    Msg.tr("swthb.dim.end", "末地")
                            .withStyle(ChatFormatting.YELLOW);
            default -> {
                String[] parts = worldId.split(":");
                yield Component.literal(parts.length > 1 ? parts[1] : worldId)
                        .withStyle(ChatFormatting.YELLOW);
            }
        };
    }
}
