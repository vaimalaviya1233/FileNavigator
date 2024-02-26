package com.w2sv.navigator.di

import android.app.NotificationManager
import android.content.Context
import com.w2sv.androidutils.services.getNotificationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object NavigatorModule {

    @Singleton
    @Provides
    fun notificationManager(@ApplicationContext context: Context): NotificationManager =
        context.getNotificationManager()
}