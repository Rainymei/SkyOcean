package me.owdding.skyocean.features.garden

import me.owdding.ktmodules.Module
import me.owdding.lib.builder.LayoutFactory
import me.owdding.lib.builder.MIDDLE
import me.owdding.lib.displays.Displays
import me.owdding.skyocean.config.features.garden.GardenConfig
import me.owdding.skyocean.helpers.InventorySideGui
import me.owdding.skyocean.utils.Utils.unaryMinus
import me.owdding.skyocean.utils.chat.ChatUtils
import net.minecraft.client.gui.layouts.Layout
import net.minecraft.client.gui.layouts.LayoutElement
import tech.thatgravyboat.skyblockapi.api.area.farming.garden.pests.Pest
import tech.thatgravyboat.skyblockapi.api.area.farming.garden.pests.Spray
import tech.thatgravyboat.skyblockapi.api.events.screen.ContainerInitializedEvent
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.color

@Module
object PestBaitType : InventorySideGui("(?:Pest|Mouse|Vermin) Trap", { GardenConfig.pestBaitAlignment }) {

    override val enabled: Boolean get() = GardenConfig.pestBaitType && SkyBlockIsland.GARDEN.inIsland()

    override fun ContainerInitializedEvent.getLayout(): Layout = LayoutFactory.vertical {
        horizontal {
            string(ChatUtils.ICON_SPACE_COMPONENT)
            string(-"garden.pest_bait_type")
        }

        Spray.entries.forEach { spray ->
            val pests = Pest.getPests(spray)

            horizontal(alignment = MIDDLE) {
                display(Displays.item(spray.itemStack))
                textDisplay(": ${pests.joinToString(", ") { it.displayName }}") {
                    color = TextColor.DARK_GRAY
                }
            }
        }
    }.also { layout ->
        val sprayRows = mutableListOf<LayoutElement>()
        var skippedHeader = false
        layout.visitChildren {
            if (!skippedHeader) { skippedHeader = true; return@visitChildren }
            sprayRows.add(it)
        }

        Spray.entries.forEachIndexed { index, spray ->
            val row = sprayRows.getOrNull(index) ?: return@forEachIndexed
            registerClickRegion(row.x, row.y, row.width, row.height) {
                McClient.sendCommand("gfs ${spray.name} 64")
            }
        }
    }
}
