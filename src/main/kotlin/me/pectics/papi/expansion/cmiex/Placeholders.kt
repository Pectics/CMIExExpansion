package me.pectics.papi.expansion.cmiex

import org.bukkit.entity.Player

private typealias RT = com.Zrips.CMI.commands.list.rt
private val CMI = com.Zrips.CMI.CMI.getInstance()

@Suppress("FunctionName")
fun rt_cooldown() = CMI.randomTeleportationManager.randomTeleportCooldown

@Suppress("FunctionName")
fun rt_range_max(world: String) = CMI.randomTeleportationManager.getRandomTeleport(world)?.maxDistance ?: -1

@Suppress("FunctionName")
fun rt_range_min(world: String) = CMI.randomTeleportationManager.getRandomTeleport(world)?.minDistance ?: -1

@Suppress("FunctionName")
fun rt_center_x(world: String) = CMI.randomTeleportationManager.getRandomTeleport(world)?.center?.blockX ?: 0

@Suppress("FunctionName")
fun rt_center_z(world: String) = CMI.randomTeleportationManager.getRandomTeleport(world)?.center?.blockZ ?: 0

@Suppress("FunctionName")
fun user_rt_cooldown(player: Player): Int {
    // 获取上次使用随机传送的时间，若未曾使用则返回0
    val useTime = RT.getUsedAt(player.uniqueId, player.world.name) ?: return 0
    // 获取该随机传送对象的冷却时长或全局冷却时长
    val cooldown = CMI.randomTeleportationManager.getRandomTeleport(player.world.name)?.cooldownWithDefault ?: rt_cooldown()
    // 判断是否已过冷却时间，若已过则返回"0"
    if (useTime + cooldown * 1000L < System.currentTimeMillis()) return 0
    // 返回剩余冷却时间：冷却时长 - (当前时间 - 使用时间) + 1
    return cooldown - ((System.currentTimeMillis() - useTime) / 1000L).toInt() + 1
}