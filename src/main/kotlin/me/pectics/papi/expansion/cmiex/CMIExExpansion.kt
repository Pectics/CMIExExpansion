package me.pectics.papi.expansion.cmiex

import me.clip.placeholderapi.expansion.Configurable
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer

private val REQUIRED_PLUGINS = listOf("CMI", "PexilLibs")
private val PLACEHOLDER_ROUTER = PlaceholderRouter()

@Suppress("unused")
class CMIExExpansion : PlaceholderExpansion(), Configurable {

    companion object {
        private var _instance: CMIExExpansion? = null
        val instance: CMIExExpansion
            get() = _instance ?: throw IllegalStateException("CMIExExpansion is not initialized yet.")
        private fun setInstance(instance: CMIExExpansion) { if (_instance == null) _instance = instance }
    }

    override fun getIdentifier(): String {
        return "cmiex"
    }

    override fun getAuthor(): String {
        return "Pectics"
    }

    override fun getVersion(): String {
        return BuildConfig.VERSION
    }

    override fun getRequiredPlugin(): String? {
        // Just return the first one... fuck it
        // PlaceholderAPI only supports one required plugin
        return REQUIRED_PLUGINS.getOrNull(0)
    }

    override fun canRegister(): Boolean {
        return REQUIRED_PLUGINS.all { Bukkit.getPluginManager().getPlugin(it) != null }
    }

    override fun register(): Boolean {
        setInstance(this)
        PLACEHOLDER_ROUTER
            .register("rt_cooldown") { rt_cooldown().toString() }
            .alias("rt_cd", "rt_cooldown")

            .register("rt_cooldown_formatted_[translations]") { (_, _, params) ->
                params["translations"]
                    ?.takeIf { it.isNotBlank() }
                    ?.split("_")
                    ?.filter { it.isNotBlank() }
                    ?.toTypedArray()
                    ?.let { rt_cooldown().timef(*it) }
                    ?: rt_cooldown().timef()
            }
            .alias("rt_cd_formatted_[tr]", "rt_cooldown_formatted_[translations]")
            .alias("rt_cdf_[tr]", "rt_cooldown_formatted_[translations]")

            .register("rt_range_<type>_[world]") { (offline, _, params) ->
                val type = params["type"]
                    ?.takeIf { it.isNotBlank() }
                    ?: return@register null
                val world = params["world"]
                    ?.takeIf { it.isNotBlank() }
                    ?: offline?.player?.world?.name
                    ?: return@register null
                when (type.lowercase()) {
                    "max" -> rt_range_max(world)
                        .takeIf { it >= 0 }?.toString()
                        ?: ERROR_UNKNOWN
                    "min" -> rt_range_min(world)
                        .takeIf { it >= 0 }?.toString()
                        ?: ERROR_UNKNOWN
                    else -> null
                }
            }
            .register("rt_center_<axis>_[world]") { (offline, _, params) ->
                val axis = params["axis"]
                    ?.takeIf { it.isNotBlank() }
                    ?: return@register null
                val world = params["world"]
                    ?.takeIf { it.isNotBlank() }
                    ?: offline?.player?.world?.name
                    ?: return@register null
                when (axis.lowercase()) {
                    "x" -> rt_center_x(world).toString()
                    "z" -> rt_center_z(world).toString()
                    else -> null
                }
            }
            .register("rt_region_size_<world>") { (_, _, params) ->
                val world = params["world"]
                    ?.takeIf { it.isNotBlank() }
                    ?: return@register null
                val length = rt_range_max(world)
                    .takeIf { it >= 0 }
                    ?.let { it * 2 }
                    ?: return@register ERROR_UNKNOWN
                "$length×$length"
            }

            .register("user_rt_cooldown") { (offline) ->
                offline?.let { o ->
                    Bukkit.getPlayer(o.uniqueId)?.let { p ->
                        user_rt_cooldown(p).toString()
                    }
                }
            }
            .alias("user_rt_cd", "user_rt_cooldown")

            .register("user_rt_cooldown_formatted_[translations]") { (offline, _, params) ->
                val player = offline?.player ?: return@register null
                params["translations"]
                    ?.takeIf { it.isNotBlank() }
                    ?.split("_")
                    ?.filter { it.isNotBlank() }
                    ?.toTypedArray()
                    ?.let { user_rt_cooldown(player).timef(*it) }
                    ?: user_rt_cooldown(player).timef()
            }
            .alias("user_rt_cd_formatted_[tr]", "user_rt_cooldown_formatted_[translations]")
            .alias("user_rt_cdf_[tr]", "user_rt_cooldown_formatted_[translations]")
        return super.register()
    }

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        return PLACEHOLDER_ROUTER.dispatch(params, player)
    }

    override fun getDefaults(): Map<String, Any> {
        return mapOf(
            "error.unknown" to ERROR_UNKNOWN,
            "timef.concatf" to TIMEF_CONCATF,
            "timef.yearf" to TIMEF_YEARF,
            "timef.monthf" to TIMEF_MONTHF,
            "timef.dayf" to TIMEF_DAYF,
            "timef.hourf" to TIMEF_HOURF,
            "timef.minutef" to TIMEF_MINUTEF,
            "timef.secondf" to TIMEF_SECONDF,
        )
    }

}