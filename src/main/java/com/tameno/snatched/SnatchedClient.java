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
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public class SnatchedClient implements ClientModInitializer {

    private boolean wasAttacking = false;

    private Vec3d lastLookingVector = new Vec3d(0, 0, 0);

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

                final Vec3d lookDirection = client.player.getRotationVector();
                final Vec3d lookDifference = lookDirection.subtract(lastLookingVector);
                final Vec3d velocity = client.player.getVelocity();

                PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());

                buf.writeDouble(lookDirection.x);
                buf.writeDouble(lookDirection.y);
                buf.writeDouble(lookDirection.z);

                buf.writeDouble(lookDifference.x);
                buf.writeDouble(lookDifference.y);
                buf.writeDouble(lookDifference.z);

                buf.writeDouble(velocity.getX());
                buf.writeDouble(velocity.getY());
                buf.writeDouble(velocity.getZ());

                ClientPlayNetworking.send(Snatched.ATTACK_AIR_PACKET_ID, buf);
            } else {
                wasAttacking = false;
            }
            lastLookingVector = client.player.getRotationVector();
        });
    }
}
