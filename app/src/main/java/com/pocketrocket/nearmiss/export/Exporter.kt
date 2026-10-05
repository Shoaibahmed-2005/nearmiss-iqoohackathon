package com.pocketrocket.nearmiss.export

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import com.pocketrocket.nearmiss.events.NearMissEvent
import java.io.File
import java.io.FileWriter

object Exporter {
    fun exportEvents(context: Context, events: List<NearMissEvent>) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val csvFile = File(downloadsDir, "nearmiss_events.csv")
        val geojsonFile = File(downloadsDir, "nearmiss_events.geojson")

        // Write CSV
        FileWriter(csvFile).use { writer ->
            writer.append("id,timestampMs,lat,lon,minTtc,objectClass,brakeG,yawRate,confirmed\n")
            events.forEach { e ->
                writer.append("${e.id},${e.timestampMs},${e.lat},${e.lon},${e.minTtc},${e.objectClass},${e.brakeG},${e.yawRate},${e.confirmed}\n")
            }
        }

        // Write GeoJSON
        FileWriter(geojsonFile).use { writer ->
            writer.append("{\"type\": \"FeatureCollection\", \"features\": [")
            val features = events.filter { it.lat != null && it.lon != null }.joinToString(",") { e ->
                """
                {
                    "type": "Feature",
                    "geometry": { "type": "Point", "coordinates": [${e.lon}, ${e.lat}] },
                    "properties": {
                        "id": ${e.id},
                        "ttc": ${e.minTtc},
                        "class": "${e.objectClass}",
                        "confirmed": ${e.confirmed}
                    }
                }
                """.trimIndent()
            }
            writer.append(features)
            writer.append("]}")
        }
    }
}
