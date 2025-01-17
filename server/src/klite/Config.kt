package klite

import java.io.File

object Config {
  fun optional(env: String): String? = System.getProperty(env) ?: System.getenv(env)
  fun optional(env: String, default: String) = optional(env) ?: default
  fun required(env: String) = optional(env) ?: error("$env should be provided as system property or env var")

  fun inherited(env: String): String? = optional(env) ?: if (env.contains(".")) inherited(env.substringBeforeLast(".")) else null
  fun inherited(env: String, default: String): String = inherited(env) ?: default

  val active: List<String> by lazy { optional("ENV", "dev").split(",").map { it.trim() } }
  fun isActive(conf: String) = active.contains(conf)
  fun isAnyActive(vararg confs: String) = active.any { confs.contains(it) }

  operator fun get(env: String) = required(env)
  operator fun set(env: String, value: String): String? = System.setProperty(env, value)

  fun overridable(env: String, value: String) {
    if (optional(env) == null) Config[env] = value
  }

  fun useEnvFile(file: File = File(".env"), force: Boolean = false) {
    if (!force && !file.exists()) return logger().info("No ${file.absolutePath} found, skipping")
    file.forEachLine {
      val line = it.trim()
      if (line.isNotEmpty() && !line.startsWith('#'))
        line.split('=', limit = 2).let { (key, value) ->
          if (force || optional(key) == null) set(key, value)
        }
    }
  }
}

val Config.isDev get() = isActive("dev")
val Config.isTest get() = isActive("test")
val Config.isProd get() = isActive("prod")
