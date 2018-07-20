/*
 * Copyright (C) 2016 MarkZhai (http://zhaiyifan.cn).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.moduth.blockcanary

import android.util.Log
import com.github.moduth.blockcanary.internal.BlockInfo
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Log writer which runs in standalone thread.
 */
object LogWriter {

    private const val TAG = "LogWriter"

    private val SAVE_DELETE_LOCK = Any()
    private val FILE_NAME_FORMATTER = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSS", Locale.US)
    private val TIME_FORMATTER = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private const val OBSOLETE_DURATION = 2 * 24 * 3600 * 1000L

    /**
     * Save log to file
     *
     * @param str block info string
     * @return log file path
     */
    @JvmStatic
    fun save(str: String) {
        synchronized(SAVE_DELETE_LOCK) {
            save("looper", str)
        }
    }

    /**
     * Delete obsolete log files, which is by default 2 days.
     */
    @JvmStatic
    fun cleanObsolete() {
        HandlerThreadFactory.writeLogThreadHandler.post {
            val now = System.currentTimeMillis()
            BlockCanaryInternals.getLogFiles()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let {
                        synchronized(SAVE_DELETE_LOCK) {
                            it.filter { now - it.lastModified() > OBSOLETE_DURATION }.forEach { it.delete() }
                        }
                    }
        }
    }

    @JvmStatic
    fun deleteAll() {
        synchronized(SAVE_DELETE_LOCK) {
            try {
                BlockCanaryInternals.getLogFiles()
                        ?.takeIf { it.isNotEmpty() }
                        ?.forEach { it.delete() }
            } catch (e: Throwable) {
                Log.e(TAG, "deleteAll: ", e)
            }
        }
    }

    @JvmStatic
    private fun save(logFileName: String, str: String): String {
        var path = ""
        var writer: BufferedWriter? = null
        try {
            val file = BlockCanaryInternals.detectedBlockDirectory()
            val time = System.currentTimeMillis()
            path = (file.absolutePath + "/"
                    + logFileName + "-"
                    + FILE_NAME_FORMATTER.format(time) + ".log")

            val out = OutputStreamWriter(FileOutputStream(path, true), "UTF-8")

            writer = BufferedWriter(out)

            writer.write(BlockInfo.SEPARATOR)
            writer.write("**********************")
            writer.write(BlockInfo.SEPARATOR)
            writer.write(TIME_FORMATTER.format(time) + "(write log time)")
            writer.write(BlockInfo.SEPARATOR)
            writer.write(BlockInfo.SEPARATOR)
            writer.write(str)
            writer.write(BlockInfo.SEPARATOR)

            writer.flush()
            writer.close()
            writer = null

        } catch (t: Throwable) {
            Log.e(TAG, "save: ", t)
        } finally {
            try {
                writer?.close()
            } catch (e: Exception) {
                Log.e(TAG, "save: ", e)
            }

        }
        return path
    }

    @JvmStatic
    fun generateTempZip(filename: String): File {
        return File("${BlockCanaryInternals.getPath()}/$filename.zip")
    }
}
