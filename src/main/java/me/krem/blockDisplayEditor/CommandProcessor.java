package me.krem.blockDisplayEditor;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class CommandProcessor implements CommandExecutor {
    private final NamespacedKey toolKey;
    private final NamespacedKey blockKey;
    private final PersistentDataType<String, String> blockDataType = PersistentDataType.STRING;
    private final HashMap<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private final HashMap<UUID, Boolean> hasTools = new HashMap<>();
    private final String logo = ChatColor.DARK_GRAY + "[" + ChatColor.AQUA + "BlockDisplayEditor" + ChatColor.DARK_GRAY + "] ";
    private final BlockDisplayEditor plugin;
    private final Particle boundaries = Particle.COMPOSTER;

    public CommandProcessor(BlockDisplayEditor plugin) {
        this.plugin = plugin;
        this.toolKey = new NamespacedKey(plugin, "BDE_Tool");
        this.blockKey = new NamespacedKey(plugin, "BDE_Display");
    }

    private void drawParticles(Player player, Location location, double width, double height, double n) {
        double halfWidth = width / 2;
        int yStep = height > 0 ? 1 : -1;
        double maxXZ = width * n;
        double maxY = Math.abs(height * n);
        Location origin = location.clone().add(-(halfWidth), 0.0d, -(halfWidth));
        for (int xz = 0; xz <= maxXZ; xz++) {
            player.spawnParticle(boundaries, origin.clone().add(xz / n, 0.0d, 0.0d), 1, 0.0d, 0.0d, 0.0d, 0.0d);
            player.spawnParticle(boundaries, origin.clone().add(xz / n, 0.0d, width), 1, 0.0d, 0.0d, 0.0d, 0.0d);
            player.spawnParticle(boundaries, origin.clone().add(xz / n, height, 0.0d), 1, 0.0d, 0.0d, 0.0d, 0.0d);
            player.spawnParticle(boundaries, origin.clone().add(xz / n, height, width), 1, 0.0d, 0.0d, 0.0d, 0.0d);

            player.spawnParticle(boundaries, origin.clone().add(0.0d, 0.0d, xz / n), 1, 0.0d, 0.0d, 0.0d, 0.0d);
            player.spawnParticle(boundaries, origin.clone().add(width, 0.0d, xz / n), 1, 0.0d, 0.0d, 0.0d, 0.0d);
            player.spawnParticle(boundaries, origin.clone().add(0.0d, height, xz / n), 1, 0.0d, 0.0d, 0.0d, 0.0d);
            player.spawnParticle(boundaries, origin.clone().add(width, height, xz / n), 1, 0.0d, 0.0d, 0.0d, 0.0d);
        }
        for (int y = 0; (Math.abs(y) < maxY); y += yStep) {
            player.spawnParticle(boundaries, origin.clone().add(0.0d, y / n, 0.0d), 1, 0.0d, 0.0d, 0.0d, 0.0d);
            player.spawnParticle(boundaries, origin.clone().add(0.0d, y / n, width), 1, 0.0d, 0.0d, 0.0d, 0.0d);
            player.spawnParticle(boundaries, origin.clone().add(width, y / n, 0.0d), 1, 0.0d, 0.0d, 0.0d, 0.0d);
            player.spawnParticle(boundaries, origin.clone().add(width, y / n, width), 1, 0.0d, 0.0d, 0.0d, 0.0d);
        }
    }

    private void giveToolsToPlayer(Inventory inventory) {
        for (ToolType tool : ToolType.values()) {
            inventory.setItem(tool.getSlot(), tool.createItem(toolKey));
        }
    }

    private void restorePlayerInventory(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (savedInventories.containsKey(playerUUID)) {
            player.getInventory().setContents(savedInventories.get(playerUUID));
            player.updateInventory();
            savedInventories.remove(playerUUID);
            hasTools.put(playerUUID, false);
        }
    }

    private void startParticleTask(Player player) {
        UUID playerUUID = player.getUniqueId();
        new BukkitRunnable() {
            public void run() {
                if (!hasTools.getOrDefault(playerUUID, false) || !player.hasPermission("bde.tools")) {
                    cancel();
                    return;
                }
                List<Entity> nearbyInteractions = player.getNearbyEntities(12.0d, 12.0d, 12.0d).stream()
                        .filter(e -> e instanceof Interaction)
                        .limit(150)
                        .toList();

                for (Entity entity : nearbyInteractions) {
                    Interaction interaction = (Interaction) entity;
                    Location location = interaction.getLocation();
                    drawParticles(player, location, interaction.getInteractionWidth(), interaction.getInteractionHeight(), 2.0d);
                }
            }
        }.runTaskTimer(plugin, 0, 5);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(logo + ChatColor.RED + "Sorry, but only players can run BDE commands.");
            handleInfoCommand(sender);
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            displayHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreateCommand(player, args);
            case "delete" -> handleDeleteCommand(player, args);
            case "tools" -> handleToolsCommand(player);
            case "info" -> handleInfoCommand(sender);
            default -> player.sendMessage(logo + ChatColor.DARK_RED + "Unknown command. Do /bde to get the list of commands.");
        }
        return true;
    }

    private void displayHelp(Player player) {
        player.sendMessage("");
        player.sendMessage(logo + ChatColor.GRAY + "Available commands :");
        player.sendMessage(ChatColor.AQUA + "/bde " + ChatColor.GRAY + ": display this list.");
        player.sendMessage(ChatColor.AQUA + "/bde create <block>" + ChatColor.GRAY + ": create a block display at your location.");
        player.sendMessage(ChatColor.AQUA + "/bde delete <r> " + ChatColor.GRAY + ": delete block displays within radius r (max radius is 12 blocks, default is 3).");
        player.sendMessage(ChatColor.AQUA + "/bde tools " + ChatColor.GRAY + ": get a set of editing tools.");
        player.sendMessage(ChatColor.AQUA + "/bde info " + ChatColor.GRAY + ": get the plugin version.");
        player.sendMessage("");
    }

    private void handleCreateCommand(Player player, String[] args) {
        if (!player.hasPermission("bde.create")) {
            player.sendMessage(logo + ChatColor.DARK_RED + "Sorry, but you don't have permission to do that.");
            return;
        }

        Location playerLoc = player.getLocation();
        Location blockLoc = playerLoc.getBlock().getLocation();
        Location interactionLoc = blockLoc.clone().add(0.5, 0.0, 0.5);
        Material material = Material.GRASS_BLOCK;

        if (args.length > 1) {
            material = Material.matchMaterial(args[1]);
            if (material == null || !material.isBlock()) {
                player.sendMessage(logo + ChatColor.DARK_RED + "Invalid block.");
                return;
            }
        }

        String newID = UUID.randomUUID().toString();
        Interaction interaction = player.getWorld().spawn(interactionLoc, Interaction.class);
        BlockDisplay blockDisplay = player.getWorld().spawn(blockLoc, BlockDisplay.class);
        blockDisplay.setBlock(material.createBlockData());
        blockDisplay.getPersistentDataContainer().set(blockKey, blockDataType, newID);
        interaction.getPersistentDataContainer().set(blockKey, blockDataType, newID);
        player.sendMessage(logo + ChatColor.GREEN + "Block display created at " + ChatColor.YELLOW + blockLoc.getBlockX() + " " + blockLoc.getBlockY() + " " + blockLoc.getBlockZ());
    }

    private void handleDeleteCommand(Player player, String[] args) {
        if (!player.hasPermission("bde.delete")) {
            player.sendMessage(logo + ChatColor.DARK_RED + "Sorry, but you don't have permission to do that.");
            return;
        }

        double radius = 3.0;
        if (args.length > 1) {
            try {
                radius = Double.parseDouble(args[1]);
                if (radius > 12.0 || radius <= 0) {
                    player.sendMessage(logo + ChatColor.DARK_RED + "Radius must be between 0 and 12.");
                    return;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(logo + ChatColor.DARK_RED + "Invalid radius.");
                return;
            }
        }

        List<Entity> nearbyEntities = player.getNearbyEntities(radius, radius, radius);
        boolean deleted = false;
        for (Entity entity : nearbyEntities) {
            if (entity instanceof BlockDisplay || entity instanceof Interaction) {
                entity.remove();
                deleted = true;
            }
        }

        if (deleted) {
            player.sendMessage(logo + ChatColor.GREEN + "Deleted block displays.");
        } else {
            player.sendMessage(logo + ChatColor.RED + "No block display found.");
        }
    }

    private void handleToolsCommand(Player player) {
        if (!player.hasPermission("bde.tools")) {
            player.sendMessage(logo + ChatColor.DARK_RED + "Sorry, but you don't have permission to do that.");
            return;
        }

        UUID playerUUID = player.getUniqueId();
        if (hasTools.getOrDefault(playerUUID, false)) {
            restorePlayerInventory(player);
            return;
        }

        savedInventories.put(playerUUID, player.getInventory().getContents());
        hasTools.put(playerUUID, true);
        player.getInventory().clear();
        giveToolsToPlayer(player.getInventory());
        startParticleTask(player);
        player.sendMessage(ChatColor.AQUA + "Press F (or your swap hands key bind if different) to swap between additional tools.");
    }

    private void handleInfoCommand(CommandSender sender) {
        sender.sendMessage(logo + ChatColor.GREEN + "Author : " + ChatColor.YELLOW
                + "Krem" + ChatColor.DARK_GRAY + " (" + ChatColor.GRAY + ChatColor.ITALIC
                + "https://github.com/Cec-Krem" + ChatColor.DARK_GRAY + ")");
        sender.sendMessage(logo + ChatColor.GREEN + "Running on version " + ChatColor.YELLOW + plugin.getDescription().getVersion());
    }
}
