package me.krem.blockDisplayEditor;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;

import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.List;

public class MainListener implements Listener {
    private final NamespacedKey toolKey;
    private final NamespacedKey blockKey;
    private final PersistentDataType<Byte, Boolean> dataType = PersistentDataType.BOOLEAN;
    private final PersistentDataType<String, String> blockDataType = PersistentDataType.STRING;
    private final float deg = (float) Math.PI / 180;
    private final float onePixel = 0.0625f;
    HashMap<UUID, Boolean> isRayDragging = new HashMap<>();
    private BlockDisplayEditor plugin;

    public MainListener(BlockDisplayEditor plugin) {
        this.plugin = plugin;
        this.toolKey = new NamespacedKey(plugin, "BDE_Tool");
        this.blockKey = new NamespacedKey(plugin, "BDE_Display");
    }

    public void shuffleInv(Inventory pInstantInventory, ItemStack[] pInvPS) {
        for (int i = 0; i <= 8; i++) {
            pInstantInventory.setItem(i, pInvPS[i + 27]);
        }
        for (int i = 18; i <= 26; i++) {
            pInstantInventory.setItem(i, pInvPS[i - 18]);
        }
        for (int i = 27; i <= 35; i++) {
            pInstantInventory.setItem(i, pInvPS[i - 9]);
        }
    }

    public Transformation rotQuaternionX(float angle, BlockDisplay blockDisplay) {
        Quaternionf leftRot = blockDisplay.getTransformation().getLeftRotation().rotateX(angle);
        Quaternionf rightRot = blockDisplay.getTransformation().getRightRotation().rotateX(angle);
        Vector3f scale = blockDisplay.getTransformation().getScale();
        return new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), leftRot, scale, rightRot);
    }

    public Transformation rotQuaternionY(float angle, BlockDisplay blockDisplay) {
        Quaternionf leftRot = blockDisplay.getTransformation().getLeftRotation().rotateY(angle);
        Quaternionf rightRot = blockDisplay.getTransformation().getRightRotation().rotateY(angle);
        Vector3f scale = blockDisplay.getTransformation().getScale();
        return new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), leftRot, scale, rightRot);
    }

    public Transformation rotQuaternionZ(float angle, BlockDisplay blockDisplay) {
        Quaternionf leftRot = blockDisplay.getTransformation().getLeftRotation().rotateZ(angle);
        Quaternionf rightRot = blockDisplay.getTransformation().getRightRotation().rotateZ(angle);
        Vector3f scale = blockDisplay.getTransformation().getScale();
        return new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), leftRot, scale, rightRot);
    }

    public Transformation resetRotQuaternion(BlockDisplay blockDisplay) {
        return new Transformation(new Vector3f(0.0f, 0.0f, 0.0f),
                new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f),
                blockDisplay.getTransformation().getScale(),
                new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f));
    }

    public Transformation scaleBlock(float scalarX, float scalarY, float scalarZ, BlockDisplay blockDisplay) {
        Quaternionf leftRot = blockDisplay.getTransformation().getLeftRotation();
        Quaternionf rightRot = blockDisplay.getTransformation().getRightRotation();
        float[] scale = {blockDisplay.getTransformation().getScale().x, blockDisplay.getTransformation().getScale().y, blockDisplay.getTransformation().getScale().z};
        return new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), leftRot, new Vector3f(scale[0] + scalarX, scale[1] + scalarY, scale[2] + scalarZ), rightRot);
    }

    public void moveOperation(List<Entity> blockToMove, double x, double y, double z, Player player, String blockDisplayID, boolean doublePrecision) {
        if (doublePrecision) {
            x /= 2;
            y /= 2;
            z /= 2;
        }
        for (Entity entity : blockToMove) {
            if ((entity instanceof Interaction || entity instanceof BlockDisplay)
                    && entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID)) {
                if (player.isSneaking()) {
                    entity.teleport(entity.getLocation().add(-x, -y, -z));
                } else {
                    entity.teleport(entity.getLocation().add(x, y, z));
                }
            }
        }
    }

    public void shrinkOperation(List<Entity> blockToMove, Player player, String blockDisplayID) {
        for (Entity entity : blockToMove) {
            if (entity instanceof Interaction it
                    && entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID)) {
                float width = it.getInteractionWidth();
                if (!player.isSneaking() && width > onePixel) {
                    it.setInteractionWidth(width - onePixel);
                    it.teleport(it.getLocation().clone().add((double) -onePixel/2,0.0f,(double) -onePixel/2));
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(
                            ChatColor.RED + "Set interaction width to " + it.getInteractionWidth()));
                } else if (player.isSneaking() && width < 2.0f) {
                    it.setInteractionWidth(width + onePixel);
                    it.teleport(it.getLocation().clone().add((double) onePixel/2,0.0f,(double) onePixel/2));
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(
                            ChatColor.GREEN + "Set interaction width to " + it.getInteractionWidth()));
                } else return;
            }
        }
    }

    public void brightnessOperation(List<Entity> blockToTranslate, String type, Player player, String blockDisplayID) {
        for (Entity entity : blockToTranslate) {
            if (entity instanceof BlockDisplay bd && (entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID))) {
                if (bd.getBrightness() == null) {
                    bd.setBrightness(new Display.Brightness(0, 0));
                }
                int[] bdBrightness = {bd.getBrightness().getBlockLight(), bd.getBrightness().getSkyLight()};
                if (player.isSneaking() && type.equals("block") && bdBrightness[0] > 0) {
                    bd.setBrightness(new Display.Brightness(bdBrightness[0] - 1, bdBrightness[1]));
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.RED + "Set block brightness to " + bd.getBrightness().getBlockLight()));
                } else if (player.isSneaking() && type.equals("sky") && bdBrightness[1] > 0) {
                    bd.setBrightness(new Display.Brightness(bdBrightness[0], bdBrightness[1] - 1));
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.RED + "Set sky brightness to " + bd.getBrightness().getSkyLight()));
                } else if (!player.isSneaking() && type.equals("block") && bdBrightness[0] < 15) {
                    bd.setBrightness(new Display.Brightness(bdBrightness[0] + 1, bdBrightness[1]));
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.GREEN + "Set block brightness to " + bd.getBrightness().getBlockLight()));
                } else if (!player.isSneaking() && type.equals("sky") && bdBrightness[1] < 15) {
                    bd.setBrightness(new Display.Brightness(bdBrightness[0], bdBrightness[1] + 1));
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.GREEN + "Set sky brightness to " + bd.getBrightness().getSkyLight()));
                }
            }
        }
    }

    public void rotateOperation(List<Entity> blockToMove, String direction, Player player, String blockDisplayID, float angle) {
        for (Entity entity : blockToMove) {
            if (entity instanceof BlockDisplay bd
                    && entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID)) {
                if (player.isSneaking()) {
                    switch (direction) {
                        case "x":
                            bd.setTransformation(rotQuaternionX(-angle, bd));
                            break;
                        case "y":
                            bd.setTransformation(rotQuaternionY(-angle, bd));
                            break;
                        case "z":
                            bd.setTransformation(rotQuaternionZ(-angle, bd));
                            break;
                    }
                } else {
                    switch (direction) {
                        case "x":
                            bd.setTransformation(rotQuaternionX(angle, bd));
                            break;
                        case "y":
                            bd.setTransformation(rotQuaternionY(angle, bd));
                            break;
                        case "z":
                            bd.setTransformation(rotQuaternionZ(angle, bd));
                            break;
                    }
                }
            }
        }
    }

    public void resetRotation(List<Entity> blockToMove, String blockDisplayID) {
        for (Entity entity : blockToMove) {
            if (entity instanceof BlockDisplay bd
                    && bd.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID))
                bd.setTransformation(resetRotQuaternion(bd));
        }
    }

    public void deleteOperation(List<Entity> blockToDelete, String blockDisplayID) {
        for (Entity entity : blockToDelete) {
            if (entity instanceof BlockDisplay bd
                    && bd.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID))
                bd.remove();
            if (entity instanceof Interaction it
                    && it.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID))
                it.remove();
        }
    }

    public void cloneOperation(List<Entity> blockToClone, Player player, String blockDisplayID) {
        String newID = UUID.randomUUID().toString();
        for (Entity entity : blockToClone) {
            if (entity instanceof BlockDisplay bd
                    && bd.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID)) {
                Location bdLoc = bd.getLocation().clone().add(0.0d, bd.getTransformation().getScale().y, 0.0d);
                BlockDisplay d = player.getWorld().spawn(bdLoc, BlockDisplay.class);
                d.getPersistentDataContainer().set(blockKey, blockDataType, newID);
                d.setBlock(bd.getBlock());
                d.setTransformation(bd.getTransformation());
                d.setBrightness(bd.getBrightness());
            }
            if (entity instanceof Interaction it
                    && it.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID)) {
                Location itLoc = it.getLocation().clone().add(0.0d, it.getInteractionHeight(), 0.0d);
                Interaction i = player.getWorld().spawn(itLoc, Interaction.class);
                i.getPersistentDataContainer().set(blockKey, blockDataType, newID);
                i.setInteractionHeight(it.getInteractionHeight());
                i.setInteractionWidth(it.getInteractionWidth());
            }
        }
    }

    public void scaleOperation(List<Entity> blockToMove, String direction, Player player, String blockDisplayID, float scalar) {
        for (Entity entity : blockToMove) {
            if (entity instanceof BlockDisplay bd
                    && entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID)) {
                Vector3f bdscale = bd.getTransformation().getScale();
                if (player.isSneaking()) {
                    switch(direction) {
                        case "x":
                            if (bdscale.x <= -5.0f) return;
                            bd.setTransformation(scaleBlock(-scalar, 0.0f, 0.0f, bd));
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.RED + "X Scale : " + bd.getTransformation().getScale().x));
                            break;
                        case "y":
                            if (bdscale.y <= -5.0f) return;
                            bd.setTransformation(scaleBlock(0.0f, -scalar, 0.0f, bd));
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.RED + "Y Scale : " + bd.getTransformation().getScale().y));
                            break;
                        case "z":
                            if (bdscale.z <= -5.0f) return;
                            bd.setTransformation(scaleBlock(0.0f, 0.0f, -scalar, bd));
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.RED + "Z Scale : " + bd.getTransformation().getScale().y));
                            break;
                    }
                } else {
                    switch(direction) {
                        case "x":
                            if (bdscale.x >= 5.0f) return;
                            bd.setTransformation(scaleBlock(scalar, 0.0f, 0.0f, bd));
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.GREEN + "X Scale : " + bd.getTransformation().getScale().x));
                            break;
                        case "y":
                            if (bdscale.y >= 5.0f) return;
                            bd.setTransformation(scaleBlock(0.0f, scalar, 0.0f, bd));
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.GREEN + "Y Scale : " + bd.getTransformation().getScale().y));
                            break;
                        case "z":
                            if (bdscale.z >= 5.0f) return;
                            bd.setTransformation(scaleBlock(0.0f, 0.0f, scalar, bd));
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.GREEN + "Z Scale : " + bd.getTransformation().getScale().z));
                            break;
                    }
                }
            } else if (entity instanceof Interaction it) {
                if (it.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID)
                        && direction.equals("y")) {
                    int one = player.isSneaking() ? -1 : 1;
                    if ((it.getInteractionHeight() < 5.0f && one > 0)
                            || (it.getInteractionHeight() > -5.0f && one < 0))
                        it.setInteractionHeight(it.getInteractionHeight() + (scalar)*one);
                }
            }
        }
    }

    public void raydragOperation(List<Entity> near, Entity eventEntity, Player player, String blockDisplayID) {
        UUID pUID = player.getUniqueId();
        isRayDragging.put(pUID, !isRayDragging.getOrDefault(pUID, false));
        if (eventEntity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID)
                && eventEntity instanceof Interaction it) {
            Location playerLoc = player.getLocation();
            Location bdLoc = eventEntity.getLocation().add(0.0d,0.0d,0.0d);
            double initialDistance = Math.min(playerLoc.distance(bdLoc), 3.0d);
            new BukkitRunnable() {
                public void run() {
                    if (!isRayDragging.get(pUID)) {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(" ")); // Reset the title
                        cancel();
                        return;
                    }
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.GOLD + "Click again to stop moving the block display."));
                    for (Entity entity : near) {
                        if (entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID)) {
                            Location updatedPlayerLoc = player.getEyeLocation();
                            Vector direction = updatedPlayerLoc.getDirection().normalize().multiply(initialDistance);
                            Location newEntityLocation = updatedPlayerLoc.add(direction).add(0.0d,-it.getInteractionHeight()/2.0d,0.0d);
                            newEntityLocation.setX(Math.round(newEntityLocation.getX()*32.0d)/32.0d);
                            newEntityLocation.setY(Math.round(newEntityLocation.getY()*32.0d)/32.0d);
                            newEntityLocation.setZ(Math.round(newEntityLocation.getZ()*32.0d)/32.0d);
                            newEntityLocation.setYaw(bdLoc.getYaw());
                            newEntityLocation.setPitch(bdLoc.getPitch());
                            if (entity instanceof BlockDisplay bd) {
                                bd.teleport(newEntityLocation.add(-it.getInteractionWidth()/2.0d,0.0,-it.getInteractionWidth()/2.0d));
                            } else {
                                entity.teleport(newEntityLocation);
                            }
                        }
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
        }
    }

    @EventHandler
    public void onPlayerIntEnt(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Interaction)) return;
        Player p = event.getPlayer();
        try {
            if (!p.getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer().has(toolKey, dataType)
                    || !event.getRightClicked().getPersistentDataContainer().has(blockKey, blockDataType)) return;
        } catch (Exception e) {
            return;
        }
        if (!p.hasPermission("bde.tools")) {
            p.sendMessage(ChatColor.DARK_RED + "Sorry, but you don't have the permission to edit block displays. These tools are useless for you.");
        } else {
            String blockDisplayID = event.getRightClicked().getPersistentDataContainer().get(blockKey, blockDataType);
            String item = p.getInventory().getItemInMainHand().getItemMeta().getItemName();
            List<Entity> near = new java.util.ArrayList<>(p.getNearbyEntities(12.0d, 12.0d, 12.0d).stream()
                    .filter(e -> e instanceof BlockDisplay || e instanceof Interaction)
                    .toList());
            near.removeIf(e -> e.getPersistentDataContainer().isEmpty());
            switch (item) {
                case "Move (X)":
                    moveOperation(near, onePixel, 0.0d, 0.0d, p, blockDisplayID, false);
                    break;
                case "Move (Y)":
                    moveOperation(near, 0.0d, onePixel, 0.0d, p, blockDisplayID, false);
                    break;
                case "Move (Z)":
                    moveOperation(near, 0.0d, 0.0d, onePixel, p, blockDisplayID, false);
                    break;
                case "Rotation (X)":
                    rotateOperation(near, "x", p, blockDisplayID, deg);
                    break;
                case "Rotation (Y)":
                    rotateOperation(near, "y", p, blockDisplayID, deg);
                    break;
                case "Rotation (Z)":
                    rotateOperation(near, "z", p, blockDisplayID, deg);
                    break;
                case "Reset Rotation":
                    resetRotation(near, blockDisplayID);
                    break;
                case "Scale (X)":
                    scaleOperation(near, "x", p, blockDisplayID, onePixel);
                    break;
                case "Scale (Y)":
                    scaleOperation(near, "y", p, blockDisplayID, onePixel);
                    break;
                case "Scale (Z)":
                    scaleOperation(near, "z", p, blockDisplayID, onePixel);
                    break;
                case "Brightness (Sky)":
                    brightnessOperation(near, "sky", p, blockDisplayID);
                    break;
                case "Brightness (Block)":
                    brightnessOperation(near, "block", p, blockDisplayID);
                    break;
                case "Delete":
                    deleteOperation(near, blockDisplayID);
                    break;
                case "Move (X) (Double Precision)":
                    moveOperation(near, onePixel, 0.0d, 0.0d, p, blockDisplayID, true);
                    break;
                case "Move (Y) (Double Precision)":
                    moveOperation(near, 0.0d, onePixel, 0.0d, p, blockDisplayID, true);
                    break;
                case "Move (Z) (Double Precision)":
                    moveOperation(near, 0.0d, 0.0d, onePixel, p, blockDisplayID, true);
                    break;
                case "Clone Block Display":
                    cloneOperation(near, p, blockDisplayID);
                    break;
                case "Shrink Interaction":
                    shrinkOperation(near, p, blockDisplayID);
                    break;
                case "Ray Drag":
                    raydragOperation(near, event.getRightClicked(), p, blockDisplayID);
                    break;
                default:
            }
        }
    }

    @EventHandler
    public void onPlaceBarrier(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        if (p.hasPermission("bde.tools")
                && p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Delete")
                && event.getBlockPlaced().getType().equals(Material.BARRIER)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlaceStructureVoid(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        if (p.hasPermission("bde.tools")
                && p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Ray Drag")
                && event.getBlockPlaced().getType().equals(Material.STRUCTURE_VOID)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlaceLight(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        if (p.hasPermission("bde.tools")
                && p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Brightness (Block)")
                && event.getBlockPlaced().getType().equals(Material.LIGHT)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapToChangeHotbar(PlayerSwapHandItemsEvent event) {
        Player p = event.getPlayer();
        if (!p.hasPermission("bde.tools")) return;
        try {
            boolean hasTool = Arrays.stream(p.getInventory().getContents())
                    .filter(Objects::nonNull)
                    .filter(item -> item.hasItemMeta())
                    .anyMatch(item -> item.getItemMeta().getPersistentDataContainer().has(toolKey, dataType));
            if (!hasTool) return;
        } catch (Exception e) {
            return;
        }
        shuffleInv(p.getInventory(), p.getInventory().getContents());
        event.setCancelled(true);
    }
}
