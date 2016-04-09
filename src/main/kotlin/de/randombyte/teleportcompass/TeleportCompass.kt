package de.randombyte.teleportcompass

import com.google.inject.Inject
import org.slf4j.Logger
import org.spongepowered.api.block.BlockTypes
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.action.InteractEvent
import org.spongepowered.api.event.block.InteractBlockEvent
import org.spongepowered.api.event.filter.cause.First
import org.spongepowered.api.event.filter.type.Exclude
import org.spongepowered.api.event.game.state.GameInitializationEvent
import org.spongepowered.api.item.ItemType
import org.spongepowered.api.item.ItemTypes
import org.spongepowered.api.plugin.Plugin
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors
import org.spongepowered.api.util.blockray.BlockRay
import org.spongepowered.api.world.Location
import org.spongepowered.api.world.World

@Plugin(id = TeleportCompass.ID, name = TeleportCompass.NAME, version = TeleportCompass.VERSION,
        authors = arrayOf(TeleportCompass.AUTHOR))
class TeleportCompass @Inject constructor(private val logger: Logger) {

    @Listener
    fun onInit(event: GameInitializationEvent) {
        logger.info("${TeleportCompass.NAME} loaded: ${TeleportCompass.ID}!")
    }

    @Listener
    @Exclude(InteractBlockEvent::class) //Seems not to work: https://github.com/SpongePowered/SpongeCommon/issues/643
    fun onRightClickAir(event: InteractEvent, @First player: Player) {
        if (TeleportCompass.itemInHand(player, ItemTypes.COMPASS) && testTeleportPermission(player)) {
            TeleportCompass.teleportInDirection(player, 100)
        }
    }

    @Listener
    fun onRightClickBlock(event: InteractBlockEvent.Secondary, @First player: Player) {
        if (TeleportCompass.itemInHand(player, ItemTypes.COMPASS) && event.targetBlock.location.isPresent
                && testTeleportPermission(player)) {
            teleportOnTopOfBlocks(player, event.targetBlock.location.get())
        }
    }

    companion object {

        const val NAME = "TeleportCompass"
        const val ID = "de.randombyte.teleportcompass"
        const val VERSION = "v0.1.1"
        const val AUTHOR = "RandomByte"

        const val TELEPORT_PERMISSION = "teleportcompass.use"
        val PERMISSION_DENIED = Text.of(TextColors.RED, "You don't have permission to teleport!")

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
         * Sends a [PERMISSION_DENIED] message to [player] if he doesn't have [TELEPORT_PERMISSION].
         * @return If the player has permission
         */
        fun testTeleportPermission(player: Player): Boolean {
            return if (!player.hasPermission(TELEPORT_PERMISSION)) {
                player.sendMessage(PERMISSION_DENIED)
                false
            } else true
        }

        /**
         * @return Whether the [player] has the [item] in hand
         */
        fun itemInHand(player: Player, item: ItemType): Boolean =
                player.itemInHand.isPresent && player.itemInHand.get().item.equals(item)
    }
}