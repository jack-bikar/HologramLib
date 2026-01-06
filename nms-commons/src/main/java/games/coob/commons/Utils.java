package games.coob.commons;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

/**
 * Utility class to replace Foundation's Remain methods.
 * Provides cross-version compatibility for common operations.
 */
public final class Utils {

    private Utils() {
    }

    /**
     * Gets a player by their UUID.
     *
     * @param uuid the player's UUID
     * @return the player if online, null otherwise
     */
    public static Player getPlayerByUUID(final UUID uuid) {
        try {
            final Player player = Bukkit.getPlayer(uuid);
            return player != null && player.isOnline() ? player : null;
        } catch (NoSuchMethodError e) {
            // Fallback for very old versions
            for (final Player online : Bukkit.getOnlinePlayers()) {
                if (online.getUniqueId().equals(uuid)) {
                    return online;
                }
            }
            return null;
        }
    }

    /**
     * Gets all online players.
     *
     * @return collection of online players
     */
    public static Collection<? extends Player> getOnlinePlayers() {
        try {
            final Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            return players;
        } catch (NoSuchMethodError e) {
            // Fallback for very old versions (shouldn't happen with MC 1.17+)
            throw new RuntimeException("getOnlinePlayers() not available on this server version");
        }
    }

}
