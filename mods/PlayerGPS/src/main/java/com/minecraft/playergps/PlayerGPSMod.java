package com.minecraft.playergps;

import com.minecraft.playergps.config.PlayerGPSConfig;
import com.minecraft.playergps.item.PlayerLocatorItem;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(PlayerGPSMod.MODID)
public class PlayerGPSMod {
    public static final String MODID = "playergps";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<Item> PLAYER_LOCATOR = ITEMS.register("player_locator",
            () -> new PlayerLocatorItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.RARE)));

    public PlayerGPSMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ITEMS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        context.registerConfig(ModConfig.Type.COMMON, PlayerGPSConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Player GPS initialized");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(PLAYER_LOCATOR);
        }
    }
}
