package com.minecraft.randomspawn;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class RandomSpawnJsonBackup {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE = new TypeToken<Map<String, SpawnEntry>>() {}.getType();
    private static final String FILE_NAME = "randomspawn_spawns.json";
    private static final String LEGACY_DIR = "randomspawn";

    private RandomSpawnJsonBackup() {}

    public static void save(ServerLevel level, ServerPlayer player, BlockPos pos) {
        save(level, player.getUUID(), getPlayerName(player), pos);
    }

    public static void save(ServerLevel level, UUID uuid, String playerName, BlockPos pos) {
        String name = sanitizeName(playerName);
        if (name.isEmpty()) {
            name = uuid.toString();
        }
        try {
            Path primary = getPrimaryFile(level);
            Map<String, SpawnEntry> data = readFile(primary);
            data.put(uuid.toString(), new SpawnEntry(pos.getX(), pos.getY(), pos.getZ(), level.dimension().location().toString(), name));
            writeFile(primary, data);
            LOGGER.info("[RandomSpawn] wrote spawn backup for {} ({}) to {}", uuid, name, primary);
        } catch (IOException e) {
            LOGGER.error("[RandomSpawn] Failed to write spawn backup", e);
        }
    }

    public static BlockPos load(ServerLevel level, UUID uuid, String playerName) {
        String desiredName = sanitizeName(playerName);
        try {
            ReadResult result = readPrimaryOrLegacy(level);
            SpawnEntry entry = result.data.get(uuid.toString());
            if (entry == null && !desiredName.isEmpty()) {
                entry = findByName(result.data, desiredName);
            }
            if (entry == null) {
                return null;
            }
            if (!level.dimension().location().toString().equals(entry.dimension)) {
                return null;
            }
            LOGGER.info("[RandomSpawn] loaded spawn backup for {} ({}) from {}", uuid, entry.name, result.source);
            return new BlockPos(entry.x, entry.y, entry.z);
        } catch (IOException e) {
            LOGGER.error("[RandomSpawn] Failed to read spawn backup", e);
            return null;
        }
    }

    private static ReadResult readPrimaryOrLegacy(ServerLevel level) throws IOException {
        Path primary = getPrimaryFile(level);
        Map<String, SpawnEntry> primaryData = readFile(primary);
        if (!primaryData.isEmpty() || Files.exists(primary)) {
            return new ReadResult(primaryData, primary);
        }
        Path legacy = getLegacyFile();
        if (Files.exists(legacy)) {
            Map<String, SpawnEntry> legacyData = readFile(legacy);
            LOGGER.info("[RandomSpawn] using legacy spawn backup at {}", legacy);
            return new ReadResult(legacyData, legacy);
        }
        return new ReadResult(primaryData, primary);
    }

    private static Path getPrimaryFile(ServerLevel level) throws IOException {
        MinecraftServer server = level.getServer();
        Path worldDir = server != null ? server.getWorldPath(LevelResource.ROOT) : FMLPaths.CONFIGDIR.get();
        Path dir = worldDir.resolve("serverconfig").resolve("randomspawn");
        Files.createDirectories(dir);
        return dir.resolve(FILE_NAME);
    }

    private static Path getLegacyFile() {
        Path dir = FMLPaths.CONFIGDIR.get().resolve(LEGACY_DIR);
        return dir.resolve(FILE_NAME);
    }

    private static Map<String, SpawnEntry> readFile(Path file) throws IOException {
        if (!Files.exists(file)) {
            return new HashMap<>();
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Map<String, SpawnEntry> data = GSON.fromJson(reader, TYPE);
            return data != null ? data : new HashMap<>();
        } catch (JsonParseException e) {
            LOGGER.error("[RandomSpawn] Spawn backup file is corrupted; resetting", e);
            return new HashMap<>();
        }
    }

    private static void writeFile(Path file, Map<String, SpawnEntry> data) throws IOException {
        Files.createDirectories(file.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(data, TYPE, writer);
        }
    }

    private static SpawnEntry findByName(Map<String, SpawnEntry> data, String name) {
        String search = name.toLowerCase(Locale.ROOT);
        for (SpawnEntry entry : data.values()) {
            if (entry.name != null && entry.name.toLowerCase(Locale.ROOT).equals(search)) {
                return entry;
            }
        }
        return null;
    }

    private static String sanitizeName(String name) {
        return name == null ? "" : name.trim();
    }

    private static String getPlayerName(ServerPlayer player) {
        String profileName = player.getGameProfile().getName();
        if (profileName != null && !profileName.isBlank()) {
            return profileName;
        }
        return player.getName().getString();
    }

    private static class SpawnEntry {
        final int x;
        final int y;
        final int z;
        final String dimension;
        final String name;

        SpawnEntry(int x, int y, int z, String dimension, String name) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.name = name;
        }
    }

    private static class ReadResult {
        final Map<String, SpawnEntry> data;
        final Path source;

        ReadResult(Map<String, SpawnEntry> data, Path source) {
            this.data = data;
            this.source = source;
        }
    }
}
