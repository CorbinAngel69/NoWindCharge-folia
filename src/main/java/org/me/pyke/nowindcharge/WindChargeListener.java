package org.me.pyke.nowindcharge;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import java.util.List;

public class WindChargeListener implements Listener {

    private final NoWindCharge plugin;

    public WindChargeListener(NoWindCharge plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerUseWindCharge(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        ItemStack offHandItem = player.getInventory().getItemInOffHand();

        if ((mainHandItem.getType() != Material.WIND_CHARGE &&
             offHandItem.getType() != Material.WIND_CHARGE) ||
            (event.getAction() != Action.RIGHT_CLICK_AIR &&
             event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        if (plugin.getConfig().getBoolean("disable-completely", false)) {
            event.setCancelled(true);
            player.sendMessage(plugin.formatMessage(
                    plugin.getConfig().getString(
                            "completely-disabled-message",
                            "&cWIND_CHARGE usage is completely disabled!"
                    )));
            return;
        }

        if (player.hasPermission("nowindcharge.bypass")) {
            return;
        }

        if (isInExcludedRegion(player)) {
            event.setCancelled(true);
            String message = plugin.getConfig().getString(
                    "deny-message",
                    "&cYou cannot use this item in region &e%region%&c!"
            );
            player.sendMessage(plugin.formatMessage(
                    message.replace("%region%", getRegionName(player))
            ));
        }
    }

    @EventHandler
    public void onWindChargeExplosion(ExplosionPrimeEvent event) {
        if (plugin.getConfig().getBoolean("disable-completely", false)) {
            event.setCancelled(true);
            return;
        }

        if (event.getEntity().getType() != EntityType.WIND_CHARGE &&
            event.getEntity().getType() != EntityType.BREEZE_WIND_CHARGE) {
            return;
        }

        if (event.getEntity().getNearbyEntities(4, 4, 4).stream()
                .anyMatch(e -> e instanceof Player player && isInExcludedRegion(player))) {
            event.setCancelled(true);
        }
    }

    private boolean isInExcludedRegion(Player player) {
        List<String> excludedRegions = plugin.getConfig().getStringList("excluded-regions");

        RegionManager regionManager = com.sk89q.worldguard.WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(player.getWorld()));

        if (regionManager == null) return false;

        ApplicableRegionSet regions =
                regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(player.getLocation()));

        for (ProtectedRegion region : regions) {
            if (excludedRegions.contains(region.getId())) {
                return true;
            }
        }
        return false;
    }

    private String getRegionName(Player player) {
        RegionManager regionManager = com.sk89q.worldguard.WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(player.getWorld()));

        if (regionManager == null) return "unknown";

        ApplicableRegionSet regions =
                regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(player.getLocation()));

        for (ProtectedRegion region : regions) {
            return region.getId();
        }
        return "unknown";
    }
}
