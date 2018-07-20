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

import java.util.concurrent.atomic.AtomicBoolean

/**
 * [AbstractSampler] sampler defines sampler work flow.
 */
internal abstract class AbstractSampler(sampleInterval: Long) {

    protected var mShouldSample = AtomicBoolean(false)
    protected var mSampleInterval: Long = 0

    private val mRunnable = object : Runnable {
        override fun run() {
            doSample()
            if (mShouldSample.get()) {
                HandlerThreadFactory.timerThreadHandler
                        .postDelayed(this, mSampleInterval)
            }
        }
    }

    init {
        mSampleInterval = if (sampleInterval == 0L) DEFAULT_SAMPLE_INTERVAL.toLong() else sampleInterval
    }

    open fun start() {
        if (mShouldSample.get()) {
            return
        }
        mShouldSample.set(true)

        HandlerThreadFactory.timerThreadHandler.removeCallbacks(mRunnable)
        HandlerThreadFactory.timerThreadHandler.postDelayed(mRunnable, BlockCanaryInternals.getInstance().sampleDelay)
    }

    fun stop() {
        if (!mShouldSample.get()) {
            return
        }
        mShouldSample.set(false)
        HandlerThreadFactory.timerThreadHandler.removeCallbacks(mRunnable)
    }

    protected abstract fun doSample()

    companion object {

        private const val DEFAULT_SAMPLE_INTERVAL = 300
    }
}
