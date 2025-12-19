package com.minecraft.randomspawn.mixin;

import com.minecraft.randomspawn.Config;
import com.minecraft.randomspawn.RandomSpawnHandler;
import com.minecraft.randomspawn.RandomSpawnJsonBackup;
import com.minecraft.randomspawn.RandomSpawnStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListMixin {

	@Inject(method = "placeNewPlayer", at = @At("HEAD"))
	private void randomspawn$assignInitialSpawn(Connection connection, ServerPlayer player, CallbackInfo ci) {
		ServerLevel level = player.serverLevel();
		if (level == null || level.dimension() != Level.OVERWORLD) {
			return;
		}

		RandomSpawnStorage storage = RandomSpawnStorage.get(level);
		BlockPos manual = RandomSpawnJsonBackup.load(level, player.getUUID(), player.getGameProfile().getName());
		if (manual != null) {
			RandomSpawnHandler.debug(String.format("mixin: manual spawn override (%d,%d,%d) for %s",
					manual.getX(), manual.getY(), manual.getZ(), player.getGameProfile().getName()));
			placePlayer(player, manual);
			storage.setSpawn(player.getUUID(), manual.getX(), manual.getY(), manual.getZ());
			RandomSpawnJsonBackup.save(level, player, manual);
			return;
		}
		if (storage.isSpawned(player.getUUID())) {
			RandomSpawnHandler.debug("mixin: already spawned; skip");
			return;
		}

		BlockPos spawnPos = RandomSpawnHandler.findSafeSpawn(level, player);
		if (spawnPos == null) {
			spawnPos = RandomSpawnHandler.findSafeSpawn(level, player, Config.MAX_TRIES.get() * 10);
		}

		if (spawnPos == null) {
			BlockPos center = level.getSharedSpawnPos();
			level.getChunk(SectionPos.blockToSectionCoord(center.getX()), SectionPos.blockToSectionCoord(center.getZ()));
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.getX(), center.getZ());
			BlockPos candidate = new BlockPos(center.getX(), y, center.getZ());
			if (RandomSpawnHandler.isLocationSafe(level, candidate, player)) {
				spawnPos = candidate;
			}
		}

		if (spawnPos == null) {
			RandomSpawnHandler.debug("mixin: failed to find safe spawn; falling back to vanilla");
			return;
		}

		double fx = spawnPos.getX() + 0.5;
		double fy = spawnPos.getY() + 1;
		double fz = spawnPos.getZ() + 0.5;

		player.setRespawnPosition(Level.OVERWORLD, spawnPos, 0.0f, true, false);
		placePlayer(player, spawnPos);

		storage.setSpawn(player.getUUID(), spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
		RandomSpawnJsonBackup.save(level, player, spawnPos);
		RandomSpawnHandler.debug(String.format("mixin: assigned spawn (%d,%d,%d) for %s",
				spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), player.getGameProfile().getName()));
	}

	private static void placePlayer(ServerPlayer player, BlockPos spawnPos) {
		// Connection is not yet attached here, so just move the entity so vanilla syncs
		double fx = spawnPos.getX() + 0.5;
		double fy = spawnPos.getY() + 1;
		double fz = spawnPos.getZ() + 0.5;
		player.setPos(fx, fy, fz);
		player.setYBodyRot(0.0f);
		player.setYHeadRot(0.0f);
	}
}
