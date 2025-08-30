package me.pectics.papi.expansion.cmiex

private const val SECONDS_OF_MINUTE = 60
private const val SECONDS_OF_HOUR = SECONDS_OF_MINUTE * 60
private const val SECONDS_OF_DAY = SECONDS_OF_HOUR * 24
private const val SECONDS_OF_MONTH = SECONDS_OF_DAY * 30
private const val SECONDS_OF_YEAR = SECONDS_OF_DAY * 365

inline fun <T> T.letIf(condition: Boolean, block: (T) -> T): T {
    return if (condition) block(this) else this
}

fun <N : Number> N.timef(vararg templates: String): String {
    val duration = this.toLong()
    if (duration <= 0) return ""

    val concatf = templates.getOrNull(0) ?: CMIExExpansion.instance.getString("timef.concatf", TIMEF_CONCATF)
    val yearf = templates.getOrNull(1) ?: CMIExExpansion.instance.getString("timef.yearf", TIMEF_YEARF)
    val monthf = templates.getOrNull(2) ?: CMIExExpansion.instance.getString("timef.monthf", TIMEF_MONTHF)
    val dayf = templates.getOrNull(3) ?: CMIExExpansion.instance.getString("timef.dayf", TIMEF_DAYF)
    val hourf = templates.getOrNull(4) ?: CMIExExpansion.instance.getString("timef.hourf", TIMEF_HOURF)
    val minutef = templates.getOrNull(5) ?: CMIExExpansion.instance.getString("timef.minutef", TIMEF_MINUTEF)
    val secondf = templates.getOrNull(6) ?: CMIExExpansion.instance.getString("timef.secondf", TIMEF_SECONDF)

    val years = (duration / SECONDS_OF_YEAR).toInt()
    val months = ((duration % SECONDS_OF_YEAR) / SECONDS_OF_MONTH).toInt()
    val days = ((duration % SECONDS_OF_MONTH) / SECONDS_OF_DAY).toInt()
    val hours = ((duration % SECONDS_OF_DAY) / SECONDS_OF_HOUR).toInt()
    val minutes = ((duration % SECONDS_OF_HOUR) / SECONDS_OF_MINUTE).toInt()
    val seconds = (duration % SECONDS_OF_MINUTE).toInt()

    return String()
        .letIf(years > 0) { "<lang:$yearf:$years>" }
        .letIf(months > 0) {
            if (it.isEmpty()) "<lang:$monthf:$months>"
            else "<lang:$concatf:'$it':'<lang:$monthf:$months>'>"
        }
        .letIf(days > 0) {
            if (it.isEmpty()) "<lang:$dayf:$days>"
            else "<lang:$concatf:'$it':'<lang:$dayf:$days>'>"
        }
        .letIf(hours > 0) {
            if (it.isEmpty()) "<lang:$hourf:$hours>"
            else "<lang:$concatf:'$it':'<lang:$hourf:$hours>'>"
        }
        .letIf(minutes > 0) {
            if (it.isEmpty()) "<lang:$minutef:$minutes>"
            else "<lang:$concatf:'$it':'<lang:$minutef:$minutes>'>"
        }
        .letIf(seconds > 0) {
            if (it.isEmpty()) "<lang:$secondf:$seconds>"
            else "<lang:$concatf:'$it':'<lang:$secondf:$seconds>'>"
        }
}