package me.krem.blockDisplayEditor;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.scheduler.BukkitRunnable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class CommandProcessor implements CommandExecutor {
    private final NamespacedKey toolKey;
    private final NamespacedKey blockKey;
    private final NamespacedKey lockKey;
    private final PersistentDataType<String, String> blockDataType = PersistentDataType.STRING;
    private final PersistentDataType<Byte, Boolean> isBDLocked = PersistentDataType.BOOLEAN;

    private final HashMap<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private final HashMap<UUID, Boolean> hasTools = new HashMap<>();
    private final String logo = ChatColor.DARK_GRAY + "[" + ChatColor.AQUA + "BlockDisplayEditor" + ChatColor.DARK_GRAY + "] ";
    private final Material[] heads = {Material.PLAYER_HEAD, Material.SKELETON_SKULL, Material.WITHER_SKELETON_SKULL,
                                        Material.DRAGON_HEAD, Material.CREEPER_HEAD, Material.ZOMBIE_HEAD, Material.PIGLIN_HEAD};
    private final BlockDisplayEditor plugin;

    public CommandProcessor(BlockDisplayEditor plugin) {
        this.plugin = plugin;
        this.toolKey = new NamespacedKey(plugin, "BDE_Tool");
        this.blockKey = new NamespacedKey(plugin, "BDE_Display");
        this.lockKey = new NamespacedKey(plugin, "BDE_Locked_Display");
    }

    private static PlayerProfile getProfile(String url) {
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID()); // Get a new player profile
        PlayerTextures textures = profile.getTextures();
        URL urlObject;
        try {
            urlObject = new URL(url);
        } catch (MalformedURLException exception) {
            throw new RuntimeException("Invalid URL", exception);
        }
        textures.setSkin(urlObject); // Set the skin of the player profile to the URL
        profile.setTextures(textures); // Set the textures back to the profile
        return profile;
    }

    public static URL getUrlFromBase64(String base64) throws MalformedURLException {
        String decoded = new String(Base64.getDecoder().decode(base64));
        return new URL(decoded.substring("{\"textures\":{\"SKIN\":{\"url\":\"".length(), decoded.length() - "\"}}}".length()));
    }

    private void drawParticles(Player player, Location location, double width, double height, double n, boolean isLocked) {
        Particle boundaries = Particle.COMPOSTER;
        if (isLocked) boundaries = Particle.SCRAPE;
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
        for (int y = yStep; (Math.abs(y) < Math.abs(maxY-0.5d)); y += yStep) {
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
                    boolean isLocked = interaction.getPersistentDataContainer().getOrDefault(lockKey, isBDLocked, false);
                    drawParticles(player, location, interaction.getInteractionWidth(), interaction.getInteractionHeight(), 2.0d, isLocked);
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
            case "create" -> {
                try {
                    handleCreateCommand(player, args);
                } catch (Exception e) {
                    sender.sendMessage(logo + ChatColor.DARK_RED + "You have to either enter an URL or encrypted texture. For example, the following will work :\n" +
                            ChatColor.WHITE + "- " + ChatColor.GRAY + ChatColor.ITALIC + "https://textures.minecraft.net/texture/18813764b2abc94ec3c3bc67b9147c21be850cdf996679703157f4555997ea63\n" +
                            ChatColor.WHITE + "- " + ChatColor.YELLOW + "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWZiNzFlNzllZDVlOWRiYzUwNWY0N2RlMzQ0ZWFkZDk1ODg5NzNhZGU5Y2FiNzc2M2M0YTU5ZjgyMTMwZDMifX19\n \n" +
                            logo + ChatColor.RED + "The second one is actually the Tag 'value' from 'properties' : it can be found in /give commands or in the 'for developers' section of a head on minecraft-heads.com.");
                    return true;
                }
            }
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
        player.sendMessage(ChatColor.AQUA + "/bde create <block> [URL/Base64]" + ChatColor.GRAY + ": create a block display at your location. Can be a player head with skin, but then you MUST give a valid URL : raw/normal or as Base64 (like in the vanilla way of doing). minecraft-heads.com actually gives this data in the 'for developers' section of a head.");
        player.sendMessage(ChatColor.AQUA + "/bde delete <r> " + ChatColor.GRAY + ": delete block displays within radius r (max radius is 12 blocks, default is 3).");
        player.sendMessage(ChatColor.AQUA + "/bde tools " + ChatColor.GRAY + ": get a set of editing tools.");
        player.sendMessage(ChatColor.AQUA + "/bde info " + ChatColor.GRAY + ": get the plugin version.");
        player.sendMessage("");
    }

    private void handleCreateCommand(Player player, String[] args) throws MalformedURLException {
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
            if (Arrays.asList(heads).contains(material)) {
                ItemStack itemMaterial = material.asItemType().createItemStack();
                String newID = UUID.randomUUID().toString();

                if (args.length > 2 && material.equals(Material.PLAYER_HEAD)) {
                    // set code to change itemMaterial into wanted head skin
                    if (args[2].contains("://")) { // cheap method to check that it's not Base64...
                        PlayerProfile profile = getProfile(args[2]);
                        SkullMeta meta = (SkullMeta) itemMaterial.getItemMeta();
                        meta.setOwnerProfile(profile);
                        itemMaterial.setItemMeta(meta);
                    } else if (!args[2].isEmpty() && args[2].length() < 16) {
                        //
                    } else {
                        URL urlFromBase64 = getUrlFromBase64(args[2]);
                        PlayerProfile profile = getProfile(urlFromBase64.toString());
                        SkullMeta meta = (SkullMeta) itemMaterial.getItemMeta();
                        meta.setOwnerProfile(profile);
                        itemMaterial.setItemMeta(meta);
                    }
                }

                Interaction interaction = player.getWorld().spawn(interactionLoc, Interaction.class);
                ItemDisplay itemDisplay = player.getWorld().spawn(blockLoc.add(0.5,0.0, 0.5), ItemDisplay.class);
                itemDisplay.setItemStack(itemMaterial);
                itemDisplay.getPersistentDataContainer().set(blockKey, blockDataType, newID);
                interaction.getPersistentDataContainer().set(blockKey, blockDataType, newID);
                player.sendMessage(logo + ChatColor.GREEN + "Block display created at " + ChatColor.YELLOW + blockLoc.getBlockX() + " " + blockLoc.getBlockY() + " " + blockLoc.getBlockZ());
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
                if (entity.getPersistentDataContainer().getOrDefault(lockKey, isBDLocked, false)) continue;
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
