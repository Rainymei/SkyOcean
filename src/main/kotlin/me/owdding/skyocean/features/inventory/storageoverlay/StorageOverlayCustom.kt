package me.owdding.skyocean.features.inventory.storageoverlay

import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.utils.text.Text
import tech.thatgravyboat.skyblockapi.utils.extentions.left
import tech.thatgravyboat.skyblockapi.utils.extentions.top
import me.owdding.skyocean.mixins.ScreenAccessor

/**
 * Minimal "custom gui" adapter for SkyOcean.
 *
 * Firmament injects a full custom overlay into handled screens; SkyOcean instead attaches
 * a small button that opens a dedicated overlay screen.
 */
internal object StorageOverlayCustom {

    fun attach(screen: AbstractContainerScreen<*>) {
        val handle = StorageBackingHandle.fromScreen(screen) ?: return
        if (handle !is StorageBackingHandle.Overview && handle !is StorageBackingHandle.Page) return

        val button = Button.builder(Text.of("Overlay")) {
            McClient.setScreen(StorageOverlayScreen(activePage = null))
        }.bounds(screen.left + 2, screen.top + 2, 50, 14).build()

        (screen as ScreenAccessor).`skyocean$addRenderableWidget`(button)
    }
}
