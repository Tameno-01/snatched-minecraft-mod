package com.tameno.snatched.config;

import com.google.gson.*;
import com.tameno.snatched.Snatched;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

// Custom adapter to serialize and de-serialize Mojang's Vectors without obfuscation
class Vec3dAdapter implements JsonSerializer<Vec3d>, JsonDeserializer<Vec3d> {
    @Override
    public JsonElement serialize(Vec3d src, Type typeOfSrc, JsonSerializationContext context) {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("sidewaysOffset", src.x);
        jsonObject.addProperty("verticalOffset", src.y);
        jsonObject.addProperty("distanceOffset", src.z);
        return jsonObject;
    }

    @Override
    public Vec3d deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        final JsonObject jsonObject = json.getAsJsonObject();
        return new Vec3d(
            jsonObject.get("sidewaysOffset").getAsDouble(),
            jsonObject.get("verticalOffset").getAsDouble(),
            jsonObject.get("distanceOffset").getAsDouble()
        );
    }
}

// Settings for every individual player
public class SnatcherSettings {

    private static final Gson myGson = (
        new GsonBuilder()
        .registerTypeAdapter(Vec3d.class, new Vec3dAdapter())
        .setPrettyPrinting()
        .create()
    );
    public static Path SAVE_PATH = FabricLoader.getInstance().getConfigDir().resolve(Snatched.MOD_ID + "_settings.json");

    public static SnatcherSettings localInstance;

    public final int CONFIG_VERSION = 1;

    public boolean canBeSnatched = true;

    public boolean flipWhenUsingLeftHandAsMainHand = true;

    public Vec3d holdPosition = new Vec3d(0.0, -0.015, 0.45);

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
            JsonObject jsonObject = JsonParser.parseReader(bufferedReader).getAsJsonObject();
            bufferedReader.close();

            // Check the config version to ensure we match
            if (!settings.isVersionValid(jsonObject)) {
                // If version is mismatched, make new settings
                // TODO: Implement version bump function to bring old configs to new versions
                saveSettings();
                return;
            }
            SnatcherSettings loadedSettings = myGson.fromJson(jsonObject, SnatcherSettings.class);
            String settingsJson = myGson.toJson(loadedSettings);
            settings.holdPosition = loadedSettings.holdPosition;
            settings.flipWhenUsingLeftHandAsMainHand = loadedSettings.flipWhenUsingLeftHandAsMainHand;
            settings.canBeSnatched = loadedSettings.canBeSnatched;
        } catch(Exception exception) {
            Snatched.LOGGER.error("Error loading snatcher (client) settings", exception);
        }
    }

    // Handles when the CONFIG_VERSION is mismatched in the config file
    // This DOES NOT handle when the field is missing entirely, that is handled in loadSettings
    private boolean isVersionValid(JsonObject jsonObject) {
        if (!jsonObject.has("CONFIG_VERSION")) {
            Snatched.LOGGER.info("User Config does not have a Version present");
            return false;
        }
        int suppliedVersion = jsonObject.get("CONFIG_VERSION").getAsInt();
        if (suppliedVersion < CONFIG_VERSION) {
            Snatched.LOGGER.info(
                "User Config Version is outdated. Supplied Version: {}, Current Version: {}",
                suppliedVersion,
                CONFIG_VERSION
            );
            return false;
        } else if (suppliedVersion > CONFIG_VERSION) {
            Snatched.LOGGER.info(
                "User Config Version is too new. Supplied Version: {}, Current Version: {}",
                suppliedVersion,
                CONFIG_VERSION
            );
            return false;
        } else {
            return true;
        }
    }

    public void writeToBuf(PacketByteBuf buffer) {
        buffer.writeBoolean(this.canBeSnatched);
        buffer.writeBoolean(this.flipWhenUsingLeftHandAsMainHand);
        buffer.writeDouble(this.holdPosition.x);
        buffer.writeDouble(this.holdPosition.y);
        buffer.writeDouble(this.holdPosition.z);
    }

    public void readFromBuf(PacketByteBuf buffer) {
        this.canBeSnatched = buffer.readBoolean();
        this.flipWhenUsingLeftHandAsMainHand = buffer.readBoolean();
        this.holdPosition = new Vec3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
}
