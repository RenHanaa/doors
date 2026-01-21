package dev.fox.liminaldoors;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LiminalSaveData extends WorldSavedData {
    private static final String DATA_NAME = "liminaldoors_data";

    public static class ReturnPoint {
        public int dim;
        public double x, y, z;
        public float yaw, pitch;
        public int instanceId;

        public NBTTagCompound toNbt() {
            NBTTagCompound t = new NBTTagCompound();
            t.setInteger("dim", dim);
            t.setDouble("x", x);
            t.setDouble("y", y);
            t.setDouble("z", z);
            t.setFloat("yaw", yaw);
            t.setFloat("pitch", pitch);
            t.setInteger("instanceId", instanceId);
            return t;
        }

        public static ReturnPoint fromNbt(NBTTagCompound t) {
            ReturnPoint rp = new ReturnPoint();
            rp.dim = t.getInteger("dim");
            rp.x = t.getDouble("x");
            rp.y = t.getDouble("y");
            rp.z = t.getDouble("z");
            rp.yaw = t.getFloat("yaw");
            rp.pitch = t.getFloat("pitch");
            rp.instanceId = t.getInteger("instanceId");
            return rp;
        }
    }

    private final Map<UUID, ReturnPoint> returnPoints = new HashMap<>();
    private int nextInstanceId = 1;

    public LiminalSaveData() {
        super(DATA_NAME);
    }

    public LiminalSaveData(String name) {
        super(name);
    }

    public static LiminalSaveData get(World world) {
        MapStorage storage = world.getMapStorage();
        LiminalSaveData data = (LiminalSaveData) storage.getOrLoadData(LiminalSaveData.class, DATA_NAME);
        if (data == null) {
            data = new LiminalSaveData();
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public int allocateInstanceId() {
        int id = nextInstanceId++;
        markDirty();
        return id;
    }

    public void setReturnPoint(UUID playerId, ReturnPoint rp) {
        returnPoints.put(playerId, rp);
        markDirty();
    }

    public ReturnPoint getReturnPoint(UUID playerId) {
        return returnPoints.get(playerId);
    }

    public void clearReturnPoint(UUID playerId) {
        returnPoints.remove(playerId);
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        nextInstanceId = nbt.getInteger("nextInstanceId");
        returnPoints.clear();

        NBTTagCompound players = nbt.getCompoundTag("players");
        for (String key : players.getKeySet()) {
            UUID id = UUID.fromString(key);
            returnPoints.put(id, ReturnPoint.fromNbt(players.getCompoundTag(key)));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("nextInstanceId", nextInstanceId);

        NBTTagCompound players = new NBTTagCompound();
        for (Map.Entry<UUID, ReturnPoint> e : returnPoints.entrySet()) {
            players.setTag(e.getKey().toString(), e.getValue().toNbt());
        }
        nbt.setTag("players", players);

        return nbt;
    }
}