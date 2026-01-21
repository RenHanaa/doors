package dev.fox.liminaldoors.mixin;

import dev.fox.liminaldoors.LiminalDoors;
import dev.fox.liminaldoors.LiminalInstance;
import dev.fox.liminaldoors.LiminalSaveData;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockDoor.class)
public class MixinBlockDoor {

    @Inject(method = "onBlockActivated", at = @At("HEAD"), cancellable = true)
    private void liminaldoors$onBlockActivated(
        World world, BlockPos pos, IBlockState state,
        EntityPlayer player, EnumHand hand, EnumFacing facing,
        float hitX, float hitY, float hitZ,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (world.isRemote) return;
        if (!(player instanceof EntityPlayerMP)) return;

        EntityPlayerMP mp = (EntityPlayerMP) player;
        MinecraftServer server = mp.getServer();
        if (server == null) return;

        WorldServer overworld = server.getWorld(0);
        if (overworld == null) return;

        // Если игрок в кармане и это выходная дверь, вернуть назад
        LiminalSaveData data = LiminalSaveData.get(overworld);
        LiminalSaveData.ReturnPoint rp = data.getReturnPoint(mp.getUniqueID());

        if (rp != null) {
            LiminalInstance.Bounds b = LiminalInstance.boundsFor(rp.instanceId);
            if (b.contains(pos) && LiminalInstance.isExitDoor(overworld, pos)) {
                teleportTo(mp, rp.dim, rp.x, rp.y, rp.z, rp.yaw, rp.pitch);
                data.clearReturnPoint(mp.getUniqueID());
                cir.setReturnValue(true);
                return;
            }

            // Если внутри кармана, обычные двери не должны триггерить новый вход
            if (b.contains(pos)) return;
        }

        // 1% шанс
        if (LiminalDoors.RNG.nextDouble() > LiminalDoors.TRIGGER_CHANCE) return;

        // Запоминаем точку возврата
        int instanceId = data.allocateInstanceId();
        LiminalSaveData.ReturnPoint nrp = new LiminalSaveData.ReturnPoint();
        nrp.dim = mp.dimension;
        nrp.x = mp.posX;
        nrp.y = mp.posY;
        nrp.z = mp.posZ;
        nrp.yaw = mp.rotationYaw;
        nrp.pitch = mp.rotationPitch;
        nrp.instanceId = instanceId;
        data.setReturnPoint(mp.getUniqueID(), nrp);

        // Генерим карман
        long seed = world.getSeed() ^ mp.getUniqueID().getMostSignificantBits() ^ instanceId;
        LiminalInstance.generate(overworld, instanceId, seed);

        // Телепорт в Оверворлд в карман
        BlockPos spawn = LiminalInstance.spawnFor(instanceId);
        teleportTo(mp, 0, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, mp.rotationYaw, mp.rotationPitch);

        cir.setReturnValue(true);
    }

    private static void teleportTo(EntityPlayerMP p, int dim, double x, double y, double z, float yaw, float pitch) {
        if (p.dimension != dim) {
            p.changeDimension(dim);
        }
        p.connection.setPlayerLocation(x, y, z, yaw, pitch);
    }
}