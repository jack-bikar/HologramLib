package games.coob.v1_18;

import games.coob.commons.Hologram;
import games.coob.commons.HologramRegistry;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftChatMessage;
import org.bukkit.entity.Player;
import games.coob.commons.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * A concrete implementation of {@link Hologram} for Minecraft version 1.19.
 * This class provides version-specific logic for creating, displaying, and managing holograms.
 */
public class Hologram_v1_18 extends Hologram {

    /**
     * A list of ArmorStand entities used to display the lines of the hologram.
     */
    private final List<ArmorStand> entityLinesList = new ArrayList<>();

    /**
     * Creates an {@link ArmorStand} entity at the specified location to display a line of text.
     * This method leverages Minecraft 1.19's NMS to directly create and configure the entity.
     *
     * @param location The location in the world where the entity should be created.
     * @param line     The text line that the entity will display.
     * @return The created {@link ArmorStand} entity, configured to be invisible, no gravity, and displaying the provided text line.
     */

    @Override
    protected Object createEntity(final Location location, final String line) {
        final ArmorStand armorStand = new ArmorStand(((CraftWorld) getWorld()).getHandle(), location.getX(), location.getY(), location.getZ());
        setupArmorStandEntity(armorStand, line);
        return armorStand;
    }

    /**
     * Sets up the properties of an ArmorStand entity to display a line of text.
     *
     * @param armorStand The ArmorStand entity to set up.
     * @param line       The line of text to display.
     */
    private void setupArmorStandEntity(final ArmorStand armorStand, final String line) {
        armorStand.setMarker(true);
        armorStand.setSmall(true);
        armorStand.setInvisible(true);
        armorStand.setNoGravity(true);
        armorStand.setCustomNameVisible(true);
        armorStand.setCustomName(CraftChatMessage.fromStringOrNull(line));
    }

    /**
     * Sends packets to a player to update the visibility of the hologram.
     * In version 1.19, this method uses {@link ClientboundAddEntityPacket} and other packets
     * to control the visibility of each line represented by ArmorStand entities.
     *
     * @param armorStand The armor stand entity whose visibility is being updated.
     * @param player     The player to whom the packet is sent.
     */
    private static void sendPackets(final ArmorStand armorStand, final Player player) {
        final ServerPlayer nms = ((CraftPlayer) player).getHandle();
        final ServerGamePacketListenerImpl connection = nms.connection;

        connection.send(new ClientboundAddEntityPacket(armorStand));
        connection.send(new ClientboundSetEntityDataPacket(armorStand.getId(), armorStand.getEntityData(), true));
    }

    /**
     * Creates and positions a series of {@link ArmorStand} entities to display multiple lines of text.
     * Each line of text is represented by a separate ArmorStand entity, positioned vertically with a fixed offset.
     *
     * @param location    The starting location for the first text line. Subsequent lines are placed below this point.
     * @param linesOfText A list of strings, each representing a line of text to display as part of the hologram.
     */

    @Override
    protected void createEntityLines(final Location location, final List<String> linesOfText) {
        final Location currentLocation = location.clone();

        for (final String line : linesOfText) {
            final ArmorStand nmsArmorStand = (ArmorStand) this.createEntity(currentLocation, line);
            currentLocation.subtract(0, 0.26, 0); // Move down for the next line
            this.entityLinesList.add(nmsArmorStand);
        }
    }

    /**
     * Displays the hologram to a single player by sending the necessary packets to create the entities client-side.
     * This method ensures that each component of the hologram (each line of text) is visible to the specified player.
     *
     * @param player The player to whom the hologram should be shown. The method does nothing if the player is null or lacks the necessary permission.
     */

    @Override
    public void show(final Player player) {
        if (!canShow(player))
            return;

        this.entityLinesList.forEach(armorStand -> {
            armorStand.valid = true;
            sendPackets(armorStand, player);
        });

        this.getViewers().add(player.getUniqueId());
    }

    /**
     * Hides the hologram from a single player by sending packets to remove the entities from the client-side view.
     * This method does not remove the player from the set of viewers but makes the hologram temporarily invisible to them.
     *
     * @param player The player from whom the hologram should be hidden. If the player is null or not currently viewing the hologram, the method does nothing.
     */

    @Override
    public void hide(final Player player) {
        if (!isViewer(player))
            return;

        final ServerPlayer nms = ((CraftPlayer) player).getHandle();
        final ServerGamePacketListenerImpl connection = nms.connection;

        this.entityLinesList.forEach(armorStand -> {
            armorStand.valid = false;
            connection.send(new ClientboundRemoveEntitiesPacket(armorStand.getId()));
        });

        this.getViewers().remove(player.getUniqueId());
    }

    /**
     * Removes all entities associated with the hologram, effectively deleting the hologram from the world.
     * This method cleans up all resources tied to the hologram and ensures it is no longer visible to any players.
     */

    @Override
    public void remove() {
        for (final UUID uuid : this.getViewers()) {
            final Player player = Utils.getPlayerByUUID(uuid);

            if (player == null)
                continue;

            final ServerPlayer nms = ((CraftPlayer) player).getHandle();
            final ServerGamePacketListenerImpl connection = nms.connection;

            this.entityLinesList.forEach(armorStand -> {
                connection.send(new ClientboundRemoveEntitiesPacket(armorStand.getId()));
                armorStand.discard();
            });
        }

        HologramRegistry.removeHologram(this.getId());
    }

    /**
     * Updates the text lines of the hologram, adjusting the existing entities or creating new ones as necessary.
     * This method allows for dynamic changes to the hologram's content, reflecting the current set of text lines.
     *
     * @param lines The new set of text lines for the hologram. Previous lines will be updated or removed, and new lines will be added as needed.
     */

    @Override
    public void updateLines(final String... lines) {
        this.setLines(Arrays.asList(lines));

        int currentSize = this.entityLinesList.size();
        final int newSize = lines.length;
        final Location baseLocation = this.getLocation().clone();

        for (int i = 0; i < currentSize; i++) {
            if (i < newSize) {
                final ArmorStand armorStand = this.entityLinesList.get(i);
                armorStand.setCustomName(CraftChatMessage.fromStringOrNull(lines[i]));
            } else {
                final ArmorStand armorStand = this.entityLinesList.remove(i--);
                armorStand.discard();

                for (final UUID viewerUUID : getViewers()) {
                    final Player viewer = Utils.getPlayerByUUID(viewerUUID);
                    if (viewer != null) {
                        final ServerPlayer nmsViewer = ((CraftPlayer) viewer).getHandle();
                        nmsViewer.connection.send(new ClientboundRemoveEntitiesPacket(armorStand.getId()));
                    }
                }

                currentSize--;
            }
        }

        if (currentSize > 0) {
            final double yOffset = 0.26 * currentSize;
            baseLocation.subtract(0, yOffset, 0);
        }

        for (int i = currentSize; i < newSize; i++) {
            final Location lineLocation = baseLocation.clone().subtract(0, 0.26 * (i - currentSize), 0);
            final ArmorStand newArmorStand = (ArmorStand) createEntity(lineLocation, lines[i]);
            this.entityLinesList.add(newArmorStand);
        }

        refreshHologramForAllViewers();
    }
}