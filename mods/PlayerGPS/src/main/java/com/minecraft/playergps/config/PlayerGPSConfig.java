package com.minecraft.playergps.config;

import com.minecraft.playergps.PlayerGPSMod;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = PlayerGPSMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PlayerGPSConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.BooleanValue INCLUDE_SELF_IN_RESULTS;
    private static final ForgeConfigSpec.IntValue MAX_PLAYERS_TO_DISPLAY;
    private static final ForgeConfigSpec.BooleanValue CONSUME_IN_CREATIVE;

    public static final ForgeConfigSpec SPEC;

    public static boolean includeSelf = true;
    public static int maxPlayersToDisplay = 0;
    public static boolean consumeInCreative = false;

    static {
        BUILDER.push("player_locator");

        INCLUDE_SELF_IN_RESULTS = BUILDER
                .comment("Set to true to include the player using the locator in the report output.")
                .define("includeSelf", true);

        MAX_PLAYERS_TO_DISPLAY = BUILDER
                .comment("Maximum number of players to list per activation. Set to 0 for no limit.")
                .defineInRange("maxPlayers", 0, 0, 200);

    CONSUME_IN_CREATIVE = BUILDER
        .comment("If true, the locator is consumed even for players with creative-mode abilities.")
        .define("consumeInCreative", false);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private PlayerGPSConfig() {
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        includeSelf = INCLUDE_SELF_IN_RESULTS.get();
        maxPlayersToDisplay = MAX_PLAYERS_TO_DISPLAY.get();
        consumeInCreative = CONSUME_IN_CREATIVE.get();
    }
}
