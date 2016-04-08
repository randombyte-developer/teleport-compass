package de.randombyte.teleportcompass

import com.google.inject.Inject
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.block.BlockTypes
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.block.InteractBlockEvent
import org.spongepowered.api.event.filter.cause.First
import org.spongepowered.api.event.game.state.GameInitializationEvent
import org.spongepowered.api.item.ItemType
import org.spongepowered.api.item.ItemTypes
import org.spongepowered.api.plugin.Plugin
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors
import org.spongepowered.api.util.blockray.BlockRay
import org.spongepowered.api.world.Location
import org.spongepowered.api.world.World

@Plugin(id = PluginInfo.ID, name = PluginInfo.NAME, version = PluginInfo.VERSION, authors = arrayOf(PluginInfo.AUTHOR))
class TeleportCompass @Inject constructor(private val logger: Logger) {

    @Listener
    fun onInit(event: GameInitializationEvent) {
        Sponge.getEventManager().registerListeners(this, ExcludedListenerJava())
        logger.info("${PluginInfo.NAME} loaded: ${PluginInfo.ID}!")
    }

/*    @Listener
    @Exclude(InteractBlockEvent.Primary::class, InteractBlockEvent.Secondary::class)
    fun onRightClickAir(event: InteractEvent, @First player: Player) {
        if (itemInHand(player, ItemTypes.COMPASS)) {
            teleportInDirection(player, 100) //todo: limit config
        }
    }*/

    @Listener
    fun onRightClickBlock(event: InteractBlockEvent.Secondary, @First player: Player) {
        if (itemInHand(player, ItemTypes.COMPASS) && event.targetBlock.location.isPresent) {
            teleportOnTopOfBlocks(player, event.targetBlock.location.get())
        }
    }

    companion object {
        /**
         * Uses [BlockRay] to teleport the [player] in the direction he is looking at. [teleportLimit] limits how far the
         * player can be teleported at most.
         */
        fun teleportInDirection(player: Player, teleportLimit: Int) {
            val hitOpt = BlockRay.from(player).blockLimit(teleportLimit).filter(BlockRay.onlyAirFilter()).end()
            if (hitOpt.isPresent) {
                player.setLocationSafely(hitOpt.get().location)
            } else {
                player.sendMessage(Text.of(TextColors.RED, "No block in reach!"))
            }
        }

        /**
         * Teleport the [player] safely on the first possible spot above passed [block].
         */
        fun teleportOnTopOfBlocks(player: Player, block: Location<World>) {
            var teleportLoc = block
            do {
                teleportLoc = teleportLoc.add(0.0, 1.0, 0.0)
            } while(!teleportLoc.blockType.equals(BlockTypes.AIR) //Check teleportLoc and one block above for air,
                    || !teleportLoc.add(0.0, 1.0, 0.0).blockType.equals(BlockTypes.AIR)) //so the player can be teleported there
            player.setLocationSafely(teleportLoc)
        }

        /**
         * @return Whether the [player] has the [item] in hand
         */
        fun itemInHand(player: Player, item: ItemType): Boolean =
                player.itemInHand.isPresent && player.itemInHand.get().item.equals(item)
    }
}