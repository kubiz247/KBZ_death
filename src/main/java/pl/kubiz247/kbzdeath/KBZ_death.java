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

import java.util.Map;

public class KBZ_death extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
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
            displayName = Component.text("gołej ręki", NamedTextColor.GRAY);
        } else {
            if (meta != null && meta.hasDisplayName()) {
                displayName = LegacyComponentSerializer.legacyAmpersand().deserialize(meta.getDisplayName());
            } else {
                displayName = Component.text(formatMaterialName(item.getType()), NamedTextColor.WHITE);
            }

            if (meta != null && meta.hasEnchants() && !meta.hasDisplayName()) {
                displayName = Component.text(formatMaterialName(item.getType()), NamedTextColor.AQUA);
            }

            if (meta != null && (meta.hasEnchants() || meta.hasLore())) {
                hasHover = true;

                if (meta.hasDisplayName()) {
                    hoverText = hoverText.append(LegacyComponentSerializer.legacyAmpersand().deserialize(meta.getDisplayName()));
                } else {
                    hoverText = hoverText.append(displayName);
                }

                hoverText = hoverText.append(Component.newline());

                if (meta.hasLore()) {
                    for (String line : meta.getLore()) {
                        hoverText = hoverText.append(
                                LegacyComponentSerializer.legacyAmpersand().deserialize(line)
                        ).append(Component.newline());
                    }
                }

                for (Map.Entry<Enchantment, Integer> enchant : meta.getEnchants().entrySet()) {
                    String enchantName = capitalize(enchant.getKey().getKey().getKey().replace("_", " "));
                    String level = toRoman(enchant.getValue());
                    hoverText = hoverText.append(
                            Component.text(enchantName + " " + level, NamedTextColor.GRAY)
                    ).append(Component.newline());
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

        Component deathMessage = Component.empty()
                .append(killerComponent)
                .append(Component.text(" zabił ", NamedTextColor.GRAY))
                .append(victimComponent)
                .append(Component.text(" przy pomocy ", NamedTextColor.GRAY))
                .append(displayName);

        event.deathMessage(deathMessage);
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

//by kubiz