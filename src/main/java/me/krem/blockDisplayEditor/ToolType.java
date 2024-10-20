package me.krem.blockDisplayEditor;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.persistence.PersistentDataType;

public enum ToolType {
    MOVE_X(0,Material.TIPPED_ARROW, "Move (X)", PotionType.HEALING),
    MOVE_Y(1,Material.TIPPED_ARROW, "Move (Y)", PotionType.SWIFTNESS),
    MOVE_Z(2,Material.TIPPED_ARROW, "Move (Z)", PotionType.LUCK),
    ROTATION_X(3,Material.IRON_INGOT, "Rotation (X)"),
    ROTATION_Y(4,Material.COPPER_INGOT, "Rotation (Y)"),
    ROTATION_Z(5,Material.GOLD_INGOT, "Rotation (Z)"),
    RESET_ROTATION(8,Material.NETHERITE_INGOT, "Reset Rotation"),
    SCALE_X(18,Material.BRICK, "Scale (X)"),
    SCALE_Y(19,Material.NETHER_BRICK, "Scale (Y)"),
    SCALE_Z(20,Material.NETHERITE_SCRAP, "Scale (Z)"),
    SHRINK_INTERACTION(21,Material.PRISMARINE_CRYSTALS, "Shrink Interaction"),
    RAY_DRAG(22,Material.STRUCTURE_VOID, "Ray Drag"),
    CLONE_BLOCK_DISPLAY(26,Material.MAGMA_CREAM, "Clone Block Display"),
    MOVE_X_DOUBLE_PREC(27,Material.AMETHYST_SHARD, "Move (X) (Double Precision)"),
    MOVE_Y_DOUBLE_PREC(28,Material.PRISMARINE_SHARD, "Move (Y) (Double Precision)"),
    MOVE_Z_DOUBLE_PREC(29,Material.EMERALD, "Move (Z) (Double Precision)"),
    BRIGHTNESS_BLOCK(30,Material.LIGHT, "Brightness (Block)"),
    BRIGHTNESS_SKY(31,Material.NETHER_STAR, "Brightness (Sky)"),
    DELETE(35,Material.BARRIER, "Delete");

    private final Material material;
    private final String displayName;
    private final PotionType potionType;
    private final Integer slot;

    ToolType(Integer slot, Material material, String displayName) {
        this(slot, material, ChatColor.RESET+displayName, null);
    }

    ToolType(Integer slot, Material material, String displayName, PotionType potionType) {
        this.slot = slot;
        this.material = material;
        this.displayName = ChatColor.RESET+displayName;
        this.potionType = potionType;
    }

    public ItemStack createItem(NamespacedKey toolKey) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (potionType != null && meta instanceof PotionMeta) {
            ((PotionMeta) meta).setBasePotionType(potionType);
        }
        meta.setDisplayName(displayName);
        meta.getPersistentDataContainer().set(toolKey, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    public int getSlot() {
        return this.slot;
    }
}
