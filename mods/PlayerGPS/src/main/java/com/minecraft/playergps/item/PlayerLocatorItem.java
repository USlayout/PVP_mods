package com.minecraft.playergps.item;

import com.minecraft.playergps.config.PlayerGPSConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.MutableComponent;

import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerLocatorItem extends Item {
    private static final Comparator<ServerPlayer> PLAYER_SORT = Comparator
            .comparing((ServerPlayer player) -> player.level().dimension().location().toString())
            .thenComparing(player -> player.getGameProfile().getName());

    public PlayerLocatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            playClientFeedback(level, player);
        } else if (player instanceof ServerPlayer serverPlayer) {
            announcePlayerPositions(serverPlayer);
            consumeItem(serverPlayer, stack);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private void announcePlayerPositions(ServerPlayer user) {
        MinecraftServer server = user.getServer();
        if (server == null) {
            return;
        }

        UUID selfId = user.getUUID();
        List<ServerPlayer> targets = server.getPlayerList().getPlayers().stream()
                .filter(candidate -> PlayerGPSConfig.includeSelf || !candidate.getUUID().equals(selfId))
                .sorted(PLAYER_SORT)
                .collect(Collectors.toList());

        if (targets.isEmpty()) {
            user.sendSystemMessage(Component.translatable("message.playergps.locator.empty"));
            return;
        }

        int max = PlayerGPSConfig.maxPlayersToDisplay;
        boolean limited = max > 0 && targets.size() > max;
        int displayed = limited ? max : targets.size();

        user.sendSystemMessage(Component.translatable("message.playergps.locator.header", displayed, targets.size()));

        for (int i = 0; i < displayed; i++) {
            user.sendSystemMessage(buildEntryComponent(targets.get(i)));
        }

        if (limited) {
            user.sendSystemMessage(Component.translatable("message.playergps.locator.truncated", targets.size() - displayed));
        }
    }

    private Component buildEntryComponent(ServerPlayer target) {
        BlockPos pos = target.blockPosition();
        ResourceLocation dimensionLocation = target.level().dimension().location();
        Component dimensionName = Component.translatable(Util.makeDescriptionId("dimension", dimensionLocation));

        return Component.translatable(
                        "message.playergps.locator.entry",
                        target.getDisplayName(),
                        dimensionName,
                        pos.getX(),
                        pos.getY(),
                        pos.getZ())
                .withStyle(ChatFormatting.GOLD);
    }

    private void playClientFeedback(Level level, Player player) {
        level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, 1.2F, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.playergps.player_locator.tooltip.line1")
                .withStyle(ChatFormatting.GRAY));

        MutableComponent secondLine = PlayerGPSConfig.consumeInCreative
                ? Component.translatable("item.playergps.player_locator.tooltip.line2.consume")
                : Component.translatable("item.playergps.player_locator.tooltip.line2.keep");
        tooltip.add(secondLine.withStyle(ChatFormatting.DARK_GRAY));
    }

    private void consumeItem(ServerPlayer user, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        if (!PlayerGPSConfig.consumeInCreative && user.getAbilities().instabuild) {
            return;
        }

        stack.shrink(1);
    }
}
