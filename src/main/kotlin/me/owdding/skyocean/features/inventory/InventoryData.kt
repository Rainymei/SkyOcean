package me.owdding.skyocean.features.inventory

import com.mojang.serialization.Codec
import me.owdding.ktcodecs.FieldName
import me.owdding.ktcodecs.GenerateCodec
import me.owdding.skyocean.utils.codecs.CodecHelpers
import me.owdding.skyocean.utils.extensions.asTemplate
import me.owdding.skyocean.utils.extensions.instantiate
import me.owdding.skyocean.utils.levelBound
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate

@GenerateCodec
data class DimensionInventory(
    @FieldName("inventory") val inventoryTemplate: MutableList<ItemStackTemplate> = mutableListOf(),
    @FieldName("armour") val armourTemplate: MutableMap<EquipmentSlot, ItemStackTemplate> = mutableMapOf(),
) {
    fun updateInventory(list: List<ItemStack>) {
        inventoryDelegate.invalidate()
        inventoryTemplate.clear()
        inventoryTemplate.addAll(list.map { it.asTemplate() })
    }

    fun updateArmour(slot: EquipmentSlot, item: ItemStack) {
        armourDelegate.invalidate()
        armourTemplate.clear()
        armourTemplate[slot] = item.asTemplate()
    }

    private val inventoryDelegate = levelBound { inventoryTemplate.mapTo(ArrayList()) { it.instantiate() } }
    val inventory: List<ItemStack> by inventoryDelegate
    private val armourDelegate = levelBound { armourTemplate.mapValuesTo(LinkedHashMap(armourTemplate.size)) { (_, value) -> value.instantiate() } }
    val armour: Map<EquipmentSlot, ItemStack> by armourDelegate
}

typealias InventoryData = MutableMap<InventoryType, DimensionInventory>

enum class InventoryType {
    NORMAL,
    RIFT,
    ;

    companion object {
        val CODEC: Codec<InventoryData> = CodecHelpers.mutableMap()
    }
}
