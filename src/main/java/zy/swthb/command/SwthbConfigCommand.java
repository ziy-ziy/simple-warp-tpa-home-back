package zy.swthb.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import zy.swthb.config.ModConfig;
import zy.swthb.i18n.Msg;
import zy.swthb.i18n.Translations;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * /swthbconfig 命令 — 修改模组运行时配置
 * <p>
 * - 管理员权限项：最大家数量、最大 Warp 数量、传送倒计时、/back 开关、重载配置、保存配置
 * - 所有人可用：teleportEffects（传送粒子效果开关）
 */
public class SwthbConfigCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("swthbconfig")
                .then(Commands.literal("maxHomes")
                        .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100))
                                .executes(SwthbConfigCommand::executeSetMaxHomes)))
                .then(Commands.literal("maxWarps")
                        .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100))
                                .executes(SwthbConfigCommand::executeSetMaxWarps)))
                .then(Commands.literal("teleportDelay")
                        .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 120))
                                .executes(SwthbConfigCommand::executeSetTeleportDelay)))
                .then(Commands.literal("backEnabled")
                        .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                        .then(Commands.literal("true")
                                .executes(ctx -> executeSetBackEnabled(ctx, true)))
                        .then(Commands.literal("false")
                                .executes(ctx -> executeSetBackEnabled(ctx, false))))
                .then(Commands.literal("teleportEffects")
                        .then(Commands.literal("true")
                                .executes(ctx -> executeSetTeleportEffectsEnabled(ctx, true)))
                        .then(Commands.literal("false")
                                .executes(ctx -> executeSetTeleportEffectsEnabled(ctx, false))))
                .then(Commands.literal("language")
                        .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                        .then(Commands.argument("locale", StringArgumentType.word())
                                .suggests(SwthbConfigCommand::suggestLocales)
                                .executes(SwthbConfigCommand::executeSetLanguage)))
                .then(Commands.literal("reload")
                        .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                        .executes(SwthbConfigCommand::executeReload))
                .then(Commands.literal("save")
                        .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                        .executes(SwthbConfigCommand::executeSave))
        );
    }

    private static int executeSetMaxHomes(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        int amount = IntegerArgumentType.getInteger(ctx, "amount");

        ModConfig config = ModConfig.getInstance();
        int oldValue = config.getMaxHomes();
        config.setMaxHomes(amount);

        source.sendSuccess(() ->
                Msg.tr("swthb.config.max_homes_set",
                                "已将每名玩家最大家的数量从 %s 修改为 %s",
                                String.valueOf(oldValue), String.valueOf(amount))
                        .withStyle(ChatFormatting.GREEN),
                true
        );

        return 1;
    }

    private static int executeSetMaxWarps(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        int amount = IntegerArgumentType.getInteger(ctx, "amount");

        ModConfig config = ModConfig.getInstance();
        int oldValue = config.getMaxWarps();
        config.setMaxWarps(amount);

        source.sendSuccess(() ->
                Msg.tr("swthb.config.max_warps_set",
                                "已将 Warp 最大数量从 %s 修改为 %s",
                                String.valueOf(oldValue), String.valueOf(amount))
                        .withStyle(ChatFormatting.GREEN),
                true
        );

        return 1;
    }

    private static int executeSetTeleportDelay(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        int seconds = IntegerArgumentType.getInteger(ctx, "seconds");

        ModConfig config = ModConfig.getInstance();
        int oldValue = config.getTeleportDelay();
        config.setTeleportDelay(seconds);

        source.sendSuccess(() ->
                        Msg.tr("swthb.config.teleport_delay_set",
                                        "已将传送倒计时从 %s 秒修改为 %s 秒",
                                        String.valueOf(oldValue), String.valueOf(seconds))
                                .withStyle(ChatFormatting.GREEN),
                true
        );

        return 1;
    }

    private static int executeSetBackEnabled(CommandContext<CommandSourceStack> ctx, boolean enabled) {
        CommandSourceStack source = ctx.getSource();

        ModConfig config = ModConfig.getInstance();
        boolean oldValue = config.isBackEnabled();
        config.setBackEnabled(enabled);

        Supplier<Component> msg;
        if (enabled) {
            msg = () -> Msg.tr("swthb.config.back_enabled",
                            "已开启 /back 返回功能").withStyle(ChatFormatting.GREEN);
        } else {
            msg = () -> Msg.tr("swthb.config.back_disabled",
                            "已关闭 /back 返回功能").withStyle(ChatFormatting.RED);
        }
        source.sendSuccess(msg, true);

        return 1;
    }

    private static int executeSetTeleportEffectsEnabled(CommandContext<CommandSourceStack> ctx, boolean enabled) {
        CommandSourceStack source = ctx.getSource();

        ModConfig config = ModConfig.getInstance();
        config.setTeleportEffectsEnabled(enabled);

        Supplier<Component> msg;
        if (enabled) {
            msg = () -> Msg.tr("swthb.config.teleport_effects_enabled",
                            "已开启传送粒子效果").withStyle(ChatFormatting.GREEN);
        } else {
            msg = () -> Msg.tr("swthb.config.teleport_effects_disabled",
                            "已关闭传送粒子效果").withStyle(ChatFormatting.RED);
        }
        source.sendSuccess(msg, true);

        return 1;
    }

    private static int executeSetLanguage(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String locale = StringArgumentType.getString(ctx, "locale").toLowerCase();

        if (!Translations.isSupported(locale)) {
            source.sendFailure(Msg.tr("swthb.config.language_invalid",
                    "不支持的语言代码：%s（可用：auto、%s）",
                    locale, String.join("、", Translations.availableLocales()))
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        ModConfig config = ModConfig.getInstance();
        String oldValue = config.getLanguage();
        config.setLanguage(locale);

        source.sendSuccess(() ->
                Msg.tr("swthb.config.language_set",
                        "已将模组语言从 %s 修改为 %s", oldValue, locale)
                        .withStyle(ChatFormatting.GREEN),
                true
        );

        return 1;
    }

    private static CompletableFuture<Suggestions> suggestLocales(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        builder.suggest(Translations.AUTO);
        for (String locale : Translations.availableLocales()) {
            builder.suggest(locale);
        }
        return builder.buildFuture();
    }

    private static int executeReload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        ModConfig.load(source.getServer());

        source.sendSuccess(() ->
                Msg.tr("swthb.config.reloaded",
                        "配置文件已从磁盘重新加载").withStyle(ChatFormatting.GREEN),
                true
        );

        return 1;
    }

    private static int executeSave(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        ModConfig.getInstance().save();

        source.sendSuccess(() ->
                Msg.tr("swthb.config.saved",
                        "配置文件已保存到磁盘").withStyle(ChatFormatting.GREEN),
                true
        );

        return 1;
    }
}
