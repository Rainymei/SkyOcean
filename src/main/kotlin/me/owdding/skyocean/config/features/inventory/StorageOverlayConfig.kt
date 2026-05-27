package me.owdding.skyocean.config.features.inventory

import com.teamresourceful.resourcefulconfig.api.types.info.Translatable
import com.teamresourceful.resourcefulconfigkt.api.ObjectKt

object StorageOverlayConfig : ObjectKt(), Translatable {
    private const val PATH = "skyocean.config.inventory.storage_overlay"
    override fun getTranslationKey(): String = "$PATH.edit"

    var enabled by boolean(true) {
        translation = "$PATH.enabled"
        this.searchTerms += listOf("storage", "overlay", "enderchest", "backpack", "ec", "bp")
    }

    var columns by int(3) {
        translation = "$PATH.columns"
        range = 1..10
        slider = true
    }

    var height by int(3 * 18 * 6) {
        translation = "$PATH.height"
        range = 80..3000
        slider = true
    }

    var scrollSpeed by int(10) {
        translation = "$PATH.scroll_speed"
        range = 1..50
        slider = true
    }

    var inverseScroll by boolean(false) {
        translation = "$PATH.inverse_scroll"
    }
}
