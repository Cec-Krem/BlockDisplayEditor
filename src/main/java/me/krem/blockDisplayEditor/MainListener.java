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
import org.bukkit.util.Transformation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

public class MainListener implements Listener {
    private final NamespacedKey key;
    private final NamespacedKey blockKey;
    private final PersistentDataType<Byte, Boolean> dataType = PersistentDataType.BOOLEAN;
    private final PersistentDataType<String, String> blockDataType = PersistentDataType.STRING;
    private final float deg = (float) Math.PI / 180;
    private final float onePixel = 0.0625f;

    public MainListener(BlockDisplayEditor plugin) {
        this.key = new NamespacedKey(plugin, "BDE_Tool");
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
        return new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), blockDisplay.getTransformation().getScale(), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f));
    }

    public Transformation scaleBlock(float scalarX, float scalarY, float scalarZ, BlockDisplay blockDisplay) {
        Quaternionf leftRot = blockDisplay.getTransformation().getLeftRotation();
        Quaternionf rightRot = blockDisplay.getTransformation().getRightRotation();
        float[] scale = {blockDisplay.getTransformation().getScale().x, blockDisplay.getTransformation().getScale().y, blockDisplay.getTransformation().getScale().z};
        return new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), leftRot, new Vector3f(scale[0] + scalarX, scale[1] + scalarY, scale[2] + scalarZ), rightRot);
    }

    public void moveOperation(List<Entity> blockToMove, double x, double y, double z, Player play, String blockDisplayID, boolean doublePrecision) {
        if (doublePrecision) {
            x /= 2;
            y /= 2;
            z /= 2;
        }
        for (Entity entity : blockToMove) {
            if ((entity instanceof Interaction || entity instanceof BlockDisplay) && entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID)) {
                if (play.isSneaking()) {
                    entity.teleport(entity.getLocation().add(-x, -y, -z));
                } else {
                    entity.teleport(entity.getLocation().add(x, y, z));
                }
            }
        }
    }

    public void shrinkOperation(List<Entity> blockToMove, Player play, String blockDisplayID) {
        for (Entity entity : blockToMove) {
            if (entity instanceof Interaction it && entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID)) {
                float width = it.getInteractionWidth();
                if (!play.isSneaking() && width > onePixel) {
                    it.setInteractionWidth(width - onePixel);
                    it.teleport(it.getLocation().clone().add((double) -onePixel/2,0.0f,(double) -onePixel/2));
                } else if (play.isSneaking() && width < 2.0f) {
                    it.setInteractionWidth(width + onePixel);
                    it.teleport(it.getLocation().clone().add((double) onePixel/2,0.0f,(double) onePixel/2));
                } else return;
            }
        }
    }

    public void brightnessOperation(List<Entity> blockToTranslate, String type, Player play, String blockDisplayID) {
        for (Entity entity : blockToTranslate) {
            if (entity instanceof BlockDisplay bd && (entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID))) {
                if (bd.getBrightness() == null) {
                    bd.setBrightness(new Display.Brightness(0, 0));
                }
                int[] bdBrightness = {bd.getBrightness().getBlockLight(), bd.getBrightness().getSkyLight()};
                if (play.isSneaking() && type.equals("block") && bdBrightness[0] > 0) {
                    bd.setBrightness(new Display.Brightness(bdBrightness[0] - 1, bdBrightness[1]));
                    play.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.RED + "Set block brightness to " + bd.getBrightness().getBlockLight()));
                } else if (play.isSneaking() && type.equals("sky") && bdBrightness[1] > 0) {
                    bd.setBrightness(new Display.Brightness(bdBrightness[0], bdBrightness[1] - 1));
                    play.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.RED + "Set sky brightness to " + bd.getBrightness().getSkyLight()));
                } else if (!play.isSneaking() && type.equals("block") && bdBrightness[0] < 15) {
                    bd.setBrightness(new Display.Brightness(bdBrightness[0] + 1, bdBrightness[1]));
                    play.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.GREEN + "Set block brightness to " + bd.getBrightness().getBlockLight()));
                } else if (!play.isSneaking() && type.equals("sky") && bdBrightness[1] < 15) {
                    bd.setBrightness(new Display.Brightness(bdBrightness[0], bdBrightness[1] + 1));
                    play.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.GREEN + "Set sky brightness to " + bd.getBrightness().getSkyLight()));
                }
            }
        }
    }

    public void rotateOperation(List<Entity> blockToMove, String direction, Player play, String blockDisplayID, float angle) {
        for (Entity entity : blockToMove) {
            if (entity instanceof BlockDisplay bd
                    && entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID)) {
                if (play.isSneaking()) {
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

    public void scaleOperation(List<Entity> blockToMove, String direction, Player play, String blockDisplayID, float scalar) {
        for (Entity entity : blockToMove) {
            if (entity instanceof BlockDisplay bd
                    && entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID)) {
                Vector3f bdscale = bd.getTransformation().getScale();
                if (play.isSneaking()) {
                    switch(direction) {
                        case "x":
                            if (bdscale.x <= -5.0f) return;
                            bd.setTransformation(scaleBlock(-scalar, 0.0f, 0.0f, bd));
                            break;
                        case "y":
                            if (bdscale.y <= -5.0f) return;
                            bd.setTransformation(scaleBlock(0.0f, -scalar, 0.0f, bd));
                            break;
                        case "z":
                            if (bdscale.z <= -5.0f) return;
                            bd.setTransformation(scaleBlock(0.0f, 0.0f, -scalar, bd));
                            break;
                    }
                } else {
                    switch(direction) {
                        case "x":
                            if (bdscale.x >= 5.0f) return;
                            bd.setTransformation(scaleBlock(scalar, 0.0f, 0.0f, bd));
                            break;
                        case "y":
                            if (bdscale.y >= 5.0f) return;
                            bd.setTransformation(scaleBlock(0.0f, scalar, 0.0f, bd));
                            break;
                        case "z":
                            if (bdscale.z >= 5.0f) return;
                            bd.setTransformation(scaleBlock(0.0f, 0.0f, scalar, bd));
                            break;
                    }
                }
            } else if (entity instanceof Interaction it) {
                if (it.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID)
                        && direction.equals("y")) {
                    int one = play.isSneaking() ? -1 : 1;
                    if ((it.getInteractionHeight() < 5.0f && one > 0)
                    || (it.getInteractionHeight() > -5.0f && one < 0))
                        it.setInteractionHeight(it.getInteractionHeight() + (scalar)*one);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerIntEnt(PlayerInteractEntityEvent event) {
        // Check for tools, permissions. Translations should result in the interaction and block display being teleported.

        if (!(event.getRightClicked() instanceof Interaction)) return;
        Player p = event.getPlayer();
        try {
            if (!p.getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer().has(key, dataType)
                    || !event.getRightClicked().getPersistentDataContainer().has(blockKey, blockDataType)) return;
        } catch (Exception e) {
            return;
        }
        if (!p.hasPermission("bde.tools")) {
            p.sendMessage(ChatColor.DARK_RED + "Sorry, but you don't have the permission to edit block displays. These tools are useless for you.");
        } else {
            String blockDisplayID = event.getRightClicked().getPersistentDataContainer().get(blockKey, blockDataType);
            String item = p.getInventory().getItemInMainHand().getItemMeta().getItemName();
            List<Entity> near = new java.util.ArrayList<>(p.getNearbyEntities(12.0d, 12.0d, 12.0d)
                    .stream().filter(e -> e instanceof BlockDisplay || e instanceof Interaction)
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
        if (p.getInventory().getChestplate() == null) return;
        if (p.hasPermission("bde.tools")
                && p.getInventory().getChestplate().getType().equals(Material.LIGHT_GRAY_STAINED_GLASS_PANE)) {
            shuffleInv(p.getInventory(), p.getInventory().getContents());
            event.setCancelled(true);
        }
    }
}
