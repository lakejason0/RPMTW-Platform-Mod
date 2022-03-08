package com.rpmtw.rpmtw_platform_mod.translation.machineTranslation

import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.fuel.httpGet
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.rpmtw.rpmtw_platform_mod.RPMTWPlatformMod
import com.rpmtw.rpmtw_platform_mod.utilities.Utilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.TextColor
import net.minecraft.network.chat.TextComponent
import net.minecraft.network.chat.TranslatableComponent
import org.apache.http.client.utils.URIBuilder
import java.io.File
import java.sql.Timestamp
import java.util.concurrent.TimeUnit

object MTManager {
    private val mc = Minecraft.getInstance()
    private val cache: MutableMap<SourceText, MTInfo?> = HashMap()
    private val cacheFile: File = Utilities.getFileLocation("machine_translation_cache.json")
    private val queue: MutableList<SourceText> = ArrayList()
    private var handleQueueing: Boolean = false
    private var translatingCount: Int = 0

    @Suppress("SpellCheckingInspection")
    private val translatedLanguage: String
        get() = when (mc.languageManager.selected.code) {
            "zh_tw" -> "zh_Hant"
            "zh_hk" -> "zh_Hant"
            "zh_cn" -> "zh_Hans"
            else -> "zh_Hant"
        }

    fun create(source: String, vararg i18nArgs: Any? = arrayOf(null)): MutableComponent {
        val info: MTInfo? = cache[SourceText(source, translatedLanguage)]

        return if (info?.text != null && info.status == MTDataStatus.SUCCESS) {
            TextComponent(String.format(info.text, *i18nArgs)).withStyle {
                it.withColor(ChatFormatting.GREEN)
            }
        } else if (info?.status == MTDataStatus.FAILED && info.error != null) {
            TextComponent(I18n.get("machineTranslation.rpmtw_platform_mod.status.failed", info.error)).withStyle(
                ChatFormatting.RED
            )
        } else if (translatingCount > 3) {
            // If there are too many things to translate at the same time, the translation will be skipped to avoid sending too many requests to the server
            generateProgressText()
        } else if (info?.status == MTDataStatus.Translating) {
            generateProgressText()
        } else {
            Utilities.coroutineLaunch {
                translateAndCache(source)
            }

            generateProgressText()
        }
    }

    fun addToQueue(source: String) {
        queue.add(SourceText(source, translatedLanguage))
        if (!handleQueueing) {
            handleQueue()
        }
    }

    fun getFromCache(source: String, vararg i18nArgs: Any? = arrayOf(null)): MutableComponent? {
        val info: MTInfo? = cache[SourceText(source, translatedLanguage)]

        if (info?.text == null) return null
        return TextComponent(String.format(info.text, i18nArgs)).withStyle {
            // light blue
            it.withColor(TextColor.parseColor("#2f8eed"))
        }
    }

    fun saveCache() {
        if (!cacheFile.exists()) {
            cacheFile.createNewFile()
        }
        Gson().toJson(cache, HashMap<SourceText, MTInfo>().javaClass).let {
            cacheFile.writeText(it)
        }
    }

    fun readCache() {
        try {
            if (cacheFile.exists()) {
                val json = cacheFile.reader().readText()
                cache.putAll(Gson().fromJson(json, HashMap<SourceText, MTInfo>().javaClass))
            }
        } catch (e: Exception) {
            RPMTWPlatformMod.LOGGER.error("Failed to read machine translation cache file", e)
            cacheFile.delete()
        }
    }

    private fun handleQueue() {
        handleQueueing = true
        Utilities.coroutineLaunch {
            while (queue.isNotEmpty() && translatingCount < 3) {
                val sourceText = queue.removeAt(0)
                translateAndCache(sourceText.text)
                withContext(Dispatchers.IO) {
                    // Sleep for 2 second to prevent spamming the Google Translate API
                    Thread.sleep(TimeUnit.SECONDS.toMillis(2))
                }
            }
            handleQueueing = false
        }
    }

    private fun generateProgressText(): MutableComponent {
        val text =
            TranslatableComponent("machineTranslation.rpmtw_platform_mod.status.translating").withStyle(ChatFormatting.GRAY)
        for (i in 0 until System.currentTimeMillis() % 400 / 100) {
            text.append(".")
        }
        return text
    }

    private suspend fun translateAndCache(source: String) {
        translatingCount++
        val sourceText = SourceText(source, translatedLanguage)
        try {
            val translatingData = MTInfo(
                timestamp = Timestamp(System.currentTimeMillis()), status = MTDataStatus.Translating
            )
            cache[sourceText] = translatingData

            val result: String = translate(source)
            val data = MTInfo(
                text = result, timestamp = Timestamp(System.currentTimeMillis()), status = MTDataStatus.SUCCESS
            )
            cache[sourceText] = data

        } catch (e: Exception) {
            RPMTWPlatformMod.LOGGER.error("Translation failed", e)
            val data = MTInfo(
                timestamp = Timestamp(System.currentTimeMillis()), error = e, status = MTDataStatus.FAILED
            )
            cache[sourceText] = data
        } finally {
            translatingCount--
        }
    }

    private suspend fun translate(text: String): String {
        val builder = URIBuilder("https://translate.googleapis.com/")
        builder.path = "translate_a/single"
        builder.addParameter("client", "gtx")
        builder.addParameter("sl", "en")
        builder.addParameter("tl", translatedLanguage)
        builder.addParameter("dt", "t")
        builder.addParameter("q", text)
        builder.addParameter("format", "json")
        val url: String = builder.build().toString()

        return url.httpGet().awaitStringResult().fold({ response ->
            return@fold JsonParser.parseString(response).asJsonArray[0].asJsonArray[0].asJsonArray[0].asString
        }, {
            if (it.response.statusCode == 429) {
                throw Exception(I18n.get("machineTranslation.rpmtw_platform_mod.status.rate_limit"))
            } else {
                throw Exception(
                    I18n.get(
                        "machineTranslation.rpmtw_platform_mod.status.exception", it.response.statusCode
                    )
                )
            }
        })
    }
}

data class SourceText(val text: String, val language: String)