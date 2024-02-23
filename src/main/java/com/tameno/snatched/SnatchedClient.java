package com.tameno.snatched;

import com.tameno.snatched.config.SnatcherSettings;
import com.tameno.snatched.entity.ModEntities;
import com.tameno.snatched.entity.client.HandSeatRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.network.PacketByteBuf;

import java.util.UUID;

public class SnatchedClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.HAND_SEAT, HandSeatRenderer::new);
        SnatcherSettings.loadSettings();

        ClientPlayNetworking.registerGlobalReceiver(Snatched.SNATCHER_SETTINGS_SYNC_ID,
                (MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buffer, PacketSender sender) -> {
            UUID playerUuid = buffer.readUuid();
            SnatcherSettings newSettings = new SnatcherSettings();
            newSettings.readFromBuf(buffer);
            Snatched.allSnatcherSettings.put(playerUuid, newSettings);
        });

        ClientPlayConnectionEvents.JOIN.register((ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient client) -> {
            PacketByteBuf buffer = PacketByteBufs.create();
            SnatcherSettings settings = SnatcherSettings.getLocalInstance();
            settings.writeToBuf(buffer);
            sender.sendPacket(sender.createPacket(Snatched.SNATCHER_SETTINGS_SYNC_ID, buffer));
        });
    }
}
