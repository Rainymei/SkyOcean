package me.owdding.skyocean.features.inventory.storageoverlay

import me.owdding.lib.platform.screens.CharacterEvent
import me.owdding.lib.platform.screens.KeyEvent
import me.owdding.lib.platform.screens.MouseButtonEvent
import me.owdding.skyocean.SkyOcean
import me.owdding.skyocean.config.features.inventory.StorageOverlayConfig as OverlayConfig
import me.owdding.skyocean.utils.SkyOceanScreen
import me.owdding.skyocean.utils.rendering.RenderUtils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.CommonComponents
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import tech.thatgravyboat.skyblockapi.api.profile.items.storage.StorageAPI
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.helpers.McFont
import tech.thatgravyboat.skyblockapi.helpers.McLevel
import tech.thatgravyboat.skyblockapi.helpers.McPlayer
import tech.thatgravyboat.skyblockapi.platform.drawSprite
import tech.thatgravyboat.skyblockapi.platform.drawString
import tech.thatgravyboat.skyblockapi.platform.showTooltip
import tech.thatgravyboat.skyblockapi.utils.extentions.scissor
import tech.thatgravyboat.skyblockapi.utils.text.Text
import kotlin.math.ceil
import kotlin.math.max

/**
 * A SkyOcean adaptation of Firmament's storage overlay UI.
 *
 * Differences from Firmament:
 * - We don't inject into the backing container screen; we open a dedicated screen.
 * - We still use the same sprite-based UI to match the "firm" look.
 */
class StorageOverlayScreen(
    private val activePage: StoragePageSlot?,
) : SkyOceanScreen(CommonComponents.EMPTY) {

    companion object {
        private const val PLAYER_WIDTH = 184
        private const val PLAYER_HEIGHT = 91
        private const val PLAYER_Y_INSET = 3
        private const val SLOT_SIZE = 18
        private const val PADDING = 10
        private const val PAGE_SLOTS_WIDTH = SLOT_SIZE * 9
        private const val PAGE_WIDTH = PAGE_SLOTS_WIDTH + 4
        private const val CONTROL_X_INSET = 3
        private const val CONTROL_Y_INSET = 5
        private const val CONTROL_WIDTH = 90
        private const val CONTROL_BACKGROUND_WIDTH = CONTROL_WIDTH + CONTROL_X_INSET + 1
        private const val CONTROL_HEIGHT = 50
    }

    private data class Page(
        val slot: StoragePageSlot,
        val title: String,
        val items: List<ItemStack>,
    ) {
        val rows: Int = max(1, ceil(items.size / 9.0).toInt())
    }

    private val playerInventorySprite = SkyOcean.id("storageoverlay/player_inventory")
    private val upperBackgroundSprite = SkyOcean.id("storageoverlay/upper_background")
    private val slotRowSprite = SkyOcean.id("storageoverlay/storage_row")
    private val controllerBackground = SkyOcean.id("storageoverlay/storage_controls")

    private var scroll = 0.0
    private var lastRenderedInnerHeight = 0
    private var search = ""
    private var searchFocused = true

    private var pageWidthCount = 3

    private data class Rect(val x: Int, val y: Int, val w: Int, val h: Int) {
        fun contains(px: Double, py: Double): Boolean =
            px >= x && px <= x + w && py >= y && py <= y + h
    }

    private inner class Measurements {
        val innerScrollPanelWidth = PAGE_WIDTH * pageWidthCount + (pageWidthCount - 1) * PADDING
        // SkyOcean port: no scrollbar. Small offset to keep 9-slice scaling aligned after removing
        // the scrollbar column.
        val overviewWidth = innerScrollPanelWidth + 2 * PADDING - 2
        val x = width / 2 - overviewWidth / 2
        val overviewHeight = minOf(
            height - PLAYER_HEIGHT - minOf(80, height / 10),
            OverlayConfig.height,
        )
        val innerScrollPanelHeight = overviewHeight - PADDING * 2
        val y = height / 2 - (overviewHeight + PLAYER_HEIGHT) / 2
        val playerX = width / 2 - PLAYER_WIDTH / 2
        val playerY = y + overviewHeight - PLAYER_Y_INSET
        val controlX = playerX - CONTROL_WIDTH + CONTROL_X_INSET
        val controlY = playerY - CONTROL_Y_INSET
    }

    private var measurements = Measurements()

    override fun init() {
        super.init()

        pageWidthCount = OverlayConfig.columns
            .coerceAtMost((width - PADDING) / (PAGE_WIDTH + PADDING))
            .coerceAtLeast(1)
        measurements = Measurements()

        // Focus the active page by scrolling it into view.
        activePage?.let { target ->
            val pages = collectPages()
            val (rect, _) = firstLayoutHit(pages) { _, page -> page.slot == target } ?: return@let
            scroll = (rect.y - measurements.y - 8).toDouble().coerceAtLeast(0.0)
        }

        scroll = scroll.coerceIn(0.0, maxScroll().toDouble())
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val speed = OverlayConfig.scrollSpeed.toDouble()
        val dir = if (OverlayConfig.inverseScroll) 1 else -1
        scroll = (scroll + verticalAmount * speed * dir).coerceIn(0.0, maxScroll().toDouble())
        return true
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (!searchFocused) return super.charTyped(event)
        val ch = event.codepointAsString().firstOrNull() ?: return super.charTyped(event)
        if (ch >= ' ' && ch.code != 127) {
            search += ch
            scroll = scroll.coerceIn(0.0, maxScroll().toDouble())
            return true
        }
        return super.charTyped(event)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (!searchFocused) return super.keyPressed(event)
        // Backspace
        if (event.input == 259 && search.isNotEmpty()) {
            search = search.dropLast(1)
            scroll = scroll.coerceIn(0.0, maxScroll().toDouble())
            return true
        }
        return super.keyPressed(event)
    }

    private fun collectPages(): List<Page> {
        val pages = buildList {
            StorageAPI.enderchests.forEach { (index, stacks) ->
                add(Page(StoragePageSlot(index), "Ender Chest #${index + 1}", stacks))
            }
            StorageAPI.backpacks.forEach { (index, stacks) ->
                add(Page(StoragePageSlot(index + 9), "Backpack #${index + 1}", stacks))
            }
        }
        val q = search.trim()
        if (q.isEmpty()) return pages
        return pages.filter { page -> page.items.any { it.matchesSearch(q) } }
    }

    private fun ItemStack.matchesSearch(query: String): Boolean {
        if (this.isEmpty) return false
        return this.hoverName.string.contains(query, ignoreCase = true)
    }

    private fun scrollPanelInner(): Rect = Rect(
        // Add a small inset so page content doesn't overlap the stretched sprite border.
        measurements.x + PADDING + 3,
        measurements.y + PADDING + 3,
        measurements.innerScrollPanelWidth - 6,
        measurements.innerScrollPanelHeight - 6,
    )

    private fun maxScroll(): Int = max(0, lastRenderedInnerHeight - measurements.innerScrollPanelHeight)

    private inline fun firstLayoutHit(
        pages: List<Page>,
        predicate: (Rect, Page) -> Boolean,
    ): Pair<Rect, Page>? {
        var yOffset = -scroll.toInt()
        var xOffset = 0
        var maxHeight = 0

        for (page in pages) {
            val currentHeight = page.rows * SLOT_SIZE + 6 + McFont.height
            maxHeight = max(maxHeight, currentHeight)

            val rect = Rect(
                measurements.x + PADDING + (PAGE_WIDTH + PADDING) * xOffset,
                yOffset + measurements.y + PADDING,
                PAGE_WIDTH,
                currentHeight,
            )

            if (predicate(rect, page)) return rect to page

            xOffset++
            if (xOffset >= pageWidthCount) {
                yOffset += maxHeight
                xOffset = 0
                maxHeight = 0
            }
        }

        return null
    }

    private inline fun layoutedForEach(pages: List<Page>, func: (Rect, Page) -> Unit) {
        val panel = scrollPanelInner()
        var yOffset = -scroll.toInt()
        var xOffset = 0
        var maxHeight = 0

        for (page in pages) {
            val currentHeight = page.rows * SLOT_SIZE + 6 + McFont.height
            maxHeight = max(maxHeight, currentHeight)

            val rect = Rect(
                panel.x + (PAGE_WIDTH + PADDING) * xOffset,
                yOffset + panel.y,
                PAGE_WIDTH,
                currentHeight,
            )

            func(rect, page)

            xOffset++
            if (xOffset >= pageWidthCount) {
                yOffset += maxHeight
                xOffset = 0
                maxHeight = 0
            }
        }

        lastRenderedInnerHeight = maxHeight + yOffset + scroll.toInt()
    }

    //~ if >= 26.1 'render(' -> 'extractRenderState(' {
    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick)
    //~ }
        val pages = collectPages()

        // Firmament's overlay is meant to sit on a darkened background.
        guiGraphics.fill(0, 0, width, height, 0xB0000000.toInt())

        // The sprite set is translucent; add a stronger backplate behind the actual overlay so
        // the world doesn't show through the frame.
        guiGraphics.fill(
            measurements.x - 2,
            measurements.y - 2,
            measurements.x + measurements.overviewWidth + 2,
            measurements.y + measurements.overviewHeight + 2,
            0xF0000000.toInt(),
        )

        // Backgrounds (base).
        guiGraphics.drawSprite(
            upperBackgroundSprite,
            measurements.x,
            measurements.y,
            measurements.overviewWidth,
            measurements.overviewHeight,
        )
        guiGraphics.drawSprite(
            playerInventorySprite,
            measurements.playerX,
            measurements.playerY,
            PLAYER_WIDTH,
            PLAYER_HEIGHT,
        )
        guiGraphics.drawSprite(
            controllerBackground,
            measurements.controlX,
            measurements.controlY,
            CONTROL_BACKGROUND_WIDTH,
            CONTROL_HEIGHT,
        )

        // Search box (visual affordance + caret).
        val searchX = measurements.controlX + 6
        val searchY = measurements.controlY + 6
        val searchW = CONTROL_WIDTH - 12
        val searchH = 14
        val border = if (searchFocused) 0xFFDDDDDD.toInt() else 0xFF777777.toInt()
        guiGraphics.fill(searchX, searchY, searchX + searchW, searchY + searchH, 0x30000000)
        guiGraphics.fill(searchX, searchY, searchX + searchW, searchY + 1, border)
        guiGraphics.fill(searchX, searchY + searchH - 1, searchX + searchW, searchY + searchH, border)
        guiGraphics.fill(searchX, searchY, searchX + 1, searchY + searchH, border)
        guiGraphics.fill(searchX + searchW - 1, searchY, searchX + searchW, searchY + searchH, border)

        val caretOn = (System.currentTimeMillis() / 500L) % 2L == 0L
        val shown = search.ifEmpty { "" }
        val placeholder = "Search..."
        val text = if (shown.isEmpty() && !searchFocused) placeholder else shown
        val color = if (shown.isEmpty() && !searchFocused) 0xAAAAAA else 0xFFFFFF
        guiGraphics.drawString(
            Text.of(text + if (searchFocused && caretOn) "|" else ""),
            searchX + 3,
            searchY + 3,
            color,
            false,
        )

        // Pages panel with scissor.
        val panel = scrollPanelInner()
        guiGraphics.scissor(panel.x..(panel.x + panel.w), panel.y..(panel.y + panel.h)) {
            layoutedForEach(pages) { rect, page ->
                val isActive = activePage == page.slot

                val titleColor = if (isActive) 0xFFFFFF00.toInt() else 0xFFFFFFFF.toInt()
                guiGraphics.drawString(Text.of(page.title), rect.x + 6, rect.y + 3, titleColor, true)

                // Slot background texture.
                guiGraphics.drawSprite(
                    slotRowSprite,
                    rect.x + 2,
                    rect.y + 5 + McFont.height,
                    PAGE_SLOTS_WIDTH,
                    page.rows * SLOT_SIZE,
                )

                val gridX = rect.x + 3
                val gridY = rect.y + 5 + McFont.height + 1
                page.items.forEachIndexed { index, stack ->
                    val slotX = gridX + (index % 9) * SLOT_SIZE
                    val slotY = gridY + (index / 9) * SLOT_SIZE

                    //~ if >= 26.1 'renderItem(' -> 'item('
                    guiGraphics.item(stack, slotX, slotY)
                    //~ if >= 26.1 'renderItemDecorations(' -> 'itemDecorations('
                    guiGraphics.itemDecorations(McFont.self, stack, slotX, slotY)

                    // Grey out inactive pages, but keep the active page crisp.
                    if (!isActive) {
                        guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0x55000000)
                    }

                    if (mouseX in slotX..(slotX + 16) && mouseY in slotY..(slotY + 16) && !stack.isEmpty && panel.contains(mouseX.toDouble(), mouseY.toDouble())) {
                        RenderUtils.drawSlotHighlightBack(guiGraphics, slotX, slotY)
                        RenderUtils.drawSlotHighlightFront(guiGraphics, slotX, slotY)
                        val flag = if (McClient.options.advancedItemTooltips) TooltipFlag.ADVANCED else TooltipFlag.NORMAL
                        guiGraphics.showTooltip(
                            Text.multiline(
                                stack.getTooltipLines(
                                    Item.TooltipContext.of(McLevel.self),
                                    McPlayer.self!!,
                                    flag,
                                ),
                            ),
                        )
                    }
                }

                if (isActive) {
                    // Outline the active page in a bright yellow.
                    val c = 0xFFFFD700.toInt()
                    guiGraphics.fill(rect.x - 1, rect.y - 1, rect.x + rect.w + 1, rect.y, c)
                    guiGraphics.fill(rect.x - 1, rect.y + rect.h, rect.x + rect.w + 1, rect.y + rect.h + 1, c)
                    guiGraphics.fill(rect.x - 1, rect.y, rect.x, rect.y + rect.h, c)
                    guiGraphics.fill(rect.x + rect.w, rect.y, rect.x + rect.w + 1, rect.y + rect.h, c)
                }
            }
        }

        // Frame pass (top). The transparent overlay resource pack version is fairly translucent, so
        // redraw on top to ensure the right frame edge covers page content.
        guiGraphics.drawSprite(
            upperBackgroundSprite,
            measurements.x,
            measurements.y,
            measurements.overviewWidth,
            measurements.overviewHeight,
        )

        // No scrollbar; scrolling is mouse-wheel only.

        // Player inventory items (for look parity).
        drawPlayerInventory(guiGraphics, mouseX, mouseY)
    }

    private fun drawPlayerInventory(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val items = McPlayer.inventory
        // Same placement as the sprite (matches Firmament layout).
        val hotbarX = measurements.playerX + 12
        val hotbarY = measurements.playerY + 67
        val mainY = measurements.playerY + 9

        items.take(36).forEachIndexed { index, stack ->
            val x = if (index < 9) hotbarX + index * SLOT_SIZE else hotbarX + (index % 9) * SLOT_SIZE
            val y = if (index < 9) hotbarY else mainY + ((index / 9) - 1) * SLOT_SIZE
            //~ if >= 26.1 'renderItem(' -> 'item('
            guiGraphics.item(stack, x, y)
            //~ if >= 26.1 'renderItemDecorations(' -> 'itemDecorations('
            guiGraphics.itemDecorations(McFont.self, stack, x, y)
        }
    }

    override fun mouseClicked(mouseEvent: MouseButtonEvent, doubleClicked: Boolean): Boolean {
        val (mouseX, mouseY) = mouseEvent
        if (mouseEvent.button != 0) return super.mouseClicked(mouseEvent, doubleClicked)

        // Focus/blur the search box.
        run {
            val searchX = measurements.controlX + 6
            val searchY = measurements.controlY + 6
            val searchW = CONTROL_WIDTH - 12
            val searchH = 14
            searchFocused = mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + searchH
        }

        val pages = collectPages()
        val panel = scrollPanelInner()
        if (panel.contains(mouseX, mouseY)) {
            firstLayoutHit(pages) { rect, page ->
                rect.contains(mouseX, mouseY) && activePage != page.slot
            }?.second?.slot?.navigateTo()?.also { return true }
        }

        return super.mouseClicked(mouseEvent, doubleClicked)
    }
}
