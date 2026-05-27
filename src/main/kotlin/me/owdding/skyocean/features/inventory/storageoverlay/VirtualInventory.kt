package me.owdding.skyocean.features.inventory.storageoverlay

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.owdding.skyocean.utils.codecs.CodecHelpers
import net.minecraft.world.item.ItemStack

data class VirtualInventory(
    val stacks: List<ItemStack>,
) {
    val rows: Int = stacks.size / 9

    init {
        require(stacks.size % 9 == 0) { "VirtualInventory stacks must be a multiple of 9, got ${stacks.size}" }
        // Storage pages in SkyBlock are <= 5 rows, but keep the upper bound slightly loose for future-proofing.
        require(rows in 1..6) { "VirtualInventory rows out of expected range: $rows" }
    }

    companion object {
        val CODEC: Codec<VirtualInventory> = RecordCodecBuilder.create { instance ->
            instance.group(
                CodecHelpers.ITEM_STACK_CODEC.listOf().fieldOf("stacks").forGetter(VirtualInventory::stacks),
            ).apply(instance, ::VirtualInventory)
        }
    }
}
