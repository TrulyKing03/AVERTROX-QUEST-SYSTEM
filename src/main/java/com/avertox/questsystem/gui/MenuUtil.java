package com.avertox.questsystem.gui;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class MenuUtil {
    private MenuUtil() {
    }

    public static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static ItemStack glass(Material material, String name) {
        return item(material, name, List.of("§8"));
    }

    public static void frame(Inventory inventory, Material material, String label) {
        ItemStack frame = glass(material, label);
        int rows = inventory.getSize() / 9;

        for (int x = 0; x < 9; x++) {
            inventory.setItem(x, frame);
            inventory.setItem((rows - 1) * 9 + x, frame);
        }
        for (int y = 0; y < rows; y++) {
            inventory.setItem(y * 9, frame);
            inventory.setItem(y * 9 + 8, frame);
        }
    }

    public static String bar(double percent) {
        int filled = (int) Math.round(Math.max(0D, Math.min(100D, percent)) / 10D);
        StringBuilder sb = new StringBuilder("§8[");
        for (int i = 0; i < 10; i++) {
            sb.append(i < filled ? "§a▌" : "§7▌");
        }
        sb.append("§8]");
        return sb.toString();
    }
}
