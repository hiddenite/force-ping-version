package eu.hiddenite;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

@Plugin(id = "force-ping-version", name = "ForcePingVersion", version = "2.0.0", authors = {"Hiddenite"})
public class ForcePingVersionPlugin {
    private final Logger logger;

    private Configuration config;

    @ConfigSerializable
    public static class Configuration {
        public Version version;
        public String errorMessage;

        @ConfigSerializable
        public static class Version {
            public String name;
            public int protocol;
        }
    }

    @Inject
    public ForcePingVersionPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.logger = logger;

        if (!loadConfiguration(dataDirectory.toFile())) {
            logger.warning("Failed to load the configuration: the plugin won't do anything.");
        }
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        if (config == null) {
            return;
        }

        int protocol = event.getConnection().getProtocolVersion().getProtocol();
        if (protocol == config.version.protocol) {
            return;
        }

        logger.info("Rejected login from " + event.getUsername() + ", protocol version " + protocol + ", expected " + config.version.protocol);

        String message = config.errorMessage.replace("{VERSION}", config.version.name);
        event.setResult(PreLoginEvent.PreLoginComponentResult.denied(MiniMessage.miniMessage().deserialize(message)));
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        if (config == null) {
            return;
        }

        var version = new ServerPing.Version(config.version.protocol, config.version.name);
        event.setPing(event.getPing().asBuilder().version(version).build());
    }

    public boolean loadConfiguration(File dataDirectory) {
        if (!dataDirectory.exists()) {
            if (!dataDirectory.mkdir()) {
                logger.warning("Could not create the configuration folder.");
                return false;
            }
        }

        File file = new File(dataDirectory, "config.yml");
        if (!file.exists()) {
            logger.warning("No configuration file found, creating a default one.");

            try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException exception) {
                exception.printStackTrace();
                return false;
            }
        }

        YamlConfigurationLoader reader = YamlConfigurationLoader.builder().path(dataDirectory.toPath().resolve("config.yml")).build();

        try {
            config = reader.load().get(Configuration.class);
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }

        return true;
    }
}
