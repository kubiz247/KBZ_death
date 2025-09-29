package pl.kubiz247.kbzdeath;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KBZ_death extends JavaPlugin implements Listener {

    private static final String DEFAULT_CONFIG =
            "# change to pl for Polish\n" +
            "language: en\n";

    @Override
    public void onEnable() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            boolean created = dataFolder.mkdirs();
            if (!created) {
                getLogger().warning("KBZ_death: failed to create data folder: " + dataFolder.getAbsolutePath());
            }
        }

        File configFile = new File(dataFolder, "config.yml");
        if (!configFile.exists()) {
            try (FileWriter fw = new FileWriter(configFile)) {
                fw.write(DEFAULT_CONFIG);
                getLogger().info("KBZ_death: default config.yml created at " + configFile.getAbsolutePath());
            } catch (IOException e) {
                getLogger().severe("KBZ_death: failed to write default config.yml: " + e.getMessage());
                e.printStackTrace();
            }
        }

        reloadConfig();

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("KBZ_death enabled. Language: " + getConfig().getString("language", "en"));

        if (getCommand("kbzdeath") != null)
        {
            getCommand("kbzdeath").setTabCompleter((sender, command, alias, args) -> {
                if (args.length == 1)
                {
                    String partial = args[0].toLowerCase();

                    boolean canReload = sender instanceof ConsoleCommandSender
                        || sender.isOp()
                        || sender.hasPermission("kbzdeath.reload")
                        || sender.hasPermission("kbzdeath.*");

                    if (canReload)
                    {
                        List<String> subs = Arrays.asList("reload");
                        return subs.stream()
                                .filter(s -> s.startsWith(partial))
                                .collect(Collectors.toList());
                    }
                }
                return Collections.emptyList();
            });
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("kbzdeath")) return false;

        boolean isConsole = sender instanceof ConsoleCommandSender;
        boolean isOp = sender.isOp();
        boolean hasAllPerm = sender.hasPermission("kbzdeath.*");
        boolean canReloadPerm = sender.hasPermission("kbzdeath.reload");

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!(isConsole || isOp || canReloadPerm || hasAllPerm)) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to run this command.");
                return true;
            }

            reloadConfig();

            String lang = getConfig().getString("language", "en");
            if (!"en".equalsIgnoreCase(lang) && !"pl".equalsIgnoreCase(lang)) {
                getLogger().warning("Invalid language in config.yml: " + lang + " - falling back to 'en'");
                getConfig().set("language", "en");
                saveConfig();
                lang = "en";
            }

            sender.sendMessage(ChatColor.GREEN + "KBZ_death: config reloaded. Language: " + ChatColor.AQUA + lang);
            getLogger().info("KBZ_death config reloaded by " + (isConsole ? "CONSOLE" : sender.getName()) + ". Language: " + lang);
            return true;
        }

        if (!(isConsole || isOp || hasAllPerm)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to view KBZ_death commands.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "KBZ_death commands:");
        sender.sendMessage(ChatColor.AQUA + "/kbzdeath reload" + ChatColor.WHITE + " - reload plugin config");
        return true;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) return;

        ItemStack item = killer.getInventory().getItemInMainHand();
        ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : null;

        Component displayName;
        Component hoverText = Component.empty();
        boolean hasHover = false;
        boolean isHand = item.getType() == Material.AIR;

        if (isHand) {
            displayName = getMessageComponent("handName", NamedTextColor.GRAY);
        } else {
            if (meta != null && meta.hasDisplayName()) {
                String rawName = meta.getDisplayName();
                if (rawName.contains("&")) {
                    displayName = LegacyComponentSerializer.legacyAmpersand().deserialize(rawName);
                } else {
                    displayName = Component.text().content(rawName).build();
                }
            } else {
                displayName = Component.text(formatMaterialName(item.getType()), NamedTextColor.WHITE);
                if (meta != null && meta.hasEnchants()) {
                    displayName = Component.text(formatMaterialName(item.getType()), NamedTextColor.AQUA);
                }
            }

            if (meta != null && (meta.hasEnchants() || meta.hasLore())) {
                hasHover = true;

                hoverText = hoverText.append(displayName).append(Component.newline());

                for (Map.Entry<Enchantment, Integer> enchant : meta.getEnchants().entrySet()) {
                    Enchantment ench = enchant.getKey();
                    int level = enchant.getValue();

                    String enchantName = capitalize(ench.getKey().getKey().replace("_", " "));

                    if (ench.getMaxLevel() == 1) {
                        hoverText = hoverText.append(
                                Component.text(enchantName, NamedTextColor.GRAY)
                        ).append(Component.newline());
                    } else {
                        hoverText = hoverText.append(
                                Component.text(enchantName + " " + toRoman(level), NamedTextColor.GRAY)
                        ).append(Component.newline());
                    }
                }


                if (meta.hasLore()) {
                    for (String line : meta.getLore()) {
                        hoverText = hoverText.append(
                                LegacyComponentSerializer.legacyAmpersand().deserialize(line)
                        ).append(Component.newline());
                    }
                }
            }

            displayName = Component.empty()
                    .append(Component.text("[", NamedTextColor.DARK_GRAY))
                    .append(hasHover ? displayName.hoverEvent(HoverEvent.showText(hoverText)) : displayName)
                    .append(Component.text("]", NamedTextColor.DARK_GRAY));
        }

        Component killerComponent = Component.text("🗡 ", TextColor.fromHexString("#ecb578"))
                .append(Component.text(killer.getName(), TextColor.fromHexString("#40f49f")));
        Component victimComponent = Component.text("☠ ", TextColor.fromHexString("#ecb578"))
                .append(Component.text(victim.getName(), TextColor.fromHexString("#ec7063")));

        Component killedTextComp = getMessageComponent("killed", NamedTextColor.GRAY);
        Component withTextComp = getMessageComponent("with", NamedTextColor.GRAY);

        Component deathMessage = Component.empty()
                .append(killerComponent)
                .append(Component.text(" "))
                .append(killedTextComp)
                .append(Component.text(" "))
                .append(victimComponent)
                .append(Component.text(" "))
                .append(withTextComp)
                .append(Component.text(" "))
                .append(displayName);

        event.deathMessage(deathMessage);
    }

    private Component getMessageComponent(String key, NamedTextColor defaultColor) {
        String lang = getConfig().getString("language", "en");
        if ("pl".equalsIgnoreCase(lang)) {
            switch (key) {
                case "handName": return Component.text("gołej ręki", defaultColor);
                case "killed": return Component.text("zabił", defaultColor);
                case "with": return Component.text("przy pomocy", defaultColor);
                default: return Component.text(key, defaultColor);
            }
        } else {
            switch (key) {
                case "handName": return Component.text("bare hands", defaultColor);
                case "killed": return Component.text("killed", defaultColor);
                case "with": return Component.text("with", defaultColor);
                default: return Component.text(key, defaultColor);
            }
        }
    }

    private String formatMaterialName(Material material) {
        String name = material.toString().toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            formatted.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1)).append(" ");
        }
        return formatted.toString().trim();
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return "";
        return Character.toUpperCase(text.charAt(0)) + text.substring(1).toLowerCase();
    }

    private String toRoman(int number) {
        if (number <= 0 || number > 20) return Integer.toString(number);

        String[] romans = {
                "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
                "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX"
        };
        return romans[number - 1];
    }
}
