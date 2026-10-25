package me.bewf.hitspan.util

import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
//? if >= 1.21.10 {
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.MouseButtonInfo
//?}

object KeyMatch {

    @JvmStatic
    fun attackMouse(button: Int): Boolean {
        val options = Minecraft.getInstance().options ?: return false
        return options.keyAttack.matchesMouseButton(button)
    }

    @JvmStatic
    fun useMouse(button: Int): Boolean {
        val options = Minecraft.getInstance().options ?: return false
        return options.keyUse.matchesMouseButton(button)
    }

    @JvmStatic
    fun attackKey(keyCode: Int): Boolean {
        val options = Minecraft.getInstance().options ?: return false
        return options.keyAttack.matchesKeyCode(keyCode)
    }

    @JvmStatic
    fun useKey(keyCode: Int): Boolean {
        val options = Minecraft.getInstance().options ?: return false
        return options.keyUse.matchesKeyCode(keyCode)
    }

    private fun KeyMapping.matchesMouseButton(button: Int): Boolean {
        //? if >= 1.21.10 {
        return matchesMouse(MouseButtonEvent(0.0, 0.0, MouseButtonInfo(button, 0)))
        //?} else {
        /*return matchesMouse(button)
        *///?}
    }

    private fun KeyMapping.matchesKeyCode(keyCode: Int): Boolean {
        //? if >= 1.21.10 {
        return matches(KeyEvent(keyCode, 0, 0))
        //?} else {
        /*return matches(keyCode, 0)
        *///?}
    }
}
