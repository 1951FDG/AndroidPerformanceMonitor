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
package com.github.moduth.blockcanary.internal

import android.app.ActivityManager
import android.content.Context

import com.github.moduth.blockcanary.BlockCanaryInternals

class ProcessUtils private constructor() {
    companion object {

        @Volatile
        private var sProcessName: String? = null
        private val sNameLock = Any()

        @JvmStatic
        fun myProcessName(): String? {
            if (sProcessName != null) {
                return sProcessName
            }
            synchronized(sNameLock) {
                if (sProcessName != null) {
                    return sProcessName
                }
                sProcessName = obtainProcessName(BlockCanaryInternals.getContext().provideContext())
                return sProcessName
            }
        }

        private fun obtainProcessName(context: Context): String? {
            val pid = android.os.Process.myPid()
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            return am.runningAppProcesses
                    ?.takeIf { it.isNotEmpty() }
                    ?.find { it != null && it.pid == pid }
                    ?.processName
        }
    }
}
