package me.krem.blockDisplayEditor;

import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class CommandProcessor implements CommandExecutor {
    private BlockDisplayEditor plugin;
    private final NamespacedKey key;
    private final NamespacedKey blockKey;
    private final PersistentDataType<Byte, Boolean> dataType = PersistentDataType.BOOLEAN;
    private final PersistentDataType<String, String> blockDataType = PersistentDataType.STRING;

    public CommandProcessor(BlockDisplayEditor plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "BDE_Tool");
        this.blockKey = new NamespacedKey(plugin, "BDE_Display");
    }

    HashMap<UUID, ItemStack[]> savedInv = new HashMap<>();

    public void editToolsTArrow(ItemStack item, String name, PotionType potionType) {
        PotionMeta tArrowMeta = (PotionMeta) item.getItemMeta();
        tArrowMeta.setItemName(ChatColor.RESET + name);
        tArrowMeta.setBasePotionType(potionType);
        tArrowMeta.getPersistentDataContainer().set(key, dataType, true);
        item.setItemMeta(tArrowMeta);
    }

    public void editToolsLambda(ItemStack item, String name) {
        ItemMeta toolMeta = item.getItemMeta();
        toolMeta.setItemName(ChatColor.RESET + name);
        toolMeta.getPersistentDataContainer().set(key, dataType, true);
        item.setItemMeta(toolMeta);
    }

    public void editToolsInvisible(ItemStack item) {
        ItemMeta toolMeta = item.getItemMeta();
        toolMeta.setHideTooltip(true);
        item.setItemMeta(toolMeta);
    }

    public void getTools(Inventory invThatNeedTools) {
        ItemStack moveX = new ItemStack(Material.TIPPED_ARROW);
        editToolsTArrow(moveX, "Move (X)", PotionType.HEALING);
        ItemStack moveY = new ItemStack(Material.TIPPED_ARROW);
        editToolsTArrow(moveY, "Move (Y)", PotionType.SWIFTNESS);
        ItemStack moveZ = new ItemStack(Material.TIPPED_ARROW);
        editToolsTArrow(moveZ, "Move (Z)", PotionType.LUCK);
        ItemStack rotX = new ItemStack(Material.IRON_INGOT);
        editToolsLambda(rotX, "Rotation (X)");
        ItemStack rotY = new ItemStack(Material.COPPER_INGOT);
        editToolsLambda(rotY, "Rotation (Y)");
        ItemStack rotZ = new ItemStack(Material.GOLD_INGOT);
        editToolsLambda(rotZ, "Rotation (Z)");
        ItemStack rotR = new ItemStack(Material.NETHERITE_INGOT);
        editToolsLambda(rotR, "Reset Rotation");
        ItemStack scaleX = new ItemStack(Material.BRICK);
        editToolsLambda(scaleX, "Scale (X)");
        ItemStack scaleY = new ItemStack(Material.NETHER_BRICK);
        editToolsLambda(scaleY, "Scale (Y)");
        ItemStack scaleZ = new ItemStack(Material.NETHERITE_SCRAP);
        editToolsLambda(scaleZ, "Scale (Z)");
        ItemStack delete = new ItemStack(Material.BARRIER);
        editToolsLambda(delete, "Delete");
        ItemStack brightB = new ItemStack(Material.LIGHT);
        editToolsLambda(brightB, "Brightness (Block)");
        ItemStack brightS = new ItemStack(Material.NETHER_STAR);
        editToolsLambda(brightS, "Brightness (Sky)");
        ItemStack confirmToolMode = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        editToolsInvisible(confirmToolMode);

        invThatNeedTools.setItem(0, moveX);
        invThatNeedTools.setItem(1, moveY);
        invThatNeedTools.setItem(2, moveZ);
        invThatNeedTools.setItem(3, rotX);
        invThatNeedTools.setItem(4, rotY);
        invThatNeedTools.setItem(5, rotZ);
        invThatNeedTools.setItem(8, rotR);
        invThatNeedTools.setItem(27, scaleX);
        invThatNeedTools.setItem(28, scaleY);
        invThatNeedTools.setItem(29, scaleZ);
        invThatNeedTools.setItem(30, brightB);
        invThatNeedTools.setItem(31, brightS);
        invThatNeedTools.setItem(35, delete);
        invThatNeedTools.setItem(38, confirmToolMode);
    }

    String logo = ChatColor.DARK_GRAY + "[" + ChatColor.AQUA + "BlockDisplayEditor" + ChatColor.DARK_GRAY + "]";

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                player.sendMessage("");
                player.sendMessage(logo + ChatColor.GRAY + " Available commands :");
                player.sendMessage(ChatColor.AQUA + "/bde " + ChatColor.GRAY + ": display this list");
                player.sendMessage(ChatColor.AQUA + "/bde create <block>" + ChatColor.GRAY + ": create a block display that you will edit. If you don't specify what block you want to create, the default one is a grass block.");
                player.sendMessage(ChatColor.AQUA + "/bde delete <r> " + ChatColor.GRAY + ": delete the block displays in a given radius (use /bde tools for better control, max radius is 12 and default is 3)");
                player.sendMessage(ChatColor.AQUA + "/bde tools " + ChatColor.GRAY + ": get a set of tools to edit block displays");
                player.sendMessage(ChatColor.AQUA + "/bde info " + ChatColor.GRAY + ": get the running version of the plugin");
                player.sendMessage("");
                return true;
            } else {
                switch (args[0].toLowerCase()) {
                    case "create":
                        if (!player.hasPermission("bde.create")) {
                            player.sendMessage(ChatColor.DARK_RED + "Sorry, but you don't have the permission to do that.");
                            return false;
                        } else {
                            Location playerLoc = player.getLocation();
                            int[] blockPos = {playerLoc.getBlockX(), playerLoc.getBlockY(), playerLoc.getBlockZ()};
                            Location blockLoc = new Location(player.getWorld(), blockPos[0], blockPos[1], blockPos[2]);
                            Location interLoc = new Location(player.getWorld(), blockPos[0] + 0.5f, blockPos[1], blockPos[2] + 0.5f);

                            if (args.length == 1) {
                                BlockData defaultBlock = Material.GRASS_BLOCK.createBlockData();
                                Interaction i = player.getWorld().spawn(interLoc, Interaction.class);
                                BlockDisplay d = player.getWorld().spawn(blockLoc, BlockDisplay.class);
                                d.setBlock(defaultBlock);

                                String newID = UUID.randomUUID().toString();
                                d.getPersistentDataContainer().set(blockKey, blockDataType, newID);
                                i.getPersistentDataContainer().set(blockKey, blockDataType, newID);
                                sender.sendMessage(ChatColor.GREEN + "Successfully created new block display at " + ChatColor.YELLOW + blockPos[0] + " " + blockPos[1] + " " + blockPos[2]);
                                return true;
                            } else {
                                try {
                                    args[1].toLowerCase();
                                    if (Material.matchMaterial(args[1]).isBlock()) {
                                        BlockData specialBlock = Material.matchMaterial(args[1]).createBlockData();
                                        Interaction i = player.getWorld().spawn(interLoc, Interaction.class);
                                        BlockDisplay d = player.getWorld().spawn(blockLoc, BlockDisplay.class);
                                        d.setBlock(specialBlock);

                                        String newID = UUID.randomUUID().toString();
                                        d.getPersistentDataContainer().set(blockKey, blockDataType, newID);
                                        i.getPersistentDataContainer().set(blockKey, blockDataType, newID);
                                        sender.sendMessage(ChatColor.GREEN + "Successfully created new block display at " + ChatColor.YELLOW + blockPos[0] + " " + blockPos[1] + " " + blockPos[2]);
                                        return true;
                                    }
                                } catch (Exception e) {
                                    sender.sendMessage(ChatColor.DARK_RED + "It seems that the block you provided is invalid.");
                                    return true;
                                }
                            }
                        }

                    case "delete":
                        if (!player.hasPermission("bde.delete")) {
                            player.sendMessage(ChatColor.DARK_RED + "Sorry, but you don't have the permission to do that.");
                            return false;
                        } else {
                            int a = 0;

                            if (args.length == 1) {
                                List<Entity> near = player.getNearbyEntities(3.0d, 3.0d, 3.0d);
                                for (Entity entity : near) {
                                    if (entity instanceof BlockDisplay || entity instanceof Interaction) {
                                        entity.remove();
                                        a++;
                                    }
                                }

                                if (a == 0) {
                                    player.sendMessage(ChatColor.DARK_RED + "No block display to delete (default radius = 3.0 blocks).");
                                    return true;
                                } else if (a % 2 != 0) {
                                    player.sendMessage(ChatColor.BLUE + "Some block displays may not have been deleted.");
                                    return true;
                                } else {
                                    player.sendMessage(ChatColor.GREEN + "Successfully deleted block display.");
                                    return true;
                                }
                            }

                            try {
                                if (Double.parseDouble(args[1]) > 12.0) {
                                    player.sendMessage(ChatColor.DARK_RED + "Sorry, but the radius must be less or equal than 12.");
                                    return true;
                                }
                                if (Double.parseDouble(args[1]) <= 0) {
                                    player.sendMessage(ChatColor.DARK_RED + "Sorry, but the radius must be over 0.");
                                    return true;
                                }
                                double radi = (Double.parseDouble(args[1]) <= 12.0 && Double.parseDouble(args[1]) > 0) ? Double.parseDouble(args[1]) : 3.0d;
                                List<Entity> near = player.getNearbyEntities(radi, radi, radi);
                                for (Entity entity : near) {
                                    if (entity instanceof BlockDisplay || entity instanceof Interaction) {
                                        entity.remove();
                                        a++;
                                    }
                                }

                                if (a == 0) {
                                    player.sendMessage(ChatColor.DARK_RED + "No block display to delete (radius = " + radi + " blocks).");
                                    return true;
                                } else if (a % 2 != 0) {
                                    player.sendMessage(ChatColor.BLUE + "Some block displays may not have been deleted.");
                                    return true;
                                }
                            } catch (Exception e) {
                                player.sendMessage(ChatColor.DARK_RED + "An error occurred. Did you use a valid radius ?");
                                return true;
                            }

                            player.sendMessage(ChatColor.GREEN + "Successfully deleted block display.");
                            return true;
                        }

                    case "tools":
                         /* Save inventory and give tools. If the player execute
                            the command again, give back the saved inventory. */

                        if (!player.hasPermission("bde.tools")) {
                            player.sendMessage(ChatColor.DARK_RED + "Sorry, but you don't have the permission to do that.");
                            return false;
                        } else {
                            UUID pUID = player.getUniqueId();
                            ItemStack[] pInv = player.getInventory().getContents();
                            try {
                                if (savedInv.get(pUID).length != 0 || savedInv.isEmpty()) {
                                    player.getInventory().setContents(savedInv.get(pUID));
                                    player.updateInventory();
                                    savedInv.remove(pUID);
                                    return true;
                                }
                            } catch (Exception ignored) {
                                // There is nothing we will do
                            }
                            player.sendMessage(ChatColor.AQUA + "Press F (or your swap hands key bind if different) to swap between additional tools.");
                            savedInv.putIfAbsent(pUID, pInv);
                            player.getInventory().clear();
                            getTools(player.getInventory());
                            return true;
                        }

                    case "info":
                        if (!player.hasPermission("bde.info")) {
                            player.sendMessage(ChatColor.DARK_RED + "Sorry, but you don't have the permission to do that.");
                            return false;
                        } else {
                            sender.sendMessage(logo + ChatColor.GREEN + " running on version " + ChatColor.YELLOW + plugin.getDescription().getVersion() + ChatColor.GREEN + " for Bukkit " + ChatColor.YELLOW + plugin.getDescription().getAPIVersion());
                            sender.sendMessage(ChatColor.GRAY + "Author : " + ChatColor.AQUA + "Krem");
                            return true;
                        }

                    default:
                        player.sendMessage(ChatColor.DARK_RED + "Unknown command. Do /bde to get a list of the commands");
                        return true;
                }
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Sorry, only players can run BDE commands.");
            sender.sendMessage(logo + ChatColor.GREEN + " running on version " + ChatColor.YELLOW + plugin.getDescription().getVersion() + ChatColor.GREEN + " for Bukkit " + ChatColor.YELLOW + plugin.getDescription().getAPIVersion());
            sender.sendMessage(ChatColor.GRAY + "Author : " + ChatColor.AQUA + "Krem");
            return true;
        }
    }
}
