package zy.swthb;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import zy.swthb.command.BackCommand;
import zy.swthb.command.BackHandler;
import zy.swthb.command.HomeCommand;
import zy.swthb.command.SwthbConfigCommand;
import zy.swthb.command.TeleportHandler;
import zy.swthb.command.TpaCommand;
import zy.swthb.command.WarpCommand;
import zy.swthb.config.ModConfig;

public class SimpleWarpTpaHomeBack implements ModInitializer {

    @Override
    public void onInitialize() {
        // 注册所有命令
        CommandRegistrationCallback.EVENT.register((dispatcher, _, _) -> {
            TpaCommand.register(dispatcher);
            HomeCommand.register(dispatcher);
            SwthbConfigCommand.register(dispatcher);
            WarpCommand.register(dispatcher);
            BackCommand.register(dispatcher);
        });

        // 服务器启动时加载当前存档的数据
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ModConfig.load(server);
            Constants.LOG.info("存档数据已加载");
        });

        // 玩家下线时清理 back 点
        ServerPlayerEvents.LEAVE.register(BackHandler::onPlayerLeave);

        // 每 tick 处理传送倒计时
        ServerTickEvents.START_SERVER_TICK.register(server -> TeleportHandler.tick());

        // 每 5 分钟清理超时的 TPA 请求（请求超时时长为 1 分钟）
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 6000 == 0) {
                TpaCommand.cleanup();
            }
        });

        // 服务器关闭时保存数据并清除旧状态（切换存档时确保互不影响）
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ModConfig.getInstance().save();
            ModConfig.reset();
            BackHandler.clearAll();
            Constants.LOG.info("存档数据已保存");
        });

        Constants.LOG.info("Simple Warp Tpa Home Back 已初始化！");
    }
}
