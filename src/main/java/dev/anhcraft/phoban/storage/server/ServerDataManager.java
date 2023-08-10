package dev.anhcraft.phoban.storage.server;

import dev.anhcraft.vanhen.VanHen;
import dev.anhcraft.vanhen.util.CompressUtils;
import dev.anhcraft.vanhen.util.ConfigHelper;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

public class ServerDataManager {
    private final VanHen plugin;
    private final File file;
    private ServerData serverData;

    public ServerDataManager(VanHen plugin) {
        this.plugin = plugin;
        File folder = new File(plugin.getDataFolder(), "data");
        folder.mkdir();
        file = new File(folder, "server.gz");

        loadData();
    }

    public ServerData getData() {
        return serverData;
    }

    private void loadData() {
        if (file.exists()) {
            YamlConfiguration conf = null;
            try {
                conf = YamlConfiguration.loadConfiguration(new StringReader(CompressUtils.readAndDecompressString(file)));
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (conf == null)
                serverData = new ServerData();

            serverData = ConfigHelper.load(ServerData.class, conf);
        } else {
            serverData = new ServerData();
        }

        plugin.debug("Server data loaded!");
    }

    private void saveDataIfDirty() {
        if (serverData.dirty.compareAndSet(true, false)) {
            plugin.debug("Saving server data...");
            YamlConfiguration conf = new YamlConfiguration();
            ConfigHelper.save(ServerData.class, conf, serverData);
            try {
                CompressUtils.compressAndWriteString(conf.saveToString(), file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void reload() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::saveDataIfDirty, 20, 200);
    }

    public void terminate() {
        serverData.dirty.set(true);
        saveDataIfDirty();
    }
}
