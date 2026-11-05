package zy.swthb.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import zy.swthb.command.BackHandler;
import zy.swthb.command.TeleportHandler;
import zy.swthb.config.ModConfig;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TPA 传送请求功能
 * 玩家可向其他玩家发送传送请求，对方可在聊天栏点击 [接受] 或 [拒绝]。
 */
public class TpaCommand {

    // 所有待处理的传送请求：key = 目标玩家 UUID, value = 请求信息
    private static final Map<UUID, PendingRequest> PENDING = new ConcurrentHashMap<>();

    // 请求超时时间：60 秒
    private static final long TIMEOUT_MS = 60_000L;

    /**
     * 一条待处理的传送请求记录
     *
     * @param requesterId 发起请求的玩家 UUID
     * @param timestamp   请求发起时间（毫秒时间戳）
     */
    private record PendingRequest(UUID requesterId, long timestamp, boolean requesterGoesToTarget) {}

    /**
     * 注册 TPA 过期清理事件（每秒执行一次）
     */
    public static void registerCleanupEvent() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 == 0) {
                cleanup();
            }
        });
    }

    /**
     * 服务器停止时清空所有待处理请求，避免跨存档残留。
     */
    public static void clear() {
        PENDING.clear();
    }

    /**
     * 注册 /tpa、/tpay、/tpan、/tpaauto、/tphere 命令
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /tpa <玩家> — 向目标玩家发送传送请求
        dispatcher.register(Commands.literal("tpa")
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(TpaCommand::executeTpa)));

        // /tpay — 接受最近一条传送请求
        dispatcher.register(Commands.literal("tpay")
                .executes(TpaCommand::executeTpay));

        // /tpan — 拒绝传送请求
        dispatcher.register(Commands.literal("tpan")
                .executes(TpaCommand::executeTpan));

        // /tpaauto [accept|deny|off] — 设置自动接受/自动拒绝/关闭（不带参数查看当前设置）
        // /tpaauto whitelist <add|remove|list> — OP 管理自动接受白名单
        dispatcher.register(Commands.literal("tpaauto")
                .executes(TpaCommand::executeTpaAutoStatus)
                .then(Commands.literal("accept")
                        .executes(ctx -> executeSetTpaAuto(ctx, ModConfig.TpaMode.ACCEPT)))
                .then(Commands.literal("deny")
                        .executes(ctx -> executeSetTpaAuto(ctx, ModConfig.TpaMode.DENY)))
                .then(Commands.literal("off")
                        .executes(ctx -> executeSetTpaAuto(ctx, ModConfig.TpaMode.MANUAL)))
                .then(Commands.literal("whitelist")
                        .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                        .then(Commands.literal("add")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(TpaCommand::executeWhitelistAdd)))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(TpaCommand::executeWhitelistRemove)))
                        .then(Commands.literal("list")
                                .executes(TpaCommand::executeWhitelistList))));

        // /tphere <玩家> — 请求目标玩家传送到自己身边
        dispatcher.register(Commands.literal("tphere")
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(TpaCommand::executeTphere)));
    }

    /**
     * 执行 /tpa <target>
     */
    private static int executeTpa(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        ServerPlayer target;
        try {
            target = EntityArgument.getPlayer(ctx, "target");
        } catch (Exception e) {
            source.sendFailure(Component.translatableWithFallback("swthb.tpa.player_not_found", "玩家未找到"));
            return 0;
        }

        if (player.getUUID().equals(target.getUUID())) {
            source.sendFailure(Component.translatableWithFallback("swthb.tpa.self", "不能向自己发送传送请求"));
            return 0;
        }

        PendingRequest request = new PendingRequest(player.getUUID(), System.currentTimeMillis(), true);

        // OP 白名单中的玩家（通常是假人）无法点击接受，直接自动接受
        if (ModConfig.getInstance().isTpaWhitelisted(target.getGameProfile().name())) {
            if (!acceptRequest(target, request)) {
                source.sendFailure(Component.translatableWithFallback("swthb.tpa.requester_offline", "请求者已离线"));
                return 0;
            }
            return 1;
        }

        // 目标玩家设置了自动处理：立即接受/拒绝，不生成待处理请求
        ModConfig.TpaMode mode = ModConfig.getInstance().getTpaMode(target.getUUID());
        if (mode == ModConfig.TpaMode.ACCEPT) {
            if (!acceptRequest(target, request)) {
                source.sendFailure(Component.translatableWithFallback("swthb.tpa.requester_offline", "请求者已离线"));
                return 0;
            }
            return 1;
        }
        if (mode == ModConfig.TpaMode.DENY) {
            denyRequest(target, request);
            return 1;
        }

        PENDING.put(target.getUUID(), request);

        // 通知发起方
        Component senderName = target.getDisplayName().copy().withStyle(ChatFormatting.AQUA);
        player.sendSystemMessage(
                Component.translatableWithFallback("swthb.tpa.sender_msg",
                        "已向 %s 发送传送请求，等待回应...", senderName)
                        .withStyle(ChatFormatting.GREEN)
        );

        // 通知目标方，附带可点击的接受/拒绝按钮
        Component requesterName = player.getDisplayName().copy().withStyle(ChatFormatting.AQUA);

        target.sendSystemMessage(
                Component.translatableWithFallback("swthb.tpa.receiver_msg",
                        "%s 请求传送到你的位置 %s %s",
                        requesterName, acceptButton(), denyButton())
        );

        return 1;
    }

    /**
     * 执行 /tpay
     */
    private static int executeTpay(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        PendingRequest req = takeValidRequest(player);
        if (req == null) {
            source.sendFailure(Component.translatableWithFallback("swthb.tpa.no_request", "没有待处理的传送请求"));
            return 0;
        }

        if (!acceptRequest(player, req)) {
            source.sendFailure(Component.translatableWithFallback("swthb.tpa.requester_offline", "请求者已离线"));
            return 0;
        }

        return 1;
    }

    /**
     * 执行 /tpan
     */
    private static int executeTpan(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        PendingRequest req = takeValidRequest(player);
        if (req == null) {
            source.sendFailure(Component.translatableWithFallback("swthb.tpa.no_request", "没有待处理的传送请求"));
            return 0;
        }

        denyRequest(player, req);

        return 1;
    }

    /**
     * 执行 /tphere <target>：向目标玩家发送“传送到你身边”的请求，对方可点击接受/拒绝。
     */
    private static int executeTphere(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        ServerPlayer target;
        try {
            target = EntityArgument.getPlayer(ctx, "target");
        } catch (Exception e) {
            source.sendFailure(Component.translatableWithFallback("swthb.tpa.player_not_found", "玩家未找到"));
            return 0;
        }

        if (player.getUUID().equals(target.getUUID())) {
            source.sendFailure(Component.translatableWithFallback("swthb.tphere.self", "不能对自己使用该命令"));
            return 0;
        }

        PendingRequest request = new PendingRequest(player.getUUID(), System.currentTimeMillis(), false);

        // OP 白名单中的玩家（通常是假人）无法点击接受，直接自动接受
        if (ModConfig.getInstance().isTpaWhitelisted(target.getGameProfile().name())) {
            if (!acceptRequest(target, request)) {
                source.sendFailure(Component.translatableWithFallback("swthb.tpa.requester_offline", "请求者已离线"));
                return 0;
            }
            return 1;
        }

        // /tphere 不受目标的自动接受/自动拒绝设置影响，始终需要手动确认
        PENDING.put(target.getUUID(), request);

        // 通知发起方
        Component targetName = target.getDisplayName().copy().withStyle(ChatFormatting.AQUA);
        player.sendSystemMessage(
                Component.translatableWithFallback("swthb.tphere.sent",
                        "已请求 %s 传送到你身边，等待回应...", targetName)
                        .withStyle(ChatFormatting.GREEN)
        );

        // 通知目标方，附带可点击的接受/拒绝按钮
        Component requesterName = player.getDisplayName().copy().withStyle(ChatFormatting.AQUA);
        target.sendSystemMessage(
                Component.translatableWithFallback("swthb.tphere.receiver_msg",
                        "%s 请求你传送到他身边 %s %s",
                        requesterName, acceptButton(), denyButton())
        );

        return 1;
    }

    /**
     * 执行 /tpaauto（不带参数）：查看当前自动处理设置。
     */
    private static int executeTpaAutoStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        sendTpaModeMessage(player, ModConfig.getInstance().getTpaMode(player.getUUID()));
        return 1;
    }

    /**
     * 执行 /tpaauto <accept|deny|off>：修改自动处理设置。
     */
    private static int executeSetTpaAuto(CommandContext<CommandSourceStack> ctx, ModConfig.TpaMode mode) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        ModConfig.getInstance().setTpaMode(player.getUUID(), mode);
        sendTpaModeMessage(player, mode);
        return 1;
    }

    /**
     * 向玩家反馈当前的自动处理模式。
     */
    private static void sendTpaModeMessage(ServerPlayer player, ModConfig.TpaMode mode) {
        switch (mode) {
            case ACCEPT -> player.sendSystemMessage(
                    Component.translatableWithFallback("swthb.tpa.auto_accept",
                            "已开启自动接受传送请求")
                            .withStyle(ChatFormatting.GREEN));
            case DENY -> player.sendSystemMessage(
                    Component.translatableWithFallback("swthb.tpa.auto_deny",
                            "已开启自动拒绝传送请求")
                            .withStyle(ChatFormatting.RED));
            case MANUAL -> player.sendSystemMessage(
                    Component.translatableWithFallback("swthb.tpa.auto_off",
                            "已关闭自动处理，传送请求需要手动确认")
                            .withStyle(ChatFormatting.YELLOW));
        }
    }

    /**
     * 执行 /tpaauto whitelist add <name>（仅 OP）。
     */
    private static int executeWhitelistAdd(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");

        ModConfig.getInstance().addTpaWhitelist(name);

        source.sendSuccess(() ->
                Component.translatableWithFallback("swthb.tpa.whitelist.added",
                        "已将 %s 加入 TPA 白名单", name)
                        .withStyle(ChatFormatting.GREEN),
                true
        );
        return 1;
    }

    /**
     * 执行 /tpaauto whitelist remove <name>（仅 OP）。
     */
    private static int executeWhitelistRemove(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");

        if (ModConfig.getInstance().removeTpaWhitelist(name)) {
            source.sendSuccess(() ->
                    Component.translatableWithFallback("swthb.tpa.whitelist.removed",
                            "已将 %s 从 TPA 白名单移除", name)
                            .withStyle(ChatFormatting.GREEN),
                    true
            );
            return 1;
        }

        source.sendFailure(Component.translatableWithFallback("swthb.tpa.whitelist.not_found",
                "TPA 白名单中不存在 %s", name));
        return 0;
    }

    /**
     * 执行 /tpaauto whitelist list（仅 OP）。
     */
    private static int executeWhitelistList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        List<String> names = ModConfig.getInstance().getTpaWhitelist();

        if (names.isEmpty()) {
            source.sendSuccess(() ->
                    Component.translatableWithFallback("swthb.tpa.whitelist.empty",
                            "TPA 白名单为空")
                            .withStyle(ChatFormatting.GRAY),
                    false
            );
            return 1;
        }

        source.sendSuccess(() ->
                Component.translatableWithFallback("swthb.tpa.whitelist.list_title",
                        "TPA 白名单（%s）：", String.valueOf(names.size()))
                        .withStyle(ChatFormatting.GOLD),
                false
        );
        source.sendSuccess(() -> Component.literal(String.join(", ", names))
                .withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    /**
     * 执行接受流程：通知双方、记录 /back 点并开始（延迟）传送。
     * <p>
     * /tpa 是发起方传送到接受方；/tphere 是接受方传送到发起方。
     *
     * @return 请求者是否仍在线
     */
    private static boolean acceptRequest(ServerPlayer target, PendingRequest req) {
        ServerPlayer requester = target.level().getServer().getPlayerList().getPlayer(req.requesterId());
        if (requester == null) return false;

        // 通知接受方
        Component requesterName = requester.getDisplayName().copy().withStyle(ChatFormatting.AQUA);
        target.sendSystemMessage(
                Component.translatableWithFallback("swthb.tpa.accepted_receiver",
                        "已接受 %s 的传送请求", requesterName)
                        .withStyle(ChatFormatting.GREEN)
        );

        // 决定谁传送到谁：/tpa 由发起方过去，/tphere 由接受方过去
        ServerPlayer traveler = req.requesterGoesToTarget() ? requester : target;
        ServerPlayer destination = req.requesterGoesToTarget() ? target : requester;

        // 记录传送前位置供 /back 使用
        BackHandler.saveBackPoint(traveler, BackHandler.LocationType.TELEPORT);

        // 延迟传送
        TeleportTransition transition = new TeleportTransition(
                destination.level(),
                destination.position(),
                traveler.getDeltaMovement(),
                destination.getYRot(),
                destination.getXRot(),
                TeleportTransition.DO_NOTHING
        );
        Component targetName = target.getDisplayName().copy().withStyle(ChatFormatting.AQUA);
        TeleportHandler.startTeleport(
                traveler,
                transition,
                () -> requester.sendSystemMessage(
                        Component.translatableWithFallback("swthb.tpa.accepted_sender",
                                "%s 已接受你的传送请求", targetName)
                                .withStyle(ChatFormatting.GREEN)
                ),
                () -> {}
        );
        return true;
    }

    /**
     * 聊天栏中的 [接受] 按钮（执行 /tpay）。
     */
    private static Component acceptButton() {
        return Component.translatableWithFallback("swthb.tpa.accept_btn", " [接受] ")
                .withStyle(style -> style
                        .withColor(ChatFormatting.GREEN)
                        .withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand("/tpay"))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.translatableWithFallback("swthb.tpa.accept_hover", "点击接受传送"))));
    }

    /**
     * 聊天栏中的 [拒绝] 按钮（执行 /tpan）。
     */
    private static Component denyButton() {
        return Component.translatableWithFallback("swthb.tpa.deny_btn", " [拒绝] ")
                .withStyle(style -> style
                        .withColor(ChatFormatting.RED)
                        .withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand("/tpan"))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.translatableWithFallback("swthb.tpa.deny_hover", "点击拒绝传送"))));
    }

    /**
     * 执行拒绝流程：通知双方。
     */
    private static void denyRequest(ServerPlayer target, PendingRequest req) {
        ServerPlayer requester = target.level().getServer().getPlayerList().getPlayer(req.requesterId());
        if (requester != null) {
            Component targetName = target.getDisplayName().copy().withStyle(ChatFormatting.AQUA);
            requester.sendSystemMessage(
                    Component.translatableWithFallback("swthb.tpa.denied_sender",
                            "%s 已拒绝你的传送请求", targetName)
                            .withStyle(ChatFormatting.RED)
            );
        }

        Component requesterName = requester != null
                ? requester.getDisplayName().copy().withStyle(ChatFormatting.AQUA)
                : Component.literal("该玩家");
        target.sendSystemMessage(
                Component.translatableWithFallback("swthb.tpa.denied_receiver",
                        "已拒绝 %s 的传送请求", requesterName)
                        .withStyle(ChatFormatting.RED)
        );
    }

    /**
     * 取出并移除玩家的待处理请求；已超时的请求视为不存在。
     */
    private static PendingRequest takeValidRequest(ServerPlayer player) {
        PendingRequest req = PENDING.remove(player.getUUID());
        if (req == null) return null;
        if (System.currentTimeMillis() - req.timestamp() > TIMEOUT_MS) return null;
        return req;
    }

    private static void cleanup() {
        long now = System.currentTimeMillis();
        PENDING.entrySet().removeIf(entry -> (now - entry.getValue().timestamp()) > TIMEOUT_MS);
    }
}
