package net.savagedev.guildwhitelist.listeners;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.md_5.bungee.api.ChatColor;
import net.savagedev.guildwhitelist.GuildWhitelist;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ConnectionListener implements Listener {
    private final Cache<UUID, AsyncPlayerPreLoginEvent.Result> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .maximumSize(500)
            .build();

    private final GuildWhitelist guildWhitelist;

    public ConnectionListener(GuildWhitelist guildWhitelist) {
        this.guildWhitelist = guildWhitelist;
    }

    @EventHandler
    public void on(AsyncPlayerPreLoginEvent event) {
        if (this.guildWhitelist.getConfig().getStringList("whitelist").contains(event.getUniqueId().toString())) {
            return;
        }

        try {
            event.setLoginResult(this.cache.get(event.getUniqueId(), new LocalResultLoader(event.getUniqueId())));
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        event.setKickMessage(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(this.guildWhitelist.getConfig().getString("kick-message"))
        ));
    }

    private class LocalResultLoader implements Callable<AsyncPlayerPreLoginEvent.Result> {
        private final UUID uuid;

        public LocalResultLoader(UUID uuid) {
            this.uuid = uuid;
        }

        @Override
        public AsyncPlayerPreLoginEvent.Result call() {
            final Set<UUID> members = ConnectionListener.this.guildWhitelist.getGuildMembers().join();
            if (members.contains(this.uuid)) {
                return AsyncPlayerPreLoginEvent.Result.ALLOWED;
            }
            return AsyncPlayerPreLoginEvent.Result.KICK_OTHER;
        }
    }
}
