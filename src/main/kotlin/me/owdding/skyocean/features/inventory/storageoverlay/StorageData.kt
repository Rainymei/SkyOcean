package me.owdding.skyocean.features.inventory.storageoverlay

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

data class StorageData(
    val storageInventories: Map<StoragePageSlot, StorageInventory> = sortedMapOf(),
) {

    data class StorageInventory(
        val title: String,
        val slot: StoragePageSlot,
        val inventory: VirtualInventory?,
    )

    companion object {
        private val INVENTORY_CODEC: Codec<StorageInventory> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("title").forGetter(StorageInventory::title),
                StoragePageSlot.CODEC.fieldOf("slot").forGetter(StorageInventory::slot),
                VirtualInventory.CODEC.optionalFieldOf("inventory").forGetter { java.util.Optional.ofNullable(it.inventory) },
            ).apply(instance) { title, slot, invOpt ->
                StorageInventory(title, slot, invOpt.orElse(null))
            }
        }

        val CODEC: Codec<StorageData> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.unboundedMap(StoragePageSlot.CODEC, INVENTORY_CODEC)
                    .optionalFieldOf("storageInventories", sortedMapOf())
                    .forGetter(StorageData::storageInventories),
            ).apply(instance, ::StorageData)
        }
    }
}
