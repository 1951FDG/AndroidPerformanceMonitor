/*
 * Copyright (C) 2015 Square, Inc.
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
package com.github.moduth.blockcanary

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Build
import com.github.moduth.blockcanary.internal.BlockInfo
import com.github.moduth.blockcanary.ui.DisplayActivity


internal class DisplayService : BlockInterceptor {

    override fun onBlock(context: Context, blockInfo: BlockInfo) {
        val intent = Intent(context, DisplayActivity::class.java)
        intent.putExtra("show_latest", blockInfo.timeStart)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val pendingIntent = PendingIntent.getActivity(context, 1, intent, FLAG_UPDATE_CURRENT)
        val contentTitle = context.getString(R.string.block_canary_class_has_blocked, blockInfo.timeStart)
        val contentText = context.getString(R.string.block_canary_notification_message)
        show(context, contentTitle, contentText, pendingIntent)
    }

    private fun show(context: Context, contentTitle: String, contentText: String, pendingIntent: PendingIntent) {
        createChannel(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(context, NAME)
        else Notification.Builder(context)

        builder
                .setSmallIcon(R.drawable.block_canary_notification)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

        notificationManager.notify(-0x21504111, builder.build())
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NAME, NAME, NotificationManager.IMPORTANCE_LOW)
            channel.description = "卡顿信息"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val NAME = "Block"
    }

}
