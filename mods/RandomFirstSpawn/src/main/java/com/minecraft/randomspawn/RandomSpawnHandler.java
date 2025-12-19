package com.minecraft.randomspawn;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.BiomeTags;
import net.minecraft.core.SectionPos;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.nbt.CompoundTag;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod.EventBusSubscriber
public class RandomSpawnHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD)
            return;

        RandomSpawnStorage storage = RandomSpawnStorage.get(level);
        RandomSpawnStorage.BlockPosData stored = storage.getSpawn(player.getUUID());

        if (!hasBedOrAnchorRespawn(player)) {
            BlockPos manualOverride = RandomSpawnJsonBackup.load(level, player.getUUID(), player.getGameProfile().getName());
            if (manualOverride != null) {
                boolean differs = stored == null
                        || stored.x != manualOverride.getX()
                        || stored.y != manualOverride.getY()
                        || stored.z != manualOverride.getZ();
                if (differs) {
                    applySpawn(player, level, manualOverride, 0.0f);
                    storage.setSpawn(player.getUUID(), manualOverride.getX(), manualOverride.getY(), manualOverride.getZ());
                    RandomSpawnJsonBackup.save(level, player, manualOverride);
                    debug(String.format("login: applied manual spawn override for %s at x=%d y=%d z=%d",
                            player.getGameProfile().getName(), manualOverride.getX(), manualOverride.getY(), manualOverride.getZ()));
                    return;
                }
            }
        }

        // 初回ログインでなければ終了
        if (storage.isSpawned(player.getUUID())) {
            debug("login: already spawned; skip random spawn");
            return;
        }

        // ランダムスポーン地点を探索
        BlockPos spawnPos = findSafeSpawn(level, player);

        // 追加探索（上限を増やして再試行）
        if (spawnPos == null) {
            spawnPos = findSafeSpawn(level, player, Config.MAX_TRIES.get() * 10);
        }

        // 最終手段：水面に出ないようにMOTION_BLOCKING_NO_LEAVESで原点を安全化
        if (spawnPos == null) {
            BlockPos center = level.getSharedSpawnPos();
            // ensure center chunk is loaded before sampling height
            level.getChunk(SectionPos.blockToSectionCoord(center.getX()), SectionPos.blockToSectionCoord(center.getZ()));
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.getX(), center.getZ());
            BlockPos candidate = new BlockPos(center.getX(), y, center.getZ());
            if (isLocationSafe(level, candidate, player)) {
                spawnPos = candidate;
            }
        }

        // TP: ログイン直後にサーバー→クライアントへ初期位置を同期して原点露出を防ぐ
        final BlockPos finalSpawnPos = spawnPos;
        applySpawn(player, level, finalSpawnPos, 0.0f);

        // 保存はテレポートと同ティックで実施
        RandomSpawnStorage s = RandomSpawnStorage.get(level);
        s.setSpawn(player.getUUID(), finalSpawnPos.getX(), finalSpawnPos.getY(), finalSpawnPos.getZ());
        RandomSpawnJsonBackup.save(level, player, finalSpawnPos);
        debug(String.format("saved spawn for %s at x=%d y=%d z=%d", player.getGameProfile().getName(), finalSpawnPos.getX(), finalSpawnPos.getY(), finalSpawnPos.getZ()));
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (event.isEndConquered()) {
            return;
        }

        ServerLevel level = player.serverLevel();
        if (level == null || level.dimension() != Level.OVERWORLD) {
            return;
        }

        if (hasBedOrAnchorRespawn(player)) {
            return;
        }

        RandomSpawnStorage storage = RandomSpawnStorage.get(level);
        RandomSpawnStorage.BlockPosData stored = storage.getSpawn(player.getUUID());
        BlockPos manual = RandomSpawnJsonBackup.load(level, player.getUUID(), player.getGameProfile().getName());
        BlockPos spawnPos = null;
        if (manual != null) {
            spawnPos = manual;
            storage.setSpawn(player.getUUID(), manual.getX(), manual.getY(), manual.getZ());
        } else if (stored != null) {
            spawnPos = new BlockPos(stored.x, stored.y, stored.z);
        }

        if (spawnPos == null) {
            return;
        }

        applySpawn(player, level, spawnPos, player.getYRot());
        RandomSpawnJsonBackup.save(level, player, spawnPos);

        debug(String.format("respawn: fallback to stored random spawn for %s at x=%d y=%d z=%d",
                player.getGameProfile().getName(), spawnPos.getX(), spawnPos.getY(), spawnPos.getZ()));
    }

    private static void applySpawn(ServerPlayer player, ServerLevel level, BlockPos spawnPos, float yaw) {
        double fx = spawnPos.getX() + 0.5;
        double fy = spawnPos.getY() + 1;
        double fz = spawnPos.getZ() + 0.5;
        player.setRespawnPosition(Level.OVERWORLD, spawnPos, 0.0f, true, false);
        player.teleportTo(level, fx, fy, fz, yaw, player.getXRot());
        if (player.connection != null) {
            player.connection.teleport(fx, fy, fz, yaw, player.getXRot());
        }
    }

    private static boolean hasBedOrAnchorRespawn(ServerPlayer player) {
        BlockPos respawn = player.getRespawnPosition();
        if (respawn == null) {
            return false;
        }
        if (player.isSpawnForced()) {
            return false;
        }
        ServerLevel respawnLevel = player.server.getLevel(player.getRespawnDimension());
        if (respawnLevel == null) {
            return false;
        }
        BlockState state = respawnLevel.getBlockState(respawn);
        return state.getBlock() instanceof BedBlock || state.is(Blocks.RESPAWN_ANCHOR);
    }

    public static BlockPos findSafeSpawn(ServerLevel level, ServerPlayer player) {
        int tries = Config.MAX_TRIES.get();
        return findSafeSpawn(level, player, tries);
    }

    public static BlockPos findSafeSpawn(ServerLevel level, ServerPlayer player, int tries) {

        for (int i = 0; i < tries; i++) {

            int range = Config.SPAWN_RANGE.get();
            BlockPos center = level.getSharedSpawnPos();
            int x = center.getX() + (player.getRandom().nextInt(range * 2 + 1) - range);
            int z = center.getZ() + (player.getRandom().nextInt(range * 2 + 1) - range);

            // 確実に高さを取得するため、対象チャンクを読み込み/生成
            level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            debug(String.format("try#%d candidate x=%d y=%d z=%d", (i + 1), x, y, z));

            // 境界チェック + 最低高度チェック
            if (!level.isInWorldBounds(pos)) {
                debug("reject: out of world bounds");
                continue;
            }
            if (y <= level.getMinBuildHeight()) {
                debug("reject: below min build height");
                continue;
            }

            // バイオーム（海/川）除外
            var biomeHolder = level.getBiome(pos);
            if (biomeHolder.is(BiomeTags.IS_OCEAN) || biomeHolder.is(BiomeTags.IS_RIVER)) {
                debug("reject: ocean/river biome");
                continue;
            }

            // 流体（水など）除外
            if (level.getFluidState(pos).is(FluidTags.WATER) || level.getFluidState(pos.above()).is(FluidTags.WATER)) {
                debug("reject: water at feet or head");
                continue;
            }

            // 足場の安定性（真下ブロックが上面を支えられるか）
            BlockPos ground = pos.below();
            if (!level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)) {
                debug("reject: ground not sturdy");
                continue;
            }

            // スペースの空き（衝突形状が空）
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                debug("reject: feet blocked");
                continue;
            }
            if (!level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
                debug("reject: head blocked");
                continue;
            }

            if (!isLocationSafe(level, pos, player)) {
                debug("reject: not flat enough or too close to others");
                continue;
            }

            debug(String.format("accept: x=%d y=%d z=%d", pos.getX(), pos.getY(), pos.getZ()));
            return pos;
        }
        return null;
    }

    public static boolean isLocationSafe(ServerLevel level, BlockPos pos, ServerPlayer self) {
        // 地形の平坦さ
        if (!isFlatEnough(level, pos)) return false;
        // 他プレイヤー距離
        if (!isFarFromOthers(level, pos, self)) return false;
        return true;
    }

    public static void debug(String msg) {
        if (Config.DEBUG_LOGS.get() != null && Config.DEBUG_LOGS.get()) {
            LOGGER.info("[RandomSpawn] " + msg);
        }
    }

    private static boolean isFlatEnough(ServerLevel level, BlockPos pos) {
        int centerY = pos.getY();
        int limit = Config.HEIGHT_DIFF_LIMIT.get();

        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        pos.getX() + dx, pos.getZ() + dz);
                if (Math.abs(y - centerY) > limit) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isFarFromOthers(ServerLevel level, BlockPos pos, ServerPlayer self) {
        int minDist = Config.MIN_DISTANCE.get();
        for (ServerPlayer other : level.players()) {
            if (other == self) continue;

            double dist = other.position().distanceTo(
                    new net.minecraft.world.phys.Vec3(pos.getX(), pos.getY(), pos.getZ())
            );

            if (dist < minDist)
                return false;
        }
        return true;
    }
}