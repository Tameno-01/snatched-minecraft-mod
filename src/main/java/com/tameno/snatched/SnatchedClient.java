package com.tameno.snatched;

import com.tameno.snatched.config.SnatcherSettings;
import com.tameno.snatched.entity.ModEntities;
import com.tameno.snatched.entity.client.HandSeatRenderer;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public class SnatchedClient implements ClientModInitializer {

    private boolean wasAttacking = false;

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

        ClientPlayConnectionEvents.DISCONNECT.register((ClientPlayNetworkHandler handler, MinecraftClient client) -> {
            Snatched.allSnatcherSettings.clear();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            boolean isAttacking = client.options.attackKey.isPressed();
            boolean hasNoTarget = client.crosshairTarget == null || client.crosshairTarget.getType() == net.minecraft.util.hit.HitResult.Type.MISS;
            if (isAttacking && hasNoTarget) {
                if (wasAttacking) return;
                wasAttacking = true;
                var buf = new PacketByteBuf(Unpooled.buffer());
                ClientPlayNetworking.send(Snatched.ATTACK_AIR_PACKET_ID, buf);
            } else {
                wasAttacking = false;
            }
        });

        /* This doesn't work for players, they get desynced or don't move at all. I don't know why

            ClientPlayNetworking.registerGlobalReceiver(Snatched.THROW_PLAYER_ID,
                    (client, handler, buf, responseSender) -> {
                final double velX = buf.readDouble();
                final double velY = buf.readDouble();
                final double velZ = buf.readDouble();

                client.execute(() -> {
                    ClientPlayerEntity player = client.player;
                    if (player == null) return;
                    player.dismountVehicle();
                    player.addVelocity(velX, velY, velZ);
                    player.velocityDirty = true;
                    player.sendMessage(Text.literal("x:" + velX + ", y:" + velY + ", z:" + velZ), true);
                });
            });
         */
    }
}
