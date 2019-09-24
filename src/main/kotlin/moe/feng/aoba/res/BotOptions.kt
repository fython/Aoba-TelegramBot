package moe.feng.aoba.res

import moe.feng.aoba.support.get
import moe.feng.aoba.support.resourceBundle

object BotOptions {

    private val resources by resourceBundle("local_options")

    val useProxy: Boolean get() = resources["USE_PROXY"] == "1"
    val httpProxyHost: String get() = resources["HTTP_PROXY_HOST"]
    val httpProxyPort: Int get() = resources["HTTP_PROXY_PORT"].toIntOrNull() ?: 1080

}