package net.savagedev.guildwhitelist;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.savagedev.guildwhitelist.listeners.ConnectionListener;
import org.bukkit.plugin.java.JavaPlugin;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GuildWhitelist extends JavaPlugin {
    private static final String GUILD_API_URL = "https://api.hypixel.net/guild?name=";

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.getServer().getPluginManager().registerEvents(new ConnectionListener(this), this);
    }

    public CompletableFuture<Set<UUID>> getGuildMembers() {
        return CompletableFuture.supplyAsync(() -> {
            final Set<UUID> members = new HashSet<>();
            try {
                final HttpsURLConnection connection = (HttpsURLConnection) new URL(GUILD_API_URL + this.getConfig().getString("guild-name").replace(" ", "%20")).openConnection();
                connection.setRequestMethod("GET");
                connection.setDoInput(true);

                connection.setRequestProperty("API-Key", this.getConfig().getString("api-key"));
                connection.setRequestProperty("User-Agent", "GuildWhitelist/1.0.0-SNAPSHOT");
                connection.setRequestProperty("Content-Type", "application/json");

                try (final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    final JsonElement baseElement = new JsonParser().parse(reader);
                    if (baseElement.isJsonObject()) {
                        final JsonObject guildObject = baseElement.getAsJsonObject().get("guild").getAsJsonObject();

                        if (guildObject.isJsonNull()) {
                            return members;
                        }

                        final JsonArray membersArray = guildObject.get("members").getAsJsonArray();

                        for (JsonElement memberElement : membersArray) {
                            final String unformattedUuid = memberElement.getAsJsonObject().get("uuid").getAsString();
                            members.add(UUID.fromString(unformattedUuid
                                    .replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5")
                            ));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return members;
        });
    }
}
