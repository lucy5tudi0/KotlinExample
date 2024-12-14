package kr.itskimlot.kotlinexample

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.File
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class KotlinExample: JavaPlugin(), Listener {

    companion object {
        const val PLUGIN_VERSION = "1.0"

        lateinit var instance: KotlinExample
        lateinit var lang: YamlConfiguration

        var hasUpdate: Boolean = false
        var latestVersion: String = ""
        var downloadUrl: String = ""
    }
    init { instance = this }

    override fun onEnable() {
        // Config & Lang ───────────────────────────────────────────────────────────────
        saveDefaultConfig()
        lang = saveLang()

        // Update Checker ──────────────────────────────────────────────────────────────
        updateCheck()
        server.pluginManager.registerEvents(this, this)

        // Commands ────────────────────────────────────────────────────────────────────
        // TODO("Register your commands here.")

        // Listeners ───────────────────────────────────────────────────────────────────
        // TODO("Register your listeners here.")
    }

    // Lang Util ───────────────────────────────────────────────────────────────────────
    private fun saveLang(): YamlConfiguration {
        val file = File(dataFolder, "lang.yml")
        if (!dataFolder.exists()) dataFolder.mkdirs()
        if (!file.exists()) saveResource("lang.yml", false)
        return YamlConfiguration.loadConfiguration(file)
    }

    fun reloadLang(): YamlConfiguration {
        val file = File(dataFolder, "lang.yml")
        return YamlConfiguration.loadConfiguration(file)
    }

    fun getMessage(key: String): List<String> {
        val list: MutableList<String> = mutableListOf()
        when (val msg = lang.get(key)) {
            is String -> list.add(lang.getString("prefix") + msg)
            is List<*> -> msg.forEach { list.add(lang.getString("prefix") + it) }
            else -> listOf("<red>Unknown Error: $key")
        }
        return list
    }

    fun String.toMiniMessage(): Component {
        val miniMessage = MiniMessage.miniMessage()
        return miniMessage.deserialize(this)
    }

    // Check New Update ─────────────────────────────────────────────────────────────────
    private fun updateCheck() {
        val url = "https://api.itskimlot.kr/plugin-api/check-version/"
        val packageName = this::class.java.packageName
        var connection: HttpURLConnection? = null

        try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true

            connection.setRequestProperty("Content-Type", "application/json")

            val jsonBody = """{"packageName": "$packageName"}"""
            connection.outputStream.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(jsonBody)
                    writer.flush()
                }
            }

            val respCode = connection.responseCode
            if (respCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                parseResponse(response)
            }
        } catch (e: SocketTimeoutException) {
            logger.warning("[Update Checker] Connection Timeout.")
        } catch (e: IOException) {
            logger.warning("[Update Checker] Connection Error. Check your network.")
        } catch (e: Exception) {
            logger.warning("[Update Checker] Unknown Error.")
            logger.warning(e.message)
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseResponse(response: String) {
        val parser = JSONParser()
        try {
            val jsonObj = parser.parse(response) as JSONObject

            val packageName = jsonObj["packageName"] as String
            val latestVersion = jsonObj["latestVersion"] as String
            val downloadUrl = jsonObj["downloadUrl"] as String

            if (packageName != this::class.java.packageName) {
                logger.warning("[Update Checker] Package Name is not matched.")
                return
            }

            if (latestVersion != PLUGIN_VERSION) {
                lang.getStringList("alert.new_update").forEach {
                    val msg = it.replace("%new_version%", latestVersion)
                        .replace("%now_version%", PLUGIN_VERSION)
                        .replace("%url%", downloadUrl)
                    server.consoleSender.sendMessage(msg.toMiniMessage())
                }
                hasUpdate = true
            }
        } catch (e: Exception) {
            logger.warning("[Update Checker] Failed to parse response.")
            logger.warning(e.message)
        }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (player.isOp && hasUpdate) {
            lang.getStringList("alert.new_update").forEach {
                val msg = it.replace("%new_version%", latestVersion)
                    .replace("%now_version%", PLUGIN_VERSION)
                    .replace("%url%", downloadUrl)
                player.sendMessage(msg.toMiniMessage())
            }
        }
    }
}