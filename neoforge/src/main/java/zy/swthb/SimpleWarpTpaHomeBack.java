package zy.swthb;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.server.level.ServerPlayer;
import zy.swthb.command.BackCommand;
import zy.swthb.command.BackHandler;
import zy.swthb.command.HomeCommand;
import zy.swthb.command.SwthbConfigCommand;
import zy.swthb.command.TeleportHandler;
import zy.swthb.command.TpaCommand;
import zy.swthb.command.WarpCommand;
import zy.swthb.config.ModConfig;

@Mod(Constants.MOD_ID)
public class SimpleWarpTpaHomeBack {

    public SimpleWarpTpaHomeBack(IEventBus eventBus) {
        // 注册所有命令
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> {
            TpaCommand.register(event.getDispatcher());
            HomeCommand.register(event.getDispatcher());
            SwthbConfigCommand.register(event.getDispatcher());
            WarpCommand.register(event.getDispatcher());
            BackCommand.register(event.getDispatcher());
        });

        // 服务器启动时加载当前存档的数据
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> {
            ModConfig.load(event.getServer());
            Constants.LOG.info("存档数据已加载");
        });

        // 玩家下线时清理 back 点
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                BackHandler.onPlayerLeave(player);
            }
        });

        // 每 tick 处理传送倒计时；每 5 分钟清理超时的 TPA 请求
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Pre event) -> {
            TeleportHandler.tick();
            if (event.getServer().getTickCount() % 6000 == 0) {
                TpaCommand.cleanup();
            }
        });

        // 服务器关闭时保存数据并清除旧状态（切换存档时确保互不影响）
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> {
            ModConfig.getInstance().save();
            ModConfig.reset();
            BackHandler.clearAll();
            Constants.LOG.info("存档数据已保存");
        });

        Constants.LOG.info("Simple Warp Tpa Home Back 已初始化！");
    }
}
