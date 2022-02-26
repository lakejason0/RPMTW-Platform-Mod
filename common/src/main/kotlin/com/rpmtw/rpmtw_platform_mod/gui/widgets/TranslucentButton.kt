package com.rpmtw.rpmtw_platform_mod.gui.widgets

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component

class TranslucentButton : Button {
    constructor(
        xIn: Int,
        yIn: Int,
        widthIn: Int,
        heightIn: Int,
        message: Component,
        onPress: OnPress
    ) : super(xIn, yIn, widthIn, heightIn, message, onPress)

    constructor(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        message: Component,
        onPress: OnPress,
        tooltipSupplier: (Button, PoseStack, Int, Int) -> Unit
    ) : super(x, y, width, height, message, onPress, tooltipSupplier)

    override fun blit(stack: PoseStack, x: Int, y: Int, textureX: Int, textureY: Int, width: Int, height: Int) {
        fill(stack, x, y, x + width, y + height, Int.MIN_VALUE)
    }
}