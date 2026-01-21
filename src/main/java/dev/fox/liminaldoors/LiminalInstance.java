package dev.fox.liminaldoors;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.util.Random;

public class LiminalInstance {

    public static class Bounds {
        public final int minX, minY, minZ, maxX, maxY, maxZ;
        public Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.minX = minX; this.minY = minY; this.minZ = minZ;
            this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
        }
        public boolean contains(BlockPos p) {
            return p.getX() >= minX && p.getX() <= maxX
                && p.getY() >= minY && p.getY() <= maxY
                && p.getZ() >= minZ && p.getZ() <= maxZ;
        }
    }

    public static Bounds boundsFor(int instanceId) {
        int offset = instanceId * 1024;
        int x0 = LiminalDoors.BASE_X + offset;
        int z0 = LiminalDoors.BASE_Z + offset;
        int y0 = LiminalDoors.BASE_Y;

        return new Bounds(
            x0, y0, z0,
            x0 + LiminalDoors.SIZE_XZ - 1,
            y0 + LiminalDoors.HEIGHT - 1,
            z0 + LiminalDoors.SIZE_XZ - 1
        );
    }

    public static BlockPos spawnFor(int instanceId) {
        Bounds b = boundsFor(instanceId);
        return new BlockPos(b.minX + 2, b.minY + 1, b.minZ + 2);
    }

    public static BlockPos generate(WorldServer world, int instanceId, long seed) {
        Bounds b = boundsFor(instanceId);
        Random r = new Random(seed);

        // Пол и потолок
        for (int x = b.minX; x <= b.maxX; x++) {
            for (int z = b.minZ; z <= b.maxZ; z++) {
                world.setBlockState(new BlockPos(x, b.minY, z), Blocks.STAINED_HARDENED_CLAY.getDefaultState(), 2);
                world.setBlockState(new BlockPos(x, b.maxY, z), Blocks.SANDSTONE.getDefaultState(), 2);
            }
        }

        // Комнаты и коридоры шумом
        for (int x = b.minX; x <= b.maxX; x++) {
            for (int z = b.minZ; z <= b.maxZ; z++) {
                boolean border = (x == b.minX || x == b.maxX || z == b.minZ || z == b.maxZ);
                boolean wall = border || (r.nextInt(7) == 0);

                for (int y = b.minY + 1; y <= b.maxY - 1; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (wall) world.setBlockState(p, Blocks.SANDSTONE.getDefaultState(), 2);
                    else world.setBlockToAir(p);
                }

                // “Лампы”
                if ((x - b.minX) % 6 == 0 && (z - b.minZ) % 6 == 0) {
                    world.setBlockState(new BlockPos(x, b.maxY - 1, z), Blocks.GLOWSTONE.getDefaultState(), 2);
                }
            }
        }

        // Еда
        for (int i = 0; i < 18; i++) {
            int cx = b.minX + 4 + r.nextInt(LiminalDoors.SIZE_XZ - 8);
            int cz = b.minZ + 4 + r.nextInt(LiminalDoors.SIZE_XZ - 8);
            BlockPos chestPos = new BlockPos(cx, b.minY + 1, cz);

            if (world.isAirBlock(chestPos) && world.isAirBlock(chestPos.up())) {
                world.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), 2);
                if (world.getTileEntity(chestPos) instanceof TileEntityChest) {
                    TileEntityChest chest = (TileEntityChest) world.getTileEntity(chestPos);
                    fillFood(chest, r);
                }
            }
        }

        // Выходная дверь
        BlockPos exitDoorPos = findExitSpot(world, b, r);
        placeExitDoor(world, exitDoorPos);
        return exitDoorPos;
    }

    private static void fillFood(IInventory inv, Random r) {
        int rolls = 3 + r.nextInt(5);
        for (int i = 0; i < rolls; i++) {
            int slot = r.nextInt(inv.getSizeInventory());
            ItemStack stack;
            int pick = r.nextInt(4);
            if (pick == 0) stack = new ItemStack(Items.BREAD, 1 + r.nextInt(3));
            else if (pick == 1) stack = new ItemStack(Items.APPLE, 1 + r.nextInt(4));
            else if (pick == 2) stack = new ItemStack(Items.COOKED_BEEF, 1 + r.nextInt(2));
            else stack = new ItemStack(Items.COOKED_CHICKEN, 1 + r.nextInt(3));
            inv.setInventorySlotContents(slot, stack);
        }
    }

    private static BlockPos findExitSpot(WorldServer world, Bounds b, Random r) {
        for (int tries = 0; tries < 3000; tries++) {
            int x = b.minX + 3 + r.nextInt(LiminalDoors.SIZE_XZ - 6);
            int z = b.minZ + 3 + r.nextInt(LiminalDoors.SIZE_XZ - 6);
            BlockPos p = new BlockPos(x, b.minY + 1, z);

            if (!world.isAirBlock(p) && world.isAirBlock(p.east())) {
                return p.east();
            }
        }
        return spawnFor(1).add(10, 0, 10);
    }

    private static void placeExitDoor(WorldServer world, BlockPos doorPos) {
        // Маркер под дверью
        world.setBlockState(doorPos.down(), Blocks.GOLD_BLOCK.getDefaultState(), 2);

        // Дверь в 1.12 это нижняя и верхняя часть
        world.setBlockState(doorPos, Blocks.OAK_DOOR.getDefaultState(), 2);
        world.setBlockState(doorPos.up(), Blocks.OAK_DOOR.getDefaultState(), 2);
    }

    public static boolean isExitDoor(WorldServer world, BlockPos doorPos) {
        return world.getBlockState(doorPos.down()).getBlock() == Blocks.GOLD_BLOCK;
    }
}