package zy.swthb.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 点击聊天中的可执行链接（如 /home、/tpay）后自动关闭聊天框。
 * <p>
 * Minecraft 26w02a 的 {@link ClientPacketListener#sendUnattendedCommand(String, Screen)}
 * 在发送命令后会自动调用 {@code gui.setScreen(screen)} 把聊天框重新设回去，
 * 因此需要在此方法返回后再次关闭屏幕。
 */
@Mixin(ClientPacketListener.class)
public class ChatAutoCloseMixin {

    @Inject(at = @At("TAIL"), method = "sendUnattendedCommand")
    private void afterSendUnattendedCommand(String command, Screen screen, CallbackInfo ci) {
        // 仅当从 ChatScreen 触发的命令才自动关闭聊天框
        if (!(screen instanceof ChatScreen)) return;

        Minecraft mc = Minecraft.getInstance();
        // 仅当当前屏幕仍为原理的 ChatScreen（说明命令已直接发送，未弹出权限确认对话框）
        if (mc.gui.screen() == screen) {
            mc.gui.setScreen(null);
        }
    }
}
