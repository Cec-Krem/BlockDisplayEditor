package me.krem.blockDisplayEditor;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
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
        Vector3f scale = blockDisplay.getTransformation().getScale();
        return new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), leftRot, scale, rightRot);
    }

    public Transformation rotQuaternionY(float angle, BlockDisplay blockDisplay) {
        Quaternionf leftRot = blockDisplay.getTransformation().getLeftRotation().rotateLocalY(angle);
        Quaternionf rightRot = blockDisplay.getTransformation().getRightRotation().rotateLocalY(angle);
        Vector3f scale = blockDisplay.getTransformation().getScale();
        return new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), leftRot, scale, rightRot);
    }

    public Transformation rotQuaternionZ(float angle, BlockDisplay blockDisplay) {
        Quaternionf leftRot = blockDisplay.getTransformation().getLeftRotation().rotateLocalZ(angle);
        Quaternionf rightRot = blockDisplay.getTransformation().getRightRotation().rotateLocalZ(angle);
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
            x/=2;
            y/=2;
            z/=2;
        }
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

    public void brightnessOperation(List<Entity> blockToTranslate, String type, Player play, String blockDisplayID) {
        for (Entity entity : blockToTranslate) {
            if ((entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID)) && entity instanceof BlockDisplay bd) {
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
            if (entity instanceof BlockDisplay bd) {
                if ((entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID))) {
                    if (play.isSneaking() && direction.equals("x")) {
                        bd.setTransformation(rotQuaternionX(-angle, bd));
                    } else if (direction.equals("x")) {
                        bd.setTransformation(rotQuaternionX(angle, bd));
                    } else if (play.isSneaking() && direction.equals("y")) {
                        bd.setTransformation(rotQuaternionY(-angle, bd));
                    } else if (direction.equals("y")) {
                        bd.setTransformation(rotQuaternionY(angle, bd));
                    } else if (play.isSneaking() && direction.equals("z")) {
                        bd.setTransformation(rotQuaternionZ(-angle, bd));
                    } else if (direction.equals("z")) {
                        bd.setTransformation(rotQuaternionZ(angle, bd));
                    }
                }
            }
        }
    }

    public void resetRotation(List<Entity> blockToMove, String blockDisplayID) {
        for (Entity entity : blockToMove) {
            if (entity instanceof BlockDisplay bd && bd.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID)) {
                bd.setTransformation(resetRotQuaternion(bd));
            }
        }
    }

    public void deleteOperation(List<Entity> blockToMove, String blockDisplayID) {
        for (Entity entity : blockToMove) {
            if (entity instanceof BlockDisplay bd && bd.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID)) {
                bd.remove();
            }
            if (entity instanceof Interaction it && it.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID)) {
                it.remove();
            }
        }
    }

    public void scaleOperation(List<Entity> blockToMove, String direction, Player play, String blockDisplayID, float scalar) {
        for (Entity entity : blockToMove) {
            if (entity instanceof BlockDisplay bd) {
                if ((entity.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID))) {
                    Vector3f bdscale = bd.getTransformation().getScale();
                    if (play.isSneaking() && direction.equals("x")) {
                        if (bdscale.x == -5.0f) return;
                        bd.setTransformation(scaleBlock(-scalar, 0.0f, 0.0f, bd));
                    } else if (direction.equals("x")) {
                        if (bdscale.x == 5.0f) return;
                        bd.setTransformation(scaleBlock(scalar, 0.0f, 0.0f, bd));
                    } else if (play.isSneaking() && direction.equals("y")) {
                        if (bdscale.y == -5.0f) return;
                        bd.setTransformation(scaleBlock(0.0f, -scalar, 0.0f, bd));
                    } else if (direction.equals("y")) {
                        if (bdscale.y == 5.0f) return;
                        bd.setTransformation(scaleBlock(0.0f, scalar, 0.0f, bd));
                    } else if (play.isSneaking() && direction.equals("z")) {
                        if (bdscale.z == -5.0f) return;
                        bd.setTransformation(scaleBlock(0.0f, 0.0f, -scalar, bd));
                    } else if (direction.equals("z")) {
                        if (bdscale.z == 5.0f) return;
                        bd.setTransformation(scaleBlock(0.0f, 0.0f, scalar, bd));
                    }
                }
            } else if (entity instanceof Interaction it) {
                if ((it.getPersistentDataContainer().get(blockKey, blockDataType).equals(blockDisplayID))) {
                    if (play.isSneaking() && direction.equals("y")) {
                        if (it.getInteractionHeight() == -5.0f) return;
                        it.setInteractionHeight(it.getInteractionHeight() - (scalar));
                    } else if (direction.equals("y")) {
                        if (it.getInteractionHeight() == 5.0f) return;
                        it.setInteractionHeight(it.getInteractionHeight() + scalar);
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
            List<Entity> near = p.getNearbyEntities(6.0d, 6.0d, 6.0d)
                    .stream().filter(e -> e instanceof BlockDisplay || e instanceof Interaction)
                    .toList();
            if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Move (X)")) {
                moveOperation(near, 0.0625d, 0.0d, 0.0d, p, blockDisplayID, false);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Move (Y)")) {
                moveOperation(near, 0.0, 0.0625d, 0.0d, p, blockDisplayID, false);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Move (Z)")) {
                moveOperation(near, 0.0, 0.0d, 0.0625d, p, blockDisplayID, false);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Rotation (X)")) {
                rotateOperation(near, "x", p, blockDisplayID, (float) Math.PI / 180);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Rotation (Y)")) {
                rotateOperation(near, "y", p, blockDisplayID, (float) Math.PI / 180);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Rotation (Z)")) {
                rotateOperation(near, "y", p, blockDisplayID, (float) Math.PI / 180);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Reset Rotation")) {
                resetRotation(near, blockDisplayID);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Scale (X)")) {
                scaleOperation(near, "x", p, blockDisplayID, 0.0625f);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Scale (Y)")) {
                scaleOperation(near, "y", p, blockDisplayID, 0.0625f);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Scale (Z)")) {
                scaleOperation(near, "z", p, blockDisplayID, 0.0625f);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Brightness (Sky)")) {
                brightnessOperation(near, "sky", p, blockDisplayID);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Brightness (Block)")) {
                brightnessOperation(near, "block", p, blockDisplayID);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Delete")) {
                deleteOperation(near, blockDisplayID);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Move (X) (Double Precision)")) {
                moveOperation(near, 0.0625d, 0.0d, 0.0d, p, blockDisplayID, true);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Move (Y) (Double Precision)")) {
                moveOperation(near, 0.0d, 0.0625d, 0.0d, p, blockDisplayID, true);
            } else if (p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Move (Z) (Double Precision)")) {
                moveOperation(near, 0.0d, 0.0d, 0.0625d, p, blockDisplayID, true);
            }
            return;

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
    public void onPlaceLight(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        if (p.hasPermission("bde.tools") && p.getInventory().getItemInMainHand().getItemMeta().getItemName().equals("Brightness (Block)") && event.getBlockPlaced().getType().equals(Material.LIGHT)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onSwapToChangeHotbar(PlayerSwapHandItemsEvent event) {
        Player p = event.getPlayer();
        if (p.getInventory().getChestplate() == null) return;
        if (p.hasPermission("bde.tools") && p.getInventory().getChestplate().getType().equals(Material.LIGHT_GRAY_STAINED_GLASS_PANE)) {
            ItemStack[] pInv = p.getInventory().getContents();
            for (int i = 0; i < 9; i++) {
                p.getInventory().setItem(i, pInv[i + 27]);
            }
            for (int i = 27; i < 36; i++) {
                p.getInventory().setItem(i, pInv[i - 27]);
            }
            event.setCancelled(true);
            return;
        }
    }
}
