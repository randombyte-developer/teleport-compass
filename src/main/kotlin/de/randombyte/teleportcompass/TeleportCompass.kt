package de.randombyte.teleportcompass

import com.google.inject.Inject
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.loader.ConfigurationLoader
import org.slf4j.Logger
import org.spongepowered.api.block.BlockTypes
import org.spongepowered.api.config.DefaultConfig
import org.spongepowered.api.data.type.HandTypes
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.block.InteractBlockEvent
import org.spongepowered.api.event.filter.cause.First
import org.spongepowered.api.event.game.GameReloadEvent
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
class TeleportCompass @Inject constructor(val logger: Logger) {

    @Inject
    @DefaultConfig(sharedRoot = true)
    lateinit var configLoader: ConfigurationLoader<CommentedConfigurationNode>

    var maxTeleportDistance = 100

    @Listener
    fun onInit(event: GameInitializationEvent) {
        loadConfig()
        logger.info("$NAME loaded: $VERSION!")
    }

    @Listener
    fun onReload(event: GameReloadEvent) {
        loadConfig()
    }

    fun loadConfig() {
        val rootNode = configLoader.load()
        val maxDistanceNode = rootNode.getNode("maxTeleportDistance")
        if (maxDistanceNode.isVirtual) maxDistanceNode.value = DEFAULT_MAX_TELEPORT_DISTANCE
        maxTeleportDistance = maxDistanceNode.int
        configLoader.save(rootNode)
    }

    @Listener
    fun onInteractCompass(event: InteractBlockEvent, @First player: Player) {
        if (teleportValid(player)) {
            if (event.targetBlock.location.isPresent) {
                // Interact with non air block
                teleportOnTopOfBlocks(player, event.targetBlock.location.get())
                if (event is InteractBlockEvent.Primary) {
                    // Destroyed a block by left clicking, prevent that
                    event.isCancelled = true
                }
            } else {
                // Interact with air block
                teleportInDirection(player, maxTeleportDistance)
            }
        }
    }

    companion object {

        const val NAME = "TeleportCompass"
        const val ID = "teleportcompass"
        const val VERSION = "v1.1"
        const val AUTHOR = "RandomByte"

        val TELEPORT_PERMISSION = "teleportcompass.use"
        val PERMISSION_DENIED = Text.of(TextColors.RED, "You don't have permission to teleport!")

        val DEFAULT_MAX_TELEPORT_DISTANCE = 100

        fun teleportValid(player: Player) = isItemInHand(player, ItemTypes.COMPASS) && testTeleportPermission(player)

        /**
         * Uses [BlockRay] to teleport the [player] in the direction he is looking at. [teleportLimit] limits how far the
         * player can be teleported at most.
         */
        fun teleportInDirection(player: Player, teleportLimit: Int) {
            val hitOpt = BlockRay.
                    from(player)
                    .distanceLimit(teleportLimit.toDouble())
                    .stopFilter(BlockRay.continueAfterFilter(BlockRay.onlyAirFilter(), 1))
                    .end()
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
            } while(teleportLoc.blockType != BlockTypes.AIR // Check teleportLoc and one block above for air,
                    || teleportLoc.add(0.0, 1.0, 0.0).blockType != BlockTypes.AIR) // so the player can be teleported there
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
         * @return Whether the [player] has the [itemType] in hand
         */
        fun isItemInHand(player: Player, itemType: ItemType): Boolean =
                player.getItemInHand(HandTypes.MAIN_HAND).run { isPresent && get().item == itemType }
    }
}