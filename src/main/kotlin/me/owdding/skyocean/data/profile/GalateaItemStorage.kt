package me.owdding.skyocean.data.profile

import me.owdding.ktcodecs.FieldName
import me.owdding.ktcodecs.GenerateCodec
import me.owdding.ktmodules.Module
import me.owdding.skyocean.generated.SkyOceanCodecs
import me.owdding.skyocean.utils.Utils.containerItems
import me.owdding.skyocean.utils.extensions.asTemplate
import me.owdding.skyocean.utils.extensions.instantiate
import me.owdding.skyocean.utils.levelBound
import me.owdding.skyocean.utils.storage.ProfileStorage
import me.owdding.skyocean.utils.withSetter
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyOnSkyBlock
import tech.thatgravyboat.skyblockapi.api.events.screen.InventoryChangeEvent
import tech.thatgravyboat.skyblockapi.utils.extentions.getSkyBlockId

@Module
object GalateaItemStorage {
    private val storage: ProfileStorage<GalateaItems> = ProfileStorage(
        defaultData = { GalateaItems.DEFAULT },
        fileName = "galatea_items",
        codec = { SkyOceanCodecs.GalateaItemsCodec.codec() },
    )

    val data get() = storage.get()

    @Subscription
    @OnlyOnSkyBlock
    private fun InventoryChangeEvent.onInventoryChange() {
        val items = inventory.containerItems()
        when {
            title.contains("Huntaxe") -> {
                val huntaxeItem = items.firstOrNull { it.getSkyBlockId() != null }
                storage.get()?.huntaxeItem = huntaxeItem
                storage.save()
            }

            title.contains("Hunting Toolkit") -> {
                val toolkitItems =
                    items.filter { it.getSkyBlockId() != null && it.getSkyBlockId() != "HUNTING_TOOLKIT" }
                storage.get()?.toolkitItems = toolkitItems
                storage.save()
            }
        }
    }
}

@GenerateCodec
data class GalateaItems(
    @FieldName("huntaxeItem") var huntaxeItemTemplate: ItemStackTemplate?,
    @FieldName("toolkitItems") var toolkitItemsTemplate: List<ItemStackTemplate>,
) {
    //? >= 26.1
    constructor(huntaxeItem: ItemStack, toolkitItems: List<ItemStack>) : this(huntaxeItem.asTemplate(), toolkitItems.map { it.asTemplate() })

    var huntaxeItem by levelBound { huntaxeItemTemplate?.instantiate() }.withSetter {
        huntaxeItemTemplate = it?.asTemplate()
    }
    var toolkitItems by levelBound { toolkitItemsTemplate.map { it.instantiate() } }.withSetter {
        toolkitItemsTemplate = it.map { it.asTemplate() }
    }

    companion object {
        val DEFAULT = GalateaItems(null, emptyList())
    }
}
