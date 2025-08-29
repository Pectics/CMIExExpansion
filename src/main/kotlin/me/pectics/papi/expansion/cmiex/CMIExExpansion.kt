package me.pectics.papi.expansion.cmiex

import com.Zrips.CMI.CMI
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer

@Suppress("unused")
class CMIExExpansion : PlaceholderExpansion() {

    companion object {
        val REQUIRED_PLUGINS = listOf("CMI", "PexilLibs")
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

    override fun getRequiredPlugin(): String {
        // Just return the first one... fuck it
        // PlaceholderAPI only supports one required plugin
        return REQUIRED_PLUGINS[0]
    }

    override fun canRegister(): Boolean {
        return REQUIRED_PLUGINS.all { Bukkit.getPluginManager().getPlugin(it) != null }
    }

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        val args = params.split("_")
        if (args.isEmpty()) return null
        return when (args[0].lowercase()) {
            "rt" -> when (args.getOrNull(1)?.lowercase()) {
                // %cmiex_rt_cooldown%
                "cooldown" -> CMI.getInstance().randomTeleportationManager.randomTeleportCooldown.toString()
                else -> null
            }
            else -> null
        }
    }

}