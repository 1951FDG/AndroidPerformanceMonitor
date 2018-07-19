/*
 * Copyright (C) 2016 MarkZhai (http://zhaiyifan.cn).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.moduth.blockcanary.internal

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.github.moduth.blockcanary.BlockCanaryInternals
import java.io.*
import java.util.regex.Pattern

internal class PerformanceUtils private constructor() {

    companion object {
        private const val TAG = "PerformanceUtils"

        private var sCoreNum = 0
        private var sTotalMemo: Long = 0

        /**
         * Get cpu core number
         *
         * @return int cpu core number
         */
        @JvmStatic
        val numCores: Int
            get() {
                class CpuFilter : FileFilter {
                    override fun accept(pathname: File): Boolean {
                        return Pattern.matches("cpu[0-9]", pathname.name)
                    }
                }

                if (sCoreNum == 0) {
                    sCoreNum = try {
                        val dir = File("/sys/devices/system/cpu/")
                        val files = dir.listFiles(CpuFilter())
                        files.size
                    } catch (e: Exception) {
                        Log.e(TAG, "getNumCores exception", e)
                        1
                    }

                }
                return sCoreNum
            }

        @JvmStatic
        val freeMemory: Long
            get() {
                val am = BlockCanaryInternals.getContext().provideContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val mi = ActivityManager.MemoryInfo()
                am.getMemoryInfo(mi)
                return mi.availMem / 1024
            }

        @JvmStatic
        val totalMemory: Long
            get() {
                if (sTotalMemo == 0L) {
                    val str1 = "/proc/meminfo"
                    var initialMemory: Long = -1
                    var localFileReader: FileReader? = null
                    try {
                        localFileReader = FileReader(str1)
                        val localBufferedReader = BufferedReader(localFileReader, 8192)
                        localBufferedReader.readLine()?.let {
                            val arrayOfString: Array<String> = it.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                            initialMemory = Integer.valueOf(arrayOfString[1]).toLong()
                        }
                        localBufferedReader.close()
                    } catch (e: IOException) {
                        Log.e(TAG, "getTotalMemory exception = ", e)
                    } finally {
                        localFileReader?.also {
                            try {
                                localFileReader.close()
                            } catch (e: IOException) {
                                Log.e(TAG, "close localFileReader exception = ", e)
                            }
                        }
                    }
                    sTotalMemo = initialMemory
                }
                return sTotalMemo
            }
    }
}