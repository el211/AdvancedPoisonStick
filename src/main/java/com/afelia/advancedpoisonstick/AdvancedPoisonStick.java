package com.afelia.advancedpoisonstick;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdvancedPoisonStick extends JavaPlugin implements Listener {
    private int maxUses;
    private int cooldownSeconds;
    private int customModelData;
    private int poisonDuration;
    private Map<Player, Integer> remainingUsesMap;
    private Map<Player, Long> lastUsedTimeMap;

    private String itemName;
    private List<String> itemLore;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        loadConfig();
        remainingUsesMap = new HashMap<>();
        lastUsedTimeMap = new HashMap<>();
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        maxUses = config.getInt("max_uses", 10);
        cooldownSeconds = config.getInt("cooldown", 5);
        customModelData = config.getInt("custom_model_data", 0);
        poisonDuration = config.getInt("poison_duration", 10);
        itemName = ChatColor.translateAlternateColorCodes('&', config.getString("item_name", "&cPoison Stick"));
        itemLore = config.getStringList("item_lore");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("givepoisonstick")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command!");
                return true;
            }
            Player player = (Player) sender;

            // Check if player has permission to give poison sticks
            if (!player.hasPermission("poisonstick.give")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            // Example usage of createPoisonStick method
            int remainingUses = 0; // Initially set to zero uses
            ItemStack stick = createPoisonStick(remainingUses);
            player.getInventory().addItem(stick);
            return true;
        } else if (command.getName().equalsIgnoreCase("giveplayerpoisonstick")) {
            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /giveplayerpoisonstick <player>");
                return false;
            }

            // Check if the sender is the console
            if (!(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
                sender.sendMessage(ChatColor.RED + "This command can only be executed from the console.");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(ChatColor.RED + "Player not found or not online.");
                return true;
            }

            // Check if console has permission to give poison sticks to other players
            if (!sender.hasPermission("poisonstick.giveplayer")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            int remainingUses = 0; // Initially set to zero uses
            ItemStack stick = createPoisonStick(remainingUses);
            target.getInventory().addItem(stick);
            sender.sendMessage(ChatColor.GREEN + "You have given a poison stick to " + target.getName() + ".");
            return true;
        }
        return false;
    }

    private ItemStack createPoisonStick(int remainingUses) {
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        if (meta != null) {
            // Set display name and lore from configuration
            meta.setDisplayName(itemName);
            List<String> lore = new ArrayList<>();
            for (String line : itemLore) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line)
                        .replace("%remaining_uses%", String.valueOf(remainingUses))
                        .replace("%max_uses%", String.valueOf(maxUses)));
            }
            meta.setLore(lore);

            // Set custom model data
            meta.setCustomModelData(customModelData);

            stick.setItemMeta(meta);
        }
        return stick;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item != null && item.getType() == Material.STICK && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(itemName)) {
                if (checkCooldown(player)) {
                    player.sendMessage("Stick is on cooldown!");
                    return;
                }

                Entity target = event.getRightClicked();
                if (target != null) {
                    // Check if there are remaining uses left
                    if (getRemainingUses(player) <= 0) {
                        player.sendMessage(ChatColor.RED + "You have used all the remaining uses of the Poison Stick!");
                        return;
                    }

                    // If there are remaining uses, poison the entity
                    poisonEntity(target);
                    updateRemainingUses(player);
                    updateLastUsedTime(player);
                }
            }
        }
    }

    private void poisonEntity(Entity entity) {
        // Implement poisoning logic for the targeted entity
        // For example, you can give them poison effect for a duration
        // Here's a simple example:
        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;
            livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisonDuration * 20, 2));

            // Log the application of the poison effect
            getLogger().info("Poison effect applied to " + livingEntity.getName() + " for " + poisonDuration + " seconds.");
        }
    }

    private boolean checkCooldown(Player player) {
        if (!lastUsedTimeMap.containsKey(player)) {
            return false;
        }
        long lastUsedTime = lastUsedTimeMap.get(player);
        long currentTime = System.currentTimeMillis() / 1000; // Convert to seconds
        return (currentTime - lastUsedTime) < cooldownSeconds;
    }

    private void updateRemainingUses(Player player) {
        int remainingUses = getRemainingUses(player);
        if (remainingUses > 0) {
            remainingUsesMap.put(player, remainingUses - 1);

            // Get the ItemStack from player's main hand
            ItemStack item = player.getInventory().getItemInMainHand();

            // Check if the ItemStack is a Poison Stick and has ItemMeta
            if (item != null && item.getType() == Material.STICK && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();

                // Check if the ItemMeta has lore
                if (meta.hasLore()) {
                    List<String> lore = meta.getLore();

                    // Ensure there are at least 2 lines in the lore
                    if (lore.size() >= 2) {
                        // Update the second line of the lore with remaining uses
                        lore.set(1, ChatColor.translateAlternateColorCodes('&', "Remaining Uses: " + (remainingUses - 1) + "/" + maxUses));

                        // Set the updated lore to the ItemMeta
                        meta.setLore(lore);

                        // Apply the updated ItemMeta to the ItemStack
                        item.setItemMeta(meta);

                        // Update the ItemStack in the player's inventory
                        player.getInventory().setItemInMainHand(item);
                    }
                }
            }
        }
    }

    private int getRemainingUses(Player player) {
        return remainingUsesMap.getOrDefault(player, maxUses);
    }

    private void updateLastUsedTime(Player player) {
        lastUsedTimeMap.put(player, System.currentTimeMillis() / 1000); // Convert to seconds
    }
}

    git remote set-url origin https://github.com/el211/AdvancedPoisonStick
        git remote remove origin repository_URL
