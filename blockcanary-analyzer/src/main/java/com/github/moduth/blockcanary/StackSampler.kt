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

import com.github.moduth.blockcanary.internal.BlockInfo
import java.util.*

/**
 * Dumps thread stack.
 */
internal class StackSampler(private val mCurrentThread: Thread, maxEntryCount: Int, sampleIntervalMillis: Long) : AbstractSampler(sampleIntervalMillis) {

    private var mMaxEntryCount = DEFAULT_MAX_ENTRY_COUNT

    constructor(thread: Thread, sampleIntervalMillis: Long) : this(thread, DEFAULT_MAX_ENTRY_COUNT, sampleIntervalMillis) {}

    init {
        mMaxEntryCount = maxEntryCount
    }

    fun getThreadStackEntries(startTime: Long, endTime: Long): ArrayList<String> {
        val result = ArrayList<String>()
        synchronized(sStackMap) {
            sStackMap.keys
                    .filter { it in (startTime + 1)..(endTime - 1) }
                    .forEach {
                        result.add(BlockInfo.TIME_FORMATTER.format(it)
                                + BlockInfo.SEPARATOR
                                + BlockInfo.SEPARATOR
                                + sStackMap[it])
                    }
        }
        return result
    }

    override fun doSample() {
        val stringBuilder = StringBuilder()
        mCurrentThread.stackTrace.forEach { stringBuilder.append(it.toString()).append(BlockInfo.SEPARATOR) }
        synchronized(sStackMap) {
            if (sStackMap.size == mMaxEntryCount && mMaxEntryCount > 0) {
                sStackMap.remove(sStackMap.keys.iterator().next())
            }
            sStackMap.put(System.currentTimeMillis(), stringBuilder.toString())
        }
    }

    companion object {

        private const val DEFAULT_MAX_ENTRY_COUNT = 100
        private val sStackMap = LinkedHashMap<Long, String>()
    }
}