package com.avertox.questsystem.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ItemCodec {
    private ItemCodec() {
    }

    public static ItemStack parseItem(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] split = raw.split(":");
        Material material;
        try {
            material = Material.valueOf(split[0].trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
        int amount = 1;
        if (split.length > 1) {
            try {
                amount = Math.max(1, Integer.parseInt(split[1].trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return new ItemStack(material, Math.min(amount, material.getMaxStackSize()));
    }

    public static List<ItemStack> parseItems(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<ItemStack> items = new ArrayList<>();
        for (String entry : raw) {
            ItemStack stack = parseItem(entry);
            if (stack != null) {
                items.add(stack);
            }
        }
        return items;
    }

    public static String serialize(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return "";
        }
        return stack.getType().name() + ":" + stack.getAmount();
    }
}
