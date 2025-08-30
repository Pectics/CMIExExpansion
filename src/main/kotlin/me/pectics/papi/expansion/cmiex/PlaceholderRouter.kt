package me.pectics.papi.expansion.cmiex

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.OfflinePlayer

/**
 * 占位符上下文，包含处理占位符所需的所有信息
 */
data class PlaceholderContext(
    val player: OfflinePlayer?,
    val matchedKey: String,               // 命中的模板键，比如 "test_<player>_property"
    val params: Map<String, String?>      // 命名参数：player -> <player>的实际值，可选参数可能为null
)

typealias PlaceholderHandler = (PlaceholderContext) -> String?

@Suppress("unused")
private class Node(
    val literal: String? = null,      // 静态片段（已小写）
    val paramName: String? = null,    // 参数名
    val isRequired: Boolean = false   // 是否为必填参数（<xx> 为 true，[xx] 为 false）
) {
    val children = mutableMapOf<String, Node>()
    var paramChild: Node? = null
    var handler: PlaceholderHandler? = null
    var templateKey: String? = null
}

class PlaceholderRouter {
    private val root = Node()

    /**
     * 注册占位符模板和处理器
     *
     * @param template 模板字符串，支持：
     *   - 静态：test_example
     *   - 必填参数：&lt;name&gt;
     *   - 可选参数：&#91;name&#93;
     * @param handler 处理函数
     */
    fun register(template: String, handler: PlaceholderHandler): PlaceholderRouter {
        val rawParts = template.split('_').filter { it.isNotBlank() }
        if (rawParts.isEmpty()) {
            throw IllegalArgumentException("Template cannot be empty")
        }

        // 验证参数顺序：必填参数不能出现在可选参数之后
        validateParameterOrder(template, rawParts)

        var cur = root
        for (part in rawParts) {
            cur = when {
                part.matches(Regex("""^<([A-Za-z0-9_]+)>$""")) -> {
                    val name = part.substring(1, part.length - 1)
                    cur.paramChild ?: Node(paramName = name, isRequired = true).also {
                        cur.paramChild = it
                    }
                }
                part.matches(Regex("""^\[([A-Za-z0-9_]+)]$""")) -> {
                    val name = part.substring(1, part.length - 1)
                    cur.paramChild ?: Node(paramName = name, isRequired = false).also {
                        cur.paramChild = it
                    }
                }
                else -> {
                    val key = part.lowercase()
                    cur.children.getOrPut(key) { Node(literal = key) }
                }
            }
        }

        cur.handler = handler
        cur.templateKey = template
        return this
    }

    /**
     * 创建别名
     *
     * 别名和目标模板的参数结构必须完全匹配（数量、类型、顺序）
     * 但参数名可以不同，会自动建立位置映射
     */
    fun alias(aliasTemplate: String, targetTemplate: String): PlaceholderRouter {
        val target = findNodeByTemplate(targetTemplate)
            ?: throw IllegalArgumentException("Alias target '$targetTemplate' not found")
        val originalHandler = target.handler
            ?: throw IllegalArgumentException("Target template '$targetTemplate' has no handler")

        // 验证兼容性并创建映射
        val paramMapping = validateAndCreateMapping(aliasTemplate, targetTemplate)

        // 创建参数映射包装器
        val wrappedHandler = createParameterMappingHandler(targetTemplate, originalHandler, paramMapping)
        return register(aliasTemplate, wrappedHandler)
    }

    /**
     * 分发占位符请求
     */
    fun dispatch(params: String, player: OfflinePlayer?): String? {
        val rawParts = params.trim().split('_').filter { it.isNotEmpty() }
        if (rawParts.isEmpty()) return null

        // 从最长前缀开始尝试匹配
        for (take in rawParts.size downTo 1) {
            val match = matchPrefix(rawParts, take) ?: continue
            val rest = rawParts.drop(take)
            val handler = match.node.handler ?: continue

            // 处理剩余参数
            val finalParams = match.params.toMutableMap()

            // 如果最后匹配的节点是参数节点且有剩余部分，将剩余部分合并
            if (match.node.paramName != null && rest.isNotEmpty()) {
                val existingValue = finalParams[match.node.paramName] ?: ""
                val mergedValue = if (existingValue.isEmpty()) {
                    rest.joinToString("_")
                } else {
                    existingValue + "_" + rest.joinToString("_")
                }
                finalParams[match.node.paramName] = mergedValue
            } else if (rest.isNotEmpty()) {
                // 如果不是参数节点但有剩余部分，则匹配失败
                continue
            }

            // 解析参数中的BracketPlaceholders
            for (e in finalParams.entries) {
                val v = e.value ?: continue
                e.setValue(PlaceholderAPI.setBracketPlaceholders(player, v))
            }

            val context = PlaceholderContext(
                player = player,
                matchedKey = match.node.templateKey ?: "",
                params = finalParams
            )

            return handler(context)
        }
        return null
    }

    // === 私有辅助方法 ===

    private fun validateParameterOrder(template: String, rawParts: List<String>) {
        var hasOptional = false
        for (part in rawParts) {
            val isRequired = part.matches(Regex("""^<[A-Za-z0-9_]+>$"""))
            val isOptional = part.matches(Regex("""^\[[A-Za-z0-9_]+]$"""))

            if (isOptional) {
                hasOptional = true
            } else if (isRequired && hasOptional) {
                throw IllegalArgumentException(
                    "Required parameter $part cannot appear after optional parameters in template: $template"
                )
            }
        }
    }

    private fun findNodeByTemplate(template: String): Node? {
        val parts = template.split('_').filter { it.isNotBlank() }
        var cur = root

        for (part in parts) {
            cur = when {
                part.matches(Regex("""^<[A-Za-z0-9_]+>$""")) ||
                        part.matches(Regex("""^\[[A-Za-z0-9_]+]$""")) -> {
                    cur.paramChild ?: return null
                }
                else -> cur.children[part.lowercase()] ?: return null
            }
        }
        return cur
    }

    private data class ParamInfo(
        val name: String,        // 参数名
        val isRequired: Boolean, // 是否必填
        val position: Int        // 在模板中的位置
    )

    private fun parseTemplateParams(template: String): List<ParamInfo> {
        val parts = template.split('_').filter { it.isNotBlank() }
        val params = mutableListOf<ParamInfo>()
        var position = 0

        for (part in parts) {
            when {
                part.matches(Regex("""^<([A-Za-z0-9_]+)>$""")) -> {
                    val name = part.substring(1, part.length - 1)
                    params.add(ParamInfo(name, true, position++))
                }
                part.matches(Regex("""^\[([A-Za-z0-9_]+)]$""")) -> {
                    val name = part.substring(1, part.length - 1)
                    params.add(ParamInfo(name, false, position++))
                }
            }
        }
        return params
    }

    private fun validateAndCreateMapping(aliasTemplate: String, targetTemplate: String): Map<String, String> {
        val aliasParams = parseTemplateParams(aliasTemplate)
        val targetParams = parseTemplateParams(targetTemplate)

        // 检查参数数量
        if (aliasParams.size != targetParams.size) {
            throw IllegalArgumentException(
                "Parameter count mismatch: alias has ${aliasParams.size} parameters, " +
                        "target has ${targetParams.size} parameters.\n" +
                        "Alias: $aliasTemplate\nTarget: $targetTemplate"
            )
        }

        // 检查参数类型匹配并建立映射
        val mapping = mutableMapOf<String, String>()
        for (i in aliasParams.indices) {
            val aliasParam = aliasParams[i]
            val targetParam = targetParams[i]

            if (aliasParam.isRequired != targetParam.isRequired) {
                throw IllegalArgumentException(
                    "Parameter type mismatch at position $i:\n" +
                            "Alias parameter '${aliasParam.name}' is ${if (aliasParam.isRequired) "required <>" else "optional []"}\n" +
                            "Target parameter '${targetParam.name}' is ${if (targetParam.isRequired) "required <>" else "optional []"}\n" +
                            "Alias: $aliasTemplate\nTarget: $targetTemplate"
                )
            }

            // target参数名 -> alias参数名
            mapping[targetParam.name] = aliasParam.name
        }

        return mapping
    }

    private fun createParameterMappingHandler(
        targetTemplate: String,
        originalHandler: PlaceholderHandler,
        paramMapping: Map<String, String>  // target参数名 -> alias参数名
    ): PlaceholderHandler {
        return { context ->
            // 重新映射参数：将alias参数名的值赋给target参数名
            val targetParams = mutableMapOf<String, String?>()

            for ((targetParamName, aliasParamName) in paramMapping) {
                targetParams[targetParamName] = context.params[aliasParamName]
            }

            // 处理剩余的非映射参数
            for ((key, value) in context.params) {
                if (!paramMapping.values.contains(key)) {
                    targetParams[key] = value
                }
            }

            val targetContext = PlaceholderContext(
                player = context.player,
                matchedKey = targetTemplate,
                params = targetParams
            )

            originalHandler(targetContext)
        }
    }

    private data class MatchResult(val node: Node, val params: Map<String, String?>)

    private fun matchPrefix(rawParts: List<String>, take: Int): MatchResult? {
        var cur = root
        val captured = linkedMapOf<String, String?>()

        for (i in 0 until take) {
            val raw = rawParts[i]
            val lc = raw.lowercase()

            // 优先匹配静态节点
            val staticNext = cur.children[lc]
            val paramNext = cur.paramChild

            cur = when {
                staticNext != null -> staticNext
                paramNext != null -> {
                    captured[paramNext.paramName!!] = raw
                    paramNext
                }
                else -> return null
            }
        }

        // 处理未匹配到的可选参数
        var tempCur = cur
        while (tempCur.paramChild != null && !tempCur.paramChild!!.isRequired) {
            captured[tempCur.paramChild!!.paramName!!] = null
            tempCur = tempCur.paramChild!!
            if (tempCur.handler != null) {
                cur = tempCur
            }
        }

        return if (cur.handler != null) MatchResult(cur, captured) else null
    }

}
