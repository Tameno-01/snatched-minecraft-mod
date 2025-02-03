package com.tameno.snatched.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tameno.snatched.Snatched;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;

// Settings for every individual player
public class SnatcherSettings {

    private static Gson myGson = new GsonBuilder().setPrettyPrinting().create();
    public static Path SAVE_PATH = FabricLoader.getInstance().getConfigDir().resolve(Snatched.MOD_ID + "_settings.json");

    public static SnatcherSettings localInstance;

    public Vec3d holdPosition = new Vec3d(0.0, -0.2, 0.25);

    public boolean flipWhenUsingLeftHandAsMainHand = true;

    public static SnatcherSettings getLocalInstance() {
        if (localInstance == null) {
            localInstance = new SnatcherSettings();
        }
        return localInstance;
    }

    public static void saveSettings() {
        SnatcherSettings settings = getLocalInstance();
        try {
            if (!Files.exists(SAVE_PATH)) {
                Files.createFile(SAVE_PATH);
            }
            String settingsJson = myGson.toJson(settings);
            BufferedWriter bufferedWriter = Files.newBufferedWriter(SAVE_PATH);
            bufferedWriter.write(settingsJson);
            bufferedWriter.close();
        } catch(Exception exception) {
            Snatched.LOGGER.error("Error saving snatcher (client) settings", exception);
        }
    }

    public static void loadSettings() {
        SnatcherSettings settings = getLocalInstance();
        try {
            if (!Files.exists(SAVE_PATH)) {
                saveSettings();
                return;
            }
            BufferedReader bufferedReader = Files.newBufferedReader(SAVE_PATH);
            SnatcherSettings loadedSettings = myGson.fromJson(bufferedReader, SnatcherSettings.class);
            bufferedReader.close();
            String settingsJson = myGson.toJson(loadedSettings);
            settings.holdPosition = loadedSettings.holdPosition;
            settings.flipWhenUsingLeftHandAsMainHand = loadedSettings.flipWhenUsingLeftHandAsMainHand;
        } catch(Exception exception) {
            Snatched.LOGGER.error("Error loading snatcher (client) settings", exception);
        }
    }

    public void writeToBuf(PacketByteBuf buffer) {
        buffer.writeDouble(this.holdPosition.x);
        buffer.writeDouble(this.holdPosition.y);
        buffer.writeDouble(this.holdPosition.z);
        buffer.writeBoolean(this.flipWhenUsingLeftHandAsMainHand);
    }

    public void readFromBuf(PacketByteBuf buffer) {
        this.holdPosition = new Vec3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        this.flipWhenUsingLeftHandAsMainHand = buffer.readBoolean();
    }
}
