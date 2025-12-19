package com.minecraft.randomspawn;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {

public static ForgeConfigSpec COMMON_CONFIG;

public static ForgeConfigSpec.IntValue SPAWN_RANGE;
public static ForgeConfigSpec.IntValue MIN_DISTANCE;
public static ForgeConfigSpec.IntValue MAX_TRIES;
public static ForgeConfigSpec.IntValue HEIGHT_DIFF_LIMIT;
public static ForgeConfigSpec.BooleanValue DEBUG_LOGS;

static {
ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

builder.push("RandomSpawn Settings");

SPAWN_RANGE = builder
.comment("ランダムスポーン範囲 (中心から半径)")
.defineInRange("spawnRange", 4000, 100, 50000);

MIN_DISTANCE = builder
.comment("他プレイヤーとの最低距離")
.defineInRange("minDistance", 300, 10, 5000);

MAX_TRIES = builder
.comment("スポーン地点の探索試行回数")
.defineInRange("maxTries", 20, 1, 200);

HEIGHT_DIFF_LIMIT = builder
.comment("地形の許容高低差")
.defineInRange("heightDiff", 4, 1, 20);

DEBUG_LOGS = builder
.comment("デバッグログを有効化 (探索候補や却下理由を出力)")
.define("debugLogs", false);

builder.pop();

COMMON_CONFIG = builder.build();
}
}

