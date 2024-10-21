package me.krem.blockDisplayEditor;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;

import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

public class MainListener implements Listener {
    private final NamespacedKey toolKey;
    private final NamespacedKey blockKey;
    private final NamespacedKey lockKey;
    private final PersistentDataType<Byte, Boolean> dataType = PersistentDataType.BOOLEAN;
    private final PersistentDataType<Byte, Boolean> isBDLocked = PersistentDataType.BOOLEAN;
    private final PersistentDataType<String, String> blockDataType = PersistentDataType.STRING;
    private final float deg = (float) Math.PI / 360; // Half a degree
    private final float onePixel = 0.0625f;
    private final float halfPixel = 0.03125f;
    HashMap<UUID, Boolean> isRayDragging = new HashMap<>();
    private BlockDisplayEditor plugin;

    public MainListener(BlockDisplayEditor plugin) {
        this.plugin = plugin;
        this.toolKey = new NamespacedKey(plugin, "BDE_Tool");
        this.blockKey = new NamespacedKey(plugin, "BDE_Display");
        this.lockKey = new NamespacedKey(plugin, "BDE_Locked_Display");
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

    public Transformation rotQuaternionXYZ(float angle, BlockDisplay blockDisplay, String axis) {
        List<Quaternionf> rotations = new ArrayList<>();
        // axis must be "x" "y" or "z"
        switch (axis) {
            case "x" -> rotations = Arrays.asList(blockDisplay.getTransformation().getLeftRotation().rotateX(angle),
                    blockDisplay.getTransformation().getRightRotation().rotateX(angle));
            case "y" -> rotations = Arrays.asList(blockDisplay.getTransformation().getLeftRotation().rotateY(angle),
                    blockDisplay.getTransformation().getRightRotation().rotateY(angle));
            case "z" -> rotations = Arrays.asList(blockDisplay.getTransformation().getLeftRotation().rotateZ(angle),
                    blockDisplay.getTransformation().getRightRotation().rotateZ(angle));
        }
        Vector3f scale = blockDisplay.getTransformation().getScale();
        return new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), rotations.getFirst(), scale, rotations.getLast());
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

    public void moveOperation(List<Entity> blockToMove, double x, double y, double z, Player player, String blockDisplayID) {
        for (Entity entity : blockToMove) {
            if ((entity instanceof Interaction || entity instanceof BlockDisplay)
                    && entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID)) {
                byte sneak = (byte) (player.isSneaking() ? -1 : 1);
                entity.teleport(entity.getLocation().add(x * sneak, y * sneak, z * sneak));
            }
        }
    }

    public void shrinkOperation(List<Entity> blockToShrink, Player player, String blockDisplayID) {
        byte sneak = (byte) (player.isSneaking() ? 1 : -1);
        float step = sneak * halfPixel;
        ChatColor color = sneak < 0 ? ChatColor.RED : ChatColor.GREEN;
        for (Entity entity : blockToShrink) {
            if (entity instanceof Interaction it
                    && entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID)) {
                float width = it.getInteractionWidth();
                if (width + step >= onePixel && width + step <= 2.0f) {
                    it.setInteractionWidth(width + step);
                    it.teleport(it.getLocation().clone().add(step/2,0.0f, step/2));
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(
                            color + "Set interaction width to " + it.getInteractionWidth()));
                }
            }
        }
    }

    public void brightnessOperation(List<Entity> blockToEdit, String type, Player player, String blockDisplayID) {
        byte sneak = (byte) (player.isSneaking() ? -1 : 1);
        ChatColor color = sneak < 0 ? ChatColor.RED : ChatColor.GREEN;
        for (Entity entity : blockToEdit) {
            if (entity instanceof BlockDisplay bd && (entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID))) {
                if (bd.getBrightness() == null) bd.setBrightness(new Display.Brightness(0, 0));
                int[] bdBrightness = {bd.getBrightness().getBlockLight(), bd.getBrightness().getSkyLight()};
                switch (type) {
                    case "block":
                        if (bdBrightness[0] + sneak >= 0 && bdBrightness[0] + sneak <= 15) {
                            bd.setBrightness(new Display.Brightness(bdBrightness[0] + sneak, bdBrightness[1]));
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                    TextComponent.fromLegacy(color + "Set block brightness to " + bd.getBrightness().getBlockLight()));
                        }
                        break;
                    case "sky":
                        if (bdBrightness[1] + sneak >= 0 && bdBrightness[1] + sneak <= 15) {
                            bd.setBrightness(new Display.Brightness(bdBrightness[0], bdBrightness[1] + sneak));
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                    TextComponent.fromLegacy(color + "Set sky brightness to " + bd.getBrightness().getSkyLight()));
                        }
                        break;
                }
            }
        }
    }

    public void rotateOperation(List<Entity> blockToRotate, String axis, Player player, String blockDisplayID, float angle) {
        for (Entity entity : blockToRotate) {
            if (entity instanceof BlockDisplay bd
                    && entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID)) {
                byte sneak = (byte) (player.isSneaking() ? -1 : 1);
                // axis must be "x" "y" or "z"
                bd.setTransformation(rotQuaternionXYZ(angle * sneak, bd, axis));
                bd.setTransformation(rotQuaternionXYZ(angle * sneak, bd, axis));
            }
        }
    }

    public void resetRotation(List<Entity> blockToReset, String blockDisplayID) {
        for (Entity entity : blockToReset) {
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

    public void scaleOperation(List<Entity> blockToScale, String direction, Player player, String blockDisplayID, float scalar) {
        byte sneak = (byte) (player.isSneaking() ? -1 : 1);
        float step = scalar * sneak;
        ChatColor color = sneak < 0 ? ChatColor.RED : ChatColor.GREEN;
        for (Entity entity : blockToScale) {
            if (entity instanceof BlockDisplay bd && entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID)) {
                Vector3f bdscale = bd.getTransformation().getScale();
                switch(direction) {
                    case "x":
                        if (Math.abs(bdscale.x + step) > 5.0f) return;
                        bd.setTransformation(scaleBlock(step, 0.0f, 0.0f, bd));
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent
                                .fromLegacy(color + "X Scale : " + bd.getTransformation().getScale().x));
                        break;
                    case "y":
                        if (Math.abs(bdscale.y + step) > 5.0f) return;
                        bd.setTransformation(scaleBlock(0.0f, step, 0.0f, bd));
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent
                                .fromLegacy(color + "Y Scale : " + bd.getTransformation().getScale().y));
                        break;
                    case "z":
                        if (Math.abs(bdscale.z + step) > 5.0f) return;
                        bd.setTransformation(scaleBlock(0.0f, 0.0f, step, bd));
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent
                                .fromLegacy(color + "Z Scale : " + bd.getTransformation().getScale().z));
                        break;
                }
            } else if (direction.equals("y") && entity instanceof Interaction it
                    && it.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID)
                    && (Math.abs(it.getInteractionHeight() + step) < 5.0f)) it.setInteractionHeight(it.getInteractionHeight() + step);
        }
    }

    public void raydragOperation(List<Entity> near, Entity eventEntity, Player player, String blockDisplayID) {
        UUID pUID = player.getUniqueId();
        isRayDragging.put(pUID, !isRayDragging.getOrDefault(pUID, false));
        if (eventEntity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID)
                && eventEntity instanceof Interaction it) {
            Location playerLoc = player.getLocation();
            Location bdLoc = eventEntity.getLocation();
            double initialDistance = Math.min(playerLoc.distance(bdLoc), 3.0d);
            new BukkitRunnable() {
                public void run() {
                    if (!isRayDragging.get(pUID)) {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(" ")); // Reset the title
                        cancel();
                        return;
                    }
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent
                            .fromLegacy(ChatColor.GOLD + "Click again to stop moving the block display."));
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

    public void lockOperation(List<Entity> near, Player player, String blockDisplayID) {
        for (Entity entity : near) {
            if (entity.getPersistentDataContainer().getOrDefault(blockKey, blockDataType, "").equals(blockDisplayID)) {
                PersistentDataContainer persistData = entity.getPersistentDataContainer();
                if (!persistData.getOrDefault(lockKey, isBDLocked, false)) {
                    persistData.set(lockKey, isBDLocked, true);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent
                            .fromLegacy(ChatColor.RED + "Locked Block Display"));
                } else {
                    persistData.set(lockKey, isBDLocked, false);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent
                            .fromLegacy(ChatColor.GREEN + "Unlocked Block Display"));
                }
            }
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
            String itemName = p.getInventory().getItemInMainHand().getItemMeta().getDisplayName();
            List<Entity> near = new ArrayList<>(p.getNearbyEntities(12.0d, 12.0d, 12.0d).stream()
                    .filter(e -> e instanceof BlockDisplay || e instanceof Interaction)
                    .toList());
            near.removeIf(e -> e.getPersistentDataContainer().isEmpty());
            if (!itemName.equals("Lock/Unlock Block Display")
                    && event.getRightClicked().getPersistentDataContainer().getOrDefault(lockKey, isBDLocked, false)) {
                // Don't do anything if the block display is locked
                return;
            }
            switch (itemName) {
                case "Move (X)" -> moveOperation(near, onePixel, 0.0d, 0.0d, p, blockDisplayID);
                case "Move (Y)" -> moveOperation(near, 0.0d, onePixel, 0.0d, p, blockDisplayID);
                case "Move (Z)" -> moveOperation(near, 0.0d, 0.0d, onePixel, p, blockDisplayID);
                case "Move (X) (Double Precision)" -> moveOperation(near, halfPixel, 0.0d, 0.0d, p, blockDisplayID);
                case "Move (Y) (Double Precision)" -> moveOperation(near, 0.0d, halfPixel, 0.0d, p, blockDisplayID);
                case "Move (Z) (Double Precision)" -> moveOperation(near, 0.0d, 0.0d, halfPixel, p, blockDisplayID);

                case "Rotation (X)" -> rotateOperation(near, "x", p, blockDisplayID, deg);
                case "Rotation (Y)" -> rotateOperation(near, "y", p, blockDisplayID, deg);
                case "Rotation (Z)" -> rotateOperation(near, "z", p, blockDisplayID, deg);
                case "Reset Rotation" -> resetRotation(near, blockDisplayID);

                case "Scale (X)" -> scaleOperation(near, "x", p, blockDisplayID, halfPixel);
                case "Scale (Y)" -> scaleOperation(near, "y", p, blockDisplayID, halfPixel);
                case "Scale (Z)" -> scaleOperation(near, "z", p, blockDisplayID, halfPixel);
                case "Shrink Interaction" -> shrinkOperation(near, p, blockDisplayID);

                case "Brightness (Sky)" -> brightnessOperation(near, "sky", p, blockDisplayID);
                case "Brightness (Block)" -> brightnessOperation(near, "block", p, blockDisplayID);

                case "Delete" -> deleteOperation(near, blockDisplayID);
                case "Clone Block Display" -> cloneOperation(near, p, blockDisplayID);
                case "Ray Drag" -> raydragOperation(near, event.getRightClicked(), p, blockDisplayID);
                case "Lock/Unlock Block Display" -> lockOperation(near, p, blockDisplayID);
                default ->  {}
            }
        }
    }

    @EventHandler
    public void onPlaceTool(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        if (!p.hasPermission("bde.tools")) return;
        try {
            boolean hasTool = Arrays.stream(p.getInventory().getContents())
                    .filter(Objects::nonNull)
                    .filter(ItemStack::hasItemMeta)
                    .anyMatch(item -> p.getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer().has(toolKey, dataType));
            if (!hasTool) return;
        } catch (Exception e) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlaceInItemFrame(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof ItemFrame) {
            Player p = event.getPlayer();
            try {
                boolean hasTool = Arrays.stream(p.getInventory().getContents())
                        .filter(Objects::nonNull)
                        .filter(ItemStack::hasItemMeta)
                        .anyMatch(item -> p.getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer().has(toolKey, dataType));
                if (!hasTool) return;
            } catch (Exception e) {
                return;
            }
            event.setCancelled(true);
            p.updateInventory();
        }
    }

    @EventHandler
    public void onDropTool(PlayerDropItemEvent event) {
        Player p = event.getPlayer();
        if (!p.hasPermission("bde.tools")) return;
        try {
            boolean didToolDropped = event.getItemDrop().getItemStack().getItemMeta().getPersistentDataContainer().has(toolKey, dataType);
            if (!didToolDropped) return;
        } catch (Exception e) {
            return;
        }
        event.setCancelled(true);
        p.updateInventory();
    }

    @EventHandler
    public void onSwapToChangeHotbar(PlayerSwapHandItemsEvent event) {
        Player p = event.getPlayer();
        if (!p.hasPermission("bde.tools")) return;
        try {
            boolean hasTool = Arrays.stream(p.getInventory().getContents())
                    .filter(Objects::nonNull)
                    .filter(ItemStack::hasItemMeta)
                    .anyMatch(item -> item.getItemMeta().getPersistentDataContainer().has(toolKey, dataType));
            if (!hasTool) return;
        } catch (Exception e) {
            return;
        }
        shuffleInv(p.getInventory(), p.getInventory().getContents());
        event.setCancelled(true);
    }
}
