package me.bewf.hitspan.util

import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin

object McCompat {

    @JvmStatic
    fun hurtTime(e: LivingEntity): Int = e.hurtTime

    @JvmStatic
    fun invulnerableTime(e: LivingEntity): Int = e.invulnerableTime

    @JvmStatic
    fun health(e: LivingEntity): Float = e.health

    @JvmStatic
    fun burning(e: LivingEntity): Boolean = e.isOnFire

    @JvmStatic
    fun poisoned(e: LivingEntity): Boolean = e.hasEffect(MobEffects.POISON)

    @JvmStatic
    fun withered(e: LivingEntity): Boolean = e.hasEffect(MobEffects.WITHER)

    @JvmStatic
    fun fallDistance(e: LivingEntity): Float = e.fallDistance.toFloat()

    @JvmStatic
    fun rayDistance(eyes: Vec3, look: Vec3, box: AABB, maxDist: Double): Double {
        val end = eyes.add(look.x * maxDist, look.y * maxDist, look.z * maxDist)
        val hit = box.clip(eyes, end).orElse(null) ?: return -1.0
        return eyes.distanceTo(hit)
    }

    @JvmStatic
    fun forwardX(yaw: Float): Double = -sin(Math.toRadians(yaw.toDouble()))

    @JvmStatic
    fun forwardZ(yaw: Float): Double = cos(Math.toRadians(yaw.toDouble()))
}
