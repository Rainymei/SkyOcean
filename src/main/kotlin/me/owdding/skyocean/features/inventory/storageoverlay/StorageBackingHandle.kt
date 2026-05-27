package me.owdding.skyocean.features.inventory.storageoverlay

import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.AbstractContainerMenu
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped

/**
 * A lightweight representation of the currently open "server side" storage screen.
 *
 * This is mostly used for deciding whether the overlay should be offered for the current screen.
 */
sealed interface StorageBackingHandle {

    val handler: AbstractContainerMenu

    data class Overview(override val handler: AbstractContainerMenu) : StorageBackingHandle
    data class Page(override val handler: AbstractContainerMenu, val storagePageSlot: StoragePageSlot) : StorageBackingHandle

    companion object {
        private val enderChestName = "^Ender Chest (?:✦ )?\\(([1-9])/[1-9]\\)$".toRegex()
        private val backPackName = "^.+Backpack (?:✦ )?\\(Slot #([0-9]+)\\)$".toRegex()

        fun fromScreen(screen: Screen?): StorageBackingHandle? {
            val handled = screen as? AbstractContainerScreen<*> ?: return null
            val title = handled.title.stripped

            if (title == "Storage") return Overview(handled.menu)

            enderChestName.matchEntire(title)?.let { match ->
                return Page(handled.menu, StoragePageSlot.ofEnderChestPage(match.groupValues[1].toInt()))
            }
            backPackName.matchEntire(title)?.let { match ->
                return Page(handled.menu, StoragePageSlot.ofBackPackPage(match.groupValues[1].toInt()))
            }
            return null
        }
    }
}
