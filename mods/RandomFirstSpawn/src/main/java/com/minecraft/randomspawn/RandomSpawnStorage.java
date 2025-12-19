package com.minecraft.randomspawn;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RandomSpawnStorage extends SavedData {
    private static final String DATA_NAME = "randomspawn_storage";

    private final Map<String, CompoundTag> players = new HashMap<>();

    public static RandomSpawnStorage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RandomSpawnStorage::load, RandomSpawnStorage::new, DATA_NAME);
    }

    public RandomSpawnStorage() {}

    public static RandomSpawnStorage load(CompoundTag tag) {
        RandomSpawnStorage storage = new RandomSpawnStorage();
        CompoundTag playersTag = tag.getCompound("players");
        for (String key : playersTag.getAllKeys()) {
            storage.players.put(key, playersTag.getCompound(key));
        }
        return storage;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag playersTag = new CompoundTag();
        for (Map.Entry<String, CompoundTag> e : players.entrySet()) {
            playersTag.put(e.getKey(), e.getValue().copy());
        }
        tag.put("players", playersTag);
        return tag;
    }

    public boolean isSpawned(UUID uuid) {
        CompoundTag t = players.get(uuid.toString());
        return t != null && t.getBoolean("spawned");
    }

    public BlockPosData getSpawn(UUID uuid) {
        CompoundTag t = players.get(uuid.toString());
        if (t == null) return null;
        if (!t.contains("x")) return null;
        return new BlockPosData(t.getInt("x"), t.getInt("y"), t.getInt("z"));
    }

    public void setSpawn(UUID uuid, int x, int y, int z) {
        CompoundTag t = players.computeIfAbsent(uuid.toString(), k -> new CompoundTag());
        t.putBoolean("spawned", true);
        t.putInt("x", x);
        t.putInt("y", y);
        t.putInt("z", z);
        setDirty();
    }

    public static class BlockPosData {
        public final int x, y, z;
        public BlockPosData(int x, int y, int z) {
            this.x = x; this.y = y; this.z = z;
        }
    }
}
