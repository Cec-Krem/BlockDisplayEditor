package me.krem.blockDisplayEditor;

import org.bukkit.plugin.java.JavaPlugin;

public final class BlockDisplayEditor extends JavaPlugin {

    @Override
    public void onEnable() {
        System.out.println("BlockDisplayEditor enabled");
        getServer().getPluginManager().registerEvents(new MainListener(this), this);
        getCommand("bde").setExecutor(new CommandProcessor(this));
    }

    @Override
    public void onDisable() {
        System.out.println("BlockDisplayEditor disabled");
    }

}
