package com.main.fast_irons_spellbooks_addition.network;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class FastNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            FastIronsSpellbooksAddition.id("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.messageBuilder(SyncHeroElementPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncHeroElementPacket::toBytes)
                .decoder(SyncHeroElementPacket::new)
                .consumerMainThread(SyncHeroElementPacket::handle)
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, SyncHeroElementPacket message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToPlayersTrackingEntityAndSelf(Entity entity, SyncHeroElementPacket message) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), message);
        if (entity instanceof ServerPlayer serverPlayer) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), message);
        }
    }
}