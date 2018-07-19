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

import android.os.Build
import android.util.Log
import com.github.moduth.blockcanary.internal.BlockInfo
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

/**
 * Dumps cpu usage.
 */
internal class CpuSampler(sampleInterval: Long) : AbstractSampler(sampleInterval) {

    /**
     * TODO: Explain how we define cpu busy in README
     */
    private val BUSY_TIME: Int = (mSampleInterval * 1.2f).toInt()

    private val mCpuInfoEntries = LinkedHashMap<Long, String>()
    private var mPid = 0
    private var mUserLast: Long = 0
    private var mSystemLast: Long = 0
    private var mIdleLast: Long = 0
    private var mIoWaitLast: Long = 0
    private var mTotalLast: Long = 0
    private var mAppCpuTimeLast: Long = 0

    /**
     * Get cpu rate information
     *
     * @return string show cpu rate information
     */
    val cpuRateInfo: String
        get() {
            val sb = StringBuilder()
            synchronized(mCpuInfoEntries) {
                mCpuInfoEntries.forEach { (time, value) ->
                    sb.append(BlockInfo.TIME_FORMATTER.format(time))
                            .append(' ')
                            .append(value)
                            .append(BlockInfo.SEPARATOR)
                }
            }
            return sb.toString()
        }

    override fun start() {
        if (Build.VERSION.SDK_INT < 26) {
            super.start()
            reset()
        }
    }

    fun isCpuBusy(start: Long, end: Long): Boolean {
        if (end - start > mSampleInterval) {
            val s = start - mSampleInterval
            val e = start + mSampleInterval
            var last: Long = 0
            synchronized(mCpuInfoEntries) {
                for ((time) in mCpuInfoEntries) {
                    if (time in (s + 1)..(e - 1)) {
                        if (last != 0L && time - last > BUSY_TIME) {
                            return true
                        }
                        last = time
                    }
                }
            }
        }
        return false
    }

    override fun doSample() {
        var cpuReader: BufferedReader? = null
        var pidReader: BufferedReader? = null

        try {
            cpuReader = BufferedReader(InputStreamReader(
                    FileInputStream("/proc/stat")), BUFFER_SIZE)
            val cpuRate: String = cpuReader.readLine() ?: ""

            if (mPid == 0) {
                mPid = android.os.Process.myPid()
            }
            pidReader = BufferedReader(InputStreamReader(FileInputStream("/proc/$mPid/stat")), BUFFER_SIZE)
            val pidCpuRate: String = pidReader.readLine() ?: ""
            parse(cpuRate, pidCpuRate)
        } catch (throwable: Throwable) {
            Log.e(TAG, "doSample: ", throwable)
        } finally {
            try {
                cpuReader?.close()
                pidReader?.close()
            } catch (exception: IOException) {
                Log.e(TAG, "doSample: ", exception)
            }

        }
    }

    private fun reset() {
        mUserLast = 0
        mSystemLast = 0
        mIdleLast = 0
        mIoWaitLast = 0
        mTotalLast = 0
        mAppCpuTimeLast = 0
    }

    private fun parse(cpuRate: String, pidCpuRate: String) {
        val cpuInfoArray = cpuRate.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (cpuInfoArray.size < 9) {
            return
        }

        val user = java.lang.Long.parseLong(cpuInfoArray[2])
        val nice = java.lang.Long.parseLong(cpuInfoArray[3])
        val system = java.lang.Long.parseLong(cpuInfoArray[4])
        val idle = java.lang.Long.parseLong(cpuInfoArray[5])
        val ioWait = java.lang.Long.parseLong(cpuInfoArray[6])
        val total = (user + nice + system + idle + ioWait
                + java.lang.Long.parseLong(cpuInfoArray[7])
                + java.lang.Long.parseLong(cpuInfoArray[8]))

        val pidCpuInfoList = pidCpuRate.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (pidCpuInfoList.size < 17) {
            return
        }

        val appCpuTime = (java.lang.Long.parseLong(pidCpuInfoList[13])
                + java.lang.Long.parseLong(pidCpuInfoList[14])
                + java.lang.Long.parseLong(pidCpuInfoList[15])
                + java.lang.Long.parseLong(pidCpuInfoList[16]))

        if (mTotalLast != 0L) {
            val stringBuilder = StringBuilder()
            val idleTime = idle - mIdleLast
            val totalTime = total - mTotalLast

            stringBuilder
                    .append("cpu:")
                    .append((totalTime - idleTime) * 100L / totalTime)
                    .append("% ")
                    .append("app:")
                    .append((appCpuTime - mAppCpuTimeLast) * 100L / totalTime)
                    .append("% ")
                    .append("[")
                    .append("user:").append((user - mUserLast) * 100L / totalTime)
                    .append("% ")
                    .append("system:").append((system - mSystemLast) * 100L / totalTime)
                    .append("% ")
                    .append("ioWait:").append((ioWait - mIoWaitLast) * 100L / totalTime)
                    .append("% ]")

            synchronized(mCpuInfoEntries) {
                mCpuInfoEntries[System.currentTimeMillis()] = stringBuilder.toString()
                if (mCpuInfoEntries.size > MAX_ENTRY_COUNT) {
                    for ((key) in mCpuInfoEntries) {
                        mCpuInfoEntries.remove(key)
                        break
                    }
                }
            }
        }
        mUserLast = user
        mSystemLast = system
        mIdleLast = idle
        mIoWaitLast = ioWait
        mTotalLast = total

        mAppCpuTimeLast = appCpuTime
    }

    companion object {

        private const val TAG = "CpuSampler"
        private const val BUFFER_SIZE = 1000
        private const val MAX_ENTRY_COUNT = 10
    }
}