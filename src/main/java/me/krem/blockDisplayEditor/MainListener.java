package me.krem.blockDisplayEditor;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public class MainListener implements Listener {
    private BlockDisplayEditor plugin;
    private final NamespacedKey key;
    private final NamespacedKey blockKey;
    private final PersistentDataType<Byte, Boolean> dataType = PersistentDataType.BOOLEAN;
    private final PersistentDataType<String, String> blockDataType = PersistentDataType.STRING;

    public MainListener(BlockDisplayEditor plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "BDE_Tool");
        this.blockKey = new NamespacedKey(plugin, "BDE_Display");
    }

    public Transformation rotQuaternionX(float angle, BlockDisplay blockDisplay) {
        Quaternionf leftRot = blockDisplay.getTransformation().getLeftRotation().rotateLocalX(angle);
        Quaternionf rightRot = blockDisplay.getTransformation().getRightRotation().rotateLocalX(angle);
        return new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), leftRot, new Vector3f(1.0f), rightRot);
    }

    public Transformation rotQuaternionY(float angle, BlockDisplay blockDisplay) {
        Quaternionf leftRot = blockDisplay.getTransformation().getLeftRotation().rotateLocalY(angle);
        Quaternionf rightRot = blockDisplay.getTransformation().getRightRotation().rotateLocalY(angle);
        return new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), leftRot, new Vector3f(1.0f), rightRot);
    }

    public Transformation rotQuaternionZ(float angle, BlockDisplay blockDisplay) {
        Quaternionf leftRot = blockDisplay.getTransformation().getLeftRotation().rotateLocalZ(angle);
        Quaternionf rightRot = blockDisplay.getTransformation().getRightRotation().rotateLocalZ(angle);
        return new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), leftRot, new Vector3f(1.0f), rightRot);
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

    public void translateOperation(List<Entity> blockToMove, double x, double y, double z, Player play, String blockDisplayID) {
        for (Entity entity : blockToMove) {
            if ((entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID))) {
                if (play.isSneaking()) {
                    entity.teleport(entity.getLocation().add(-x, -y, -z));
                } else {
                    entity.teleport(entity.getLocation().add(x, y, z));
                }
            }
        }
    }

    public void rotateOperation(List<Entity> blockToMove, String direction, Player play, String blockDisplayID, float angle) {
        for (Entity entity : blockToMove) {
            if (entity instanceof Interaction) {
                ((Interaction) entity).setInteractionWidth(1.0f);
                ((Interaction) entity).setInteractionHeight(1.0f);
            }
            if (entity instanceof BlockDisplay) {
                if ((entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID))) {
                    if (play.isSneaking() && direction.equals("x")) {
                        ((BlockDisplay) entity).setTransformation(rotQuaternionX(-angle, (BlockDisplay) entity));
                    } else if (direction.equals("x")) {
                        ((BlockDisplay) entity).setTransformation(rotQuaternionX(angle, (BlockDisplay) entity));
                    } else if (play.isSneaking() && direction.equals("y")) {
                        ((BlockDisplay) entity).setTransformation(rotQuaternionY(-angle, (BlockDisplay) entity));
                    } else if (direction.equals("y")) {
                        ((BlockDisplay) entity).setTransformation(rotQuaternionY(angle, (BlockDisplay) entity));
                    } else if (play.isSneaking() && direction.equals("z")) {
                        ((BlockDisplay) entity).setTransformation(rotQuaternionZ(-angle, (BlockDisplay) entity));
                    } else if (direction.equals("z")) {
                        ((BlockDisplay) entity).setTransformation(rotQuaternionZ(angle, (BlockDisplay) entity));
                    }
                }
            }
        }
    }

    public void resetRotation(List<Entity> blockToMove, String blockDisplayID) {
        for (Entity entity : blockToMove) {
            if (entity instanceof BlockDisplay) {
                if ((entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID))) {
                    ((BlockDisplay) entity).setTransformation(resetRotQuaternion((BlockDisplay) entity));
                }
            }
        }
    }

    public void deleteOperation(List<Entity> blockToMove, String blockDisplayID) {
        for (Entity entity : blockToMove) {
            if (entity instanceof BlockDisplay) {
                if ((entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID))) {
                    entity.remove();
                }
            }
            if (entity instanceof Interaction) {
                if ((entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID))) {
                    entity.remove();
                }
            }
        }
    }

    public void scaleOperation(List<Entity> blockToMove, String direction, Player play, String blockDisplayID, float scalar) {
        for (Entity entity : blockToMove) {
            if (entity instanceof BlockDisplay) {
                if ((entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID))) {
                    if (play.isSneaking() && direction.equals("x")) {
                        ((BlockDisplay) entity).setTransformation(scaleBlock(-scalar, 0.0f, 0.0f, (BlockDisplay) entity));
                    } else if (direction.equals("x")) {
                        ((BlockDisplay) entity).setTransformation(scaleBlock(scalar, 0.0f, 0.0f, (BlockDisplay) entity));
                    } else if (play.isSneaking() && direction.equals("y")) {
                        ((BlockDisplay) entity).setTransformation(scaleBlock(0.0f, -scalar, 0.0f, (BlockDisplay) entity));
                    } else if (direction.equals("y")) {
                        ((BlockDisplay) entity).setTransformation(scaleBlock(0.0f, scalar, 0.0f, (BlockDisplay) entity));
                    } else if (play.isSneaking() && direction.equals("z")) {
                        ((BlockDisplay) entity).setTransformation(scaleBlock(0.0f, 0.0f, -scalar, (BlockDisplay) entity));
                    } else if (direction.equals("z")) {
                        ((BlockDisplay) entity).setTransformation(scaleBlock(0.0f, 0.0f, scalar, (BlockDisplay) entity));
                    }
                }
            } else if (entity instanceof Interaction) {
                if ((entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID))) {
                    if (play.isSneaking() && direction.equals("y")) {
                        ((Interaction) entity).setInteractionHeight(((Interaction) entity).getInteractionHeight() - (scalar));
                    } else if (direction.equals("y")) {
                        ((Interaction) entity).setInteractionHeight(((Interaction) entity).getInteractionHeight() + scalar);
                    }
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
            p.getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer();
        } catch (Exception e) {
            return;
        }
        if (!p.getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer().has(key, dataType)) return;
        if (!p.hasPermission("bde.tools")) {
            p.sendMessage(ChatColor.DARK_RED + "Sorry, but you don't have the permission to edit block displays. These tools are useless for you.");
            return;
        } else {
            String blockDisplayID = event.getRightClicked().getPersistentDataContainer().get(blockKey, blockDataType);
            List<Entity> near = p.getNearbyEntities(6.0d, 6.0d, 6.0d);
            if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Translation (X)")) {
                translateOperation(near, 0.0625, 0.0d, 0.0d, p, blockDisplayID);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Translation (Y)")) {
                translateOperation(near, 0.0, 0.0625d, 0.0d, p, blockDisplayID);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Translation (Z)")) {
                translateOperation(near, 0.0, 0.0d, 0.0625d, p, blockDisplayID);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Rotation (X)")) {
                rotateOperation(near, "x", p, blockDisplayID, 0.02f);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Rotation (Y)")) {
                rotateOperation(near, "y", p, blockDisplayID, 0.02f);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Rotation (Z)")) {
                rotateOperation(near, "z", p, blockDisplayID, 0.02f);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Reset Rotation")) {
                resetRotation(near, blockDisplayID);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Scale (X)")) {
                scaleOperation(near, "x", p, blockDisplayID, 0.0625f);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Scale (Y)")) {
                scaleOperation(near, "y", p, blockDisplayID, 0.0625f);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Scale (Z)")) {
                scaleOperation(near, "z", p, blockDisplayID, 0.0625f);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Delete")) {
                deleteOperation(near, blockDisplayID);
                return;
            }

        }
    }

    @EventHandler
    public void onPlaceBarrier(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        if (p.hasPermission("bde.tools") && p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Delete") && event.getBlockPlaced().getType().equals(Material.BARRIER)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onSwapToChangeHotbar(PlayerSwapHandItemsEvent event) {
        Player p = event.getPlayer();
        if (p.hasPermission("bde.tools") && p.getInventory().contains(Material.LIGHT_GRAY_STAINED_GLASS_PANE)) {
            ItemStack[] pInv = p.getInventory().getContents();
            for (int i = 0; i<7;i++) {
                p.getInventory().setItem(i, pInv[i+27]);
            }
            for (int i = 27; i<34;i++) {
                p.getInventory().setItem(i, pInv[i-27]);
            }
            event.setCancelled(true);
            return;
        }
    }
}
