package games.coob.hologram;

import games.coob.commons.Hologram;
import games.coob.commons.HologramRegistry;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import games.coob.commons.Utils;

/**
 * Task that periodically updates the visibility of all registered holograms for every online player.
 * This class extends {@link BukkitRunnable} to facilitate periodic execution within the Bukkit scheduler.
 * On each run, it iterates through all holograms registered in {@link HologramRegistry} and updates their
 * visibility based on each player's location, permissions, and other visibility criteria defined in {@link Hologram}.
 */
@RequiredArgsConstructor
public final class HologramTask extends BukkitRunnable {

    /**
     * Executes the task to update hologram visibility.
     * This method is called automatically when the task is scheduled to run.
     */
    @Override
    public void run() {
        for (final HologramRegistry registry : HologramRegistry.getHolograms()) {
            final Hologram hologramAPI = registry.getHologram();

            for (final Player player : Utils.getOnlinePlayers())
                hologramAPI.updateVisibility(player);
        }
    }
}
