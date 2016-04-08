package de.randombyte.teleportcompass;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.type.Exclude;
import org.spongepowered.api.item.ItemTypes;

public class ExcludedListenerJava {

    @Listener
    @Exclude(InteractBlockEvent.class)
    public void onRightClickAir(InteractEvent event, @First Player player) {
        if (!player.hasPermission(TeleportCompass.TELEPORT_PERMISSION)) {
            player.sendMessage(TeleportCompass.Companion.getPERMISSION_DENIED());
            return;
        }
        if (TeleportCompass.Companion.itemInHand(player, ItemTypes.COMPASS)) {
            TeleportCompass.Companion.teleportInDirection(player, 100);
        }
    }

}
