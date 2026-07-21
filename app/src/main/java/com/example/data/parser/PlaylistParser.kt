package com.example.data.parser

import android.util.Log
import com.example.data.model.Channel
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

object PlaylistParser {
    private val client = OkHttpClient()

    // --- M3U / M3U8 Parser ---
    fun parseM3U(content: String, playlistId: Long): List<Channel> {
        val channels = mutableListOf<Channel>()
        val reader = content.reader().buffered()
        var currentLine: String?
        var tvgLogo: String? = null
        var groupTitle: String? = null
        var tvgId: String? = null
        var channelName = ""

        try {
            while (reader.readLine().also { currentLine = it } != null) {
                val line = currentLine!!.trim()
                if (line.isEmpty()) continue

                if (line.startsWith("#EXTINF:")) {
                    // Extract tvg-logo
                    tvgLogo = regexExtract(line, "tvg-logo=\"(.*?)\"")
                    // Extract group-title
                    groupTitle = regexExtract(line, "group-title=\"(.*?)\"") ?: regexExtract(line, "group-title=(.*?)(?:\\s|$)")
                    // Extract tvg-id
                    tvgId = regexExtract(line, "tvg-id=\"(.*?)\"")

                    // Extract name (it's after the last comma)
                    val commaIndex = line.lastIndexOf(',')
                    channelName = if (commaIndex != -1 && commaIndex < line.length - 1) {
                        line.substring(commaIndex + 1).trim()
                    } else {
                        "Unknown Channel"
                    }
                } else if (!line.startsWith("#")) {
                    // This line should be the URL
                    if (channelName.isNotEmpty()) {
                        channels.add(
                            Channel(
                                playlistId = playlistId,
                                name = channelName,
                                url = line,
                                logo = tvgLogo,
                                category = groupTitle ?: "General",
                                streamType = if (line.contains("movie") || line.contains(".mp4") || line.contains(".mkv")) "MOVIE" else "LIVE",
                                epgId = tvgId
                            )
                        )
                        // Reset metadata for next channel
                        tvgLogo = null
                        groupTitle = null
                        tvgId = null
                        channelName = ""
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PlaylistParser", "Error parsing M3U content", e)
        }
        return channels
    }

    private fun regexExtract(line: String, patternStr: String): String? {
        val pattern = Regex(patternStr, RegexOption.IGNORE_CASE)
        val match = pattern.find(line)
        return match?.groupValues?.get(1)
    }

    // Download content from a URL safely
    fun downloadUrl(urlStr: String): String {
        val request = Request.Builder().url(urlStr).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP error code: ${response.code}")
            return response.body?.string() ?: ""
        }
    }

    // --- Xtream Codes Parser ---
    fun fetchXtreamChannels(
        baseUrl: String,
        user: String,
        pass: String,
        playlistId: Long
    ): List<Channel> {
        val channels = mutableListOf<Channel>()
        try {
            // 1. Get Live Categories first to map category ID to Category Name
            val categoriesMap = mutableMapOf<String, String>()
            val catUrl = "$baseUrl/player_api.php?username=$user&password=$pass&action=get_live_categories"
            val catResponse = downloadUrl(catUrl)
            if (catResponse.trim().startsWith("[")) {
                val catArray = JSONArray(catResponse)
                for (i in 0 until catArray.length()) {
                    val obj = catArray.getJSONObject(i)
                    val catId = obj.optString("category_id")
                    val catName = obj.optString("category_name")
                    if (catId.isNotEmpty() && catName.isNotEmpty()) {
                        categoriesMap[catId] = catName
                    }
                }
            }

            // 2. Get Live Streams
            val streamsUrl = "$baseUrl/player_api.php?username=$user&password=$pass&action=get_live_streams"
            val streamsResponse = downloadUrl(streamsUrl)
            if (streamsResponse.trim().startsWith("[")) {
                val streamArray = JSONArray(streamsResponse)
                for (i in 0 until streamArray.length()) {
                    val obj = streamArray.getJSONObject(i)
                    val name = obj.optString("name")
                    val streamId = obj.optString("stream_id")
                    val logo = obj.optString("stream_icon")
                    val catId = obj.optString("category_id")
                    val epgId = obj.optString("epg_channel_id")

                    val categoryName = categoriesMap[catId] ?: "General"
                    // Xtream stream URL format: http://domain:port/live/username/password/stream_id.ts or .m3u8
                    val cleanBaseUrl = baseUrl.removeSuffix("/")
                    val url = "$cleanBaseUrl/live/$user/$pass/$streamId.ts"

                    if (name.isNotEmpty() && streamId.isNotEmpty()) {
                        channels.add(
                            Channel(
                                playlistId = playlistId,
                                name = name,
                                url = url,
                                logo = logo.ifEmpty { null },
                                category = categoryName,
                                streamType = "LIVE",
                                epgId = epgId.ifEmpty { null }
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PlaylistParser", "Error downloading or parsing Xtream Codes live streams", e)
        }
        return channels
    }
}
