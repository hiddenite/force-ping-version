package eu.hiddenite;

import net.md_5.bungee.api.ServerPing;
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

    @Override
    public void onEnable() {
        loadConfiguration();

        getProxy().getPluginManager().registerListener(this, this);

        getLogger().info("Forced ping version: " + forcedVersionName +  " (" + forcedVersionProtocol + ")");
    }

    @EventHandler
    public void onProxyPingEvent(ProxyPingEvent event) {
        if (forcedVersionName != null && !forcedVersionName.isEmpty() && forcedVersionProtocol > 0) {
            ServerPing ping = event.getResponse();
            ping.setVersion(new ServerPing.Protocol(forcedVersionName, forcedVersionProtocol));
        }
    }

    private void loadConfiguration() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
