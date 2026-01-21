package dev.fox.liminaldoors.mixin;

import dev.fox.liminaldoors.LiminalInstance;
import dev.fox.liminaldoors.LiminalSaveData;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInteractionManager.class)
public class MixinPlayerInteractionManager {

    @Shadow public World world;
    @Shadow public EntityPlayerMP player;

    @Inject(method = "tryHarvestBlock", at = @At("HEAD"), cancellable = true)
    private void liminaldoors$tryHarvestBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (world.isRemote) return;
        if (!(world instanceof WorldServer)) return;

        WorldServer ws = (WorldServer) world;

        // Карман у нас в Оверворлде
        if (player.dimension != 0) return;

        LiminalSaveData data = LiminalSaveData.get(ws);
        LiminalSaveData.ReturnPoint rp = data.getReturnPoint(player.getUniqueID());
        if (rp == null) return;

        LiminalInstance.Bounds b = LiminalInstance.boundsFor(rp.instanceId);
        if (b.contains(pos)) {
            cir.setReturnValue(false);
        }
    }
}