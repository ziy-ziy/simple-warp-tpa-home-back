package zy.swthb;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zy.swthb.command.BackCommand;
import zy.swthb.command.HomeCommand;
import zy.swthb.command.SwthbConfigCommand;
import zy.swthb.command.TpaCommand;
import zy.swthb.command.WarpCommand;
import zy.swthb.config.ModConfig;
import zy.swthb.handler.TeleportHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class SimpleWarpTpaHomeBack implements ModInitializer {
	public static final String MOD_ID = "simple-warp-tpa-home-back";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// 注册所有命令
		CommandRegistrationCallback.EVENT.register((dispatcher, _, _) -> TpaCommand.register(dispatcher));
		CommandRegistrationCallback.EVENT.register((dispatcher, _, _) -> HomeCommand.register(dispatcher));
		CommandRegistrationCallback.EVENT.register((dispatcher, _, _) -> SwthbConfigCommand.register(dispatcher));
		CommandRegistrationCallback.EVENT.register((dispatcher, _, _) -> WarpCommand.register(dispatcher));
		CommandRegistrationCallback.EVENT.register((dispatcher, _, _) -> BackCommand.register(dispatcher));

		// 服务器启动时加载当前存档的数据
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			ModConfig.load(server);
			LOGGER.info("存档数据已加载");
		});

		// 每 tick 处理传送倒计时
		ServerTickEvents.START_SERVER_TICK.register(server -> TeleportHandler.tick());

		// 服务器关闭时保存数据并清除旧状态（切换存档时确保互不影响）
		ServerLifecycleEvents.SERVER_STOPPING.register(_ -> {
			ModConfig.getInstance().save();
			ModConfig.reset();
			LOGGER.info("存档数据已保存");
		});

		LOGGER.info("Simple Warp TPA Home Back 已初始化！");
	}
}
