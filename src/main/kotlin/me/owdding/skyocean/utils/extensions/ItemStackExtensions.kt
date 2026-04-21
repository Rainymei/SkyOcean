package me.owdding.skyocean.utils.extensions

import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.level.ItemLike

fun ItemLike.getEquipmentSlot() = asItem().components().get(DataComponents.EQUIPPABLE)?.slot()

operator fun Item.contains(item: ItemStackTemplate) = item.`is`(this)

//? >= 26.1 {
fun Item.asTemplate() = ItemStackTemplate(this)
fun ItemStack.asTemplate() = ItemStackTemplate(this.typeHolder(), this.count, this.componentsPatch)
fun ItemStackTemplate.instantiate(): ItemStack = this.create()
//? } else {
/*fun Item.asTemplate() = ItemStack(this)
fun ItemStack.asTemplate() = this
fun ItemStack.instantiate() = this
*///? }
