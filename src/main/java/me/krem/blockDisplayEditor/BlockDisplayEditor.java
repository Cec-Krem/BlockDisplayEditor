package me.krem.blockDisplayEditor;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class BlockDisplayEditor extends JavaPlugin {
    private Logger log;

    @Override
    public void onEnable() {
        log = getLogger();
        log.info("BlockDisplayEditor enabled");
        getServer().getPluginManager().registerEvents(new MainListener(this), this);
        getCommand("bde").setExecutor(new CommandProcessor(this));
    }

    @Override
    public void onDisable() {
        log.info("BlockDisplayEditor disabled");
    }

}
