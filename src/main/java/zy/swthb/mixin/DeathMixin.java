package zy.swthb.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zy.swthb.handler.BackHandler;

/**
 * 玩家死亡时记录死亡位置，供 /back 使用。
 */
@Mixin(ServerPlayer.class)
public class DeathMixin {

    @Inject(at = @At("HEAD"), method = "die")
    private void onDeath(DamageSource source, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        BackHandler.saveBackPoint(player, BackHandler.LocationType.DEATH);
    }
}
