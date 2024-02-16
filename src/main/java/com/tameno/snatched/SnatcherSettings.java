package com.tameno.snatched;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.Vec3d;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class SnatcherSettings {

    private static Gson myGson = new GsonBuilder().setPrettyPrinting().create();
    public static Path SAVE_PATH = FabricLoader.getInstance().getConfigDir().resolve(Snatched.MOD_ID + "_settings.json");

    public static SnatcherSettings instance;

    public Vec3d holdPosition = new Vec3d(0.18, -0.2, 0.25);

    public boolean flipWhenUsingLeftHandAsMainHand = true;

    public static SnatcherSettings getInstance() {
        if (instance == null) {
            instance = new SnatcherSettings();
        }
        return instance;
    }

    public static void saveSettings() {
        SnatcherSettings settings = getInstance();
        try {
            if (!Files.exists(SAVE_PATH)) {
                Files.createFile(SAVE_PATH);
            }
            String settingsJson = myGson.toJson(settings);
            BufferedWriter bufferedWriter = Files.newBufferedWriter(SAVE_PATH);
            bufferedWriter.write(settingsJson);
            bufferedWriter.close();
            Snatched.LOGGER.info("saved settings " + settingsJson);
        } catch(Exception exception) {
            Snatched.LOGGER.error("Error saving snatcher (client) settings", exception);
        }
    }

    public static void loadSettings() {
        SnatcherSettings settings = getInstance();
        try {
            if (!Files.exists(SAVE_PATH)) {
                saveSettings();
                return;
            }
            BufferedReader bufferedReader = Files.newBufferedReader(SAVE_PATH);
            SnatcherSettings loadedSettings = myGson.fromJson(bufferedReader, SnatcherSettings.class);
            String settingsJson = myGson.toJson(loadedSettings);
            BufferedWriter bufferedWriter = Files.newBufferedWriter(SAVE_PATH);
            bufferedWriter.write(settingsJson);
            bufferedWriter.close();
            settings.holdPosition = loadedSettings.holdPosition;
            settings.flipWhenUsingLeftHandAsMainHand = loadedSettings.flipWhenUsingLeftHandAsMainHand;
            Snatched.LOGGER.info("settings read");
        } catch(Exception exception) {
            Snatched.LOGGER.error("Error loading snatcher (client) settings", exception);
        }
    }
}
