package com.citta.driver.di

import com.citta.driver.BuildConfig
import com.citta.driver.data.observability.CrashlyticsCrashReporter
import com.citta.driver.data.observability.LogcatLogSink
import com.citta.driver.domain.observability.CrashReporter
import com.citta.driver.domain.observability.LogLevel
import com.citta.driver.domain.observability.Logger
import com.citta.driver.domain.observability.NoOpLogSink
import com.citta.driver.domain.observability.RoutingLogger
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Wires the structured-logging and crash-reporting seams. Debug builds log to logcat from DEBUG
 * level up and skip Crashlytics; release builds stay silent on logcat and mirror WARN/ERROR to
 * Crashlytics through [RoutingLogger].
 */
@Module
@InstallIn(SingletonComponent::class)
object ObservabilityModule {

    @Provides
    @Singleton
    fun provideFirebaseCrashlytics(): FirebaseCrashlytics = FirebaseCrashlytics.getInstance()

    @Provides
    @Singleton
    fun provideCrashReporter(impl: CrashlyticsCrashReporter): CrashReporter = impl

    @Provides
    @Singleton
    fun provideLogger(logcatSink: LogcatLogSink, crashReporter: CrashReporter): Logger =
        if (BuildConfig.DEBUG) {
            RoutingLogger(minLevel = LogLevel.DEBUG, sink = logcatSink, crashReporter = crashReporter)
        } else {
            RoutingLogger(minLevel = LogLevel.WARN, sink = NoOpLogSink, crashReporter = crashReporter)
        }
}
