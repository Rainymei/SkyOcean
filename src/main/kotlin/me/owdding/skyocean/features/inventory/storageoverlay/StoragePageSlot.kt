

package me.owdding.skyocean.features.inventory.storageoverlay

import com.mojang.serialization.Codec
import tech.thatgravyboat.skyblockapi.helpers.McClient

/**
 * Represents one of the `/storage` pages.
 *
 * Index mapping matches Firmament's original layout:
 * - 0..8   = Enderchest pages 1..9
 * - 9..26  = Backpack pages 1..18
 */
data class StoragePageSlot(val index: Int) : Comparable<StoragePageSlot> {

    init {
        require(index in 0 until (3 * 9)) { "StoragePageSlot index out of range: $index" }
    }

    val isEnderChest get() = index < 9
    val isBackPack get() = !isEnderChest

    fun defaultName(): String = if (isEnderChest) "Ender Chest #${index + 1}" else "Backpack #${index - 9 + 1}"

    fun navigateTo() {
        if (isBackPack) {
            McClient.sendCommand("/bp ${index - 9 + 1}")
        } else {
            McClient.sendCommand("/ec ${index + 1}")
        }
    }

    companion object {
        val CODEC: Codec<StoragePageSlot> = Codec.INT.xmap(::StoragePageSlot, StoragePageSlot::index)

        fun ofEnderChestPage(slot: Int): StoragePageSlot {
            require(slot in 1..9)
            return StoragePageSlot(slot - 1)
        }

        fun ofBackPackPage(slot: Int): StoragePageSlot {
            require(slot in 1..18)
            return StoragePageSlot(slot - 1 + 9)
        }
    }

    override fun compareTo(other: StoragePageSlot): Int = this.index - other.index
}
