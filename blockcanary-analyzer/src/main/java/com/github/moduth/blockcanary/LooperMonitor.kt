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

import android.os.Debug
import android.os.SystemClock
import android.util.Printer

internal class LooperMonitor(private val mBlockListener: BlockListener, blockThresholdMillis: Long, private val mStopWhenDebugging: Boolean) : Printer {

    private var mBlockThresholdMillis = DEFAULT_BLOCK_THRESHOLD_MILLIS.toLong()
    private var mStartTimestamp: Long = 0
    private var mStartThreadTimestamp: Long = 0
    private var mPrintingStarted = false

    interface BlockListener {
        fun onBlockEvent(realStartTime: Long, realTimeEnd: Long, threadTimeStart: Long, threadTimeEnd: Long)
    }

    init {
        mBlockThresholdMillis = blockThresholdMillis
    }

    override fun println(x: String) {
        if (mStopWhenDebugging && Debug.isDebuggerConnected()) {
            return
        }
        if (!mPrintingStarted) {
            mStartTimestamp = System.currentTimeMillis()
            mStartThreadTimestamp = SystemClock.currentThreadTimeMillis()
            mPrintingStarted = true
            startDump()
        } else {
            val endTime = System.currentTimeMillis()
            mPrintingStarted = false
            if (isBlock(endTime)) {
                notifyBlockEvent(endTime)
            }
            stopDump()
        }
    }

    private fun isBlock(endTime: Long): Boolean {
        return endTime - mStartTimestamp > mBlockThresholdMillis
    }

    private fun notifyBlockEvent(endTime: Long) {
        val startTime = mStartTimestamp
        val startThreadTime = mStartThreadTimestamp
        val endThreadTime = SystemClock.currentThreadTimeMillis()
        HandlerThreadFactory.getWriteLogThreadHandler().post { mBlockListener.onBlockEvent(startTime, endTime, startThreadTime, endThreadTime) }
    }

    private fun startDump() {
        BlockCanaryInternals.getInstance().stackSampler?.start()
        BlockCanaryInternals.getInstance().cpuSampler?.start()
    }

    private fun stopDump() {
        BlockCanaryInternals.getInstance().stackSampler?.stop()
        BlockCanaryInternals.getInstance().cpuSampler?.stop()
    }

    companion object {
        private const val DEFAULT_BLOCK_THRESHOLD_MILLIS = 3000
    }
}