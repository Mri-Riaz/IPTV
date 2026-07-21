package com.example.data.parser

import android.util.Log
import android.util.Xml
import com.example.data.model.EpgProgram
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object EpgParser {

    private val dateFormats = listOf(
        SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US),
        SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
    )

    fun parseEpg(xmlContent: String): List<EpgProgram> {
        val programs = mutableListOf<EpgProgram>()
        val parser = Xml.newPullParser()
        try {
            parser.setInput(StringReader(xmlContent))
            var eventType = parser.eventType
            var currentChannelId: String? = null
            var startTime: Long = 0L
            var endTime: Long = 0L
            var currentTitle: String? = null
            var currentDesc: String? = null
            var text = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name == "programme") {
                            currentChannelId = parser.getAttributeValue(null, "channel")
                            val startStr = parser.getAttributeValue(null, "start")
                            val stopStr = parser.getAttributeValue(null, "stop")
                            startTime = parseXmlTvDate(startStr)
                            endTime = parseXmlTvDate(stopStr)
                            currentTitle = null
                            currentDesc = null
                        }
                    }
                    XmlPullParser.TEXT -> {
                        text = parser.text.trim()
                    }
                    XmlPullParser.END_TAG -> {
                        if (name == "title") {
                            currentTitle = text
                        } else if (name == "desc") {
                            currentDesc = text
                        } else if (name == "programme") {
                            if (currentChannelId != null && currentTitle != null) {
                                programs.add(
                                    EpgProgram(
                                        channelEpgId = currentChannelId,
                                        title = currentTitle,
                                        description = currentDesc,
                                        startTime = startTime,
                                        endTime = endTime
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e("EpgParser", "Error parsing XMLTV EPG", e)
        }
        return programs
    }

    private fun parseXmlTvDate(dateStr: String?): Long {
        if (dateStr == null) return 0L
        // Strip timezone suffix from string format like "20260720230000 +0000" if it causes parse failure
        val cleaned = dateStr.trim()
        for (format in dateFormats) {
            try {
                format.timeZone = TimeZone.getTimeZone("UTC")
                val date = format.parse(cleaned)
                if (date != null) {
                    return date.time / 1000L // Convert millisecond timestamp to seconds
                }
            } catch (e: Exception) {
                // Continue trying other formats
            }
        }
        return 0L
    }
}
