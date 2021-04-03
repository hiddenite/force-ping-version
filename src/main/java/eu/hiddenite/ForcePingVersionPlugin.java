package eu.hiddenite;

import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class ForcePingVersionPlugin extends Plugin implements Listener {
    private String forcedVersionName;
    private int forcedVersionProtocol;
    private String errorMessage;

    @Override
    public void onEnable() {
        loadConfiguration();

        getProxy().getPluginManager().registerListener(this, this);

        getLogger().info("Forced ping version: " + forcedVersionName + " (" + forcedVersionProtocol + ")");
    }

    @EventHandler
    public void onProxyPingEvent(ProxyPingEvent event) {
        if (forcedVersionName != null && !forcedVersionName.isEmpty() && forcedVersionProtocol > 0) {
            ServerPing ping = event.getResponse();
            ping.setVersion(new ServerPing.Protocol(forcedVersionName, forcedVersionProtocol));

            ServerPing.PlayerInfo[] sample = getProxy().getPlayers().stream()
                    .map(player -> new ServerPing.PlayerInfo(player.getName(), player.getUniqueId()))
                    .toArray(ServerPing.PlayerInfo[]::new);

            ping.setPlayers(new ServerPing.Players(ping.getPlayers().getMax(), ping.getPlayers().getOnline(), sample));
        }
    }

    @EventHandler
    public void onProxyPingEvent(PreLoginEvent event) {
        if (forcedVersionName != null && !forcedVersionName.isEmpty() && forcedVersionProtocol > 0) {
            if (event.getConnection().getVersion() != forcedVersionProtocol) {
                getLogger().info("Login from " + event.getConnection().getSocketAddress().toString() +
                        " rejected: protocol version " + event.getConnection().getVersion() +
                        ", expected " + forcedVersionProtocol);

                String message = errorMessage.replace("{VERSION}", forcedVersionName);
                event.setCancelled(true);
                event.setCancelReason(TextComponent.fromLegacyText(message));
            }
        }
    }

    private void loadConfiguration() {
        if (!getDataFolder().exists()) {
            if (!getDataFolder().mkdir()) {
                return;
            }
        }

        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            Configuration configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));

            forcedVersionName = configuration.getString("version.name");
            forcedVersionProtocol = configuration.getInt("version.protocol");
            errorMessage = configuration.getString("error-message");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
