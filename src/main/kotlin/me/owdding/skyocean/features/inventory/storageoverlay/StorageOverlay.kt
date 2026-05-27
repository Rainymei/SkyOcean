package me.owdding.skyocean.features.inventory.storageoverlay

import me.owdding.ktmodules.Module
import me.owdding.skyocean.config.features.inventory.StorageOverlayConfig
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.misc.RegisterCommandsEvent
import tech.thatgravyboat.skyblockapi.api.events.screen.ScreenInitializedEvent
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.utils.text.Text
import tech.thatgravyboat.skyblockapi.utils.text.Text.send

@Module
object StorageOverlay {

    private var overlayActive = false

    @Subscription(inherited = true)
    fun onScreenInit(event: ScreenInitializedEvent) {
        if (!StorageOverlayConfig.enabled) return
        val screen = event.screen
        val handle = StorageBackingHandle.fromScreen(screen) ?: return

        when (handle) {
            is StorageBackingHandle.Overview -> {
                overlayActive = true
                McClient.setScreenAsync { StorageOverlayScreen(activePage = null) }
            }

            is StorageBackingHandle.Page -> {
                if (!overlayActive) return
                McClient.setScreenAsync { StorageOverlayScreen(activePage = handle.storagePageSlot) }
            }
        }
    }

    @Subscription
    fun onCommand(event: RegisterCommandsEvent) {
        event.register("storageoverlay") {
            callback {
                if (!StorageOverlayConfig.enabled) return@callback
                overlayActive = true
                McClient.setScreenAsync { StorageOverlayScreen(activePage = null) }
            }

            thenCallback("open") {
                if (!StorageOverlayConfig.enabled) return@thenCallback
                overlayActive = true
                McClient.setScreenAsync { StorageOverlayScreen(activePage = null) }
            }

            thenCallback("help") {
                Text.of("Usage: /storageoverlay (opens the storage overlay UI)").send()
            }
        }
    }
}
