package com.citta.driver.di

import android.content.Context
import androidx.room.Room
import com.citta.driver.BuildConfig
import com.citta.driver.data.api.CittaApi
import com.citta.driver.data.auth.AuthInterceptor
import com.citta.driver.data.auth.DefaultAuthRepository
import com.citta.driver.data.auth.ForcedLogoutAuthenticator
import com.citta.driver.data.auth.PasswordChangeRequiredInterceptor
import com.citta.driver.data.driver.DefaultDriverRepository
import com.citta.driver.data.driver.OutboxBackedDriverRepository
import com.citta.driver.data.messaging.DefaultFcmTokenRegistrar
import com.citta.driver.data.maintenance.AppCacheCleaner
import com.citta.driver.data.messaging.PrefsRegisteredTokenStore
import com.citta.driver.data.notifications.DriverDatabase
import com.citta.driver.data.notifications.NotificationHistoryDao
import com.citta.driver.data.notifications.RoomNotificationHistoryRepository
import com.citta.driver.data.outbox.FileOutboxQueue
import com.citta.driver.data.outbox.RetrofitOutboxActionSender
import com.citta.driver.data.profile.PrefsDriverProfileStore
import com.citta.driver.data.session.DefaultSessionRepository
import com.citta.driver.data.session.EncryptedTokenStore
import com.citta.driver.data.session.TokenStore
import com.citta.driver.data.shift.PrefsLocalShiftStore
import com.citta.driver.data.tracking.LocationUploadRepository
import com.citta.driver.domain.auth.AuthRepository
import com.citta.driver.domain.driver.DriverRepository
import com.citta.driver.domain.maintenance.CacheCleaner
import com.citta.driver.domain.messaging.FcmTokenRegistrar
import com.citta.driver.domain.messaging.PushEventBus
import com.citta.driver.domain.messaging.RegisteredTokenStore
import com.citta.driver.domain.observability.CrashReporter
import com.citta.driver.domain.observability.Logger
import com.citta.driver.domain.notifications.NotificationHistoryRepository
import com.citta.driver.domain.outbox.OutboxActionSender
import com.citta.driver.domain.outbox.OutboxQueue
import com.citta.driver.domain.outbox.OutboxReplayer
import com.citta.driver.domain.profile.DriverProfileStore
import com.citta.driver.domain.session.SessionRepository
import com.citta.driver.domain.shift.LocalShiftStore
import com.citta.driver.domain.tracking.DefaultTrackingCoordinator
import com.citta.driver.domain.tracking.LocationProvider
import com.citta.driver.domain.tracking.LocationUploader
import com.citta.driver.domain.tracking.TrackingCoordinator
import com.citta.driver.domain.tracking.TrackingSessionController
import com.citta.driver.service.FusedLocationProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import coil.ImageLoader
import coil.imageLoader
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTokenStore(@ApplicationContext context: Context): TokenStore =
        EncryptedTokenStore(context)

    @Provides
    @Singleton
    fun provideSessionRepository(store: TokenStore): SessionRepository =
        DefaultSessionRepository(store)

    @Provides
    @Singleton
    fun provideDriverProfileStore(@ApplicationContext context: Context): DriverProfileStore =
        PrefsDriverProfileStore(context)

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader =
        context.imageLoader

    @Provides
    @Singleton
    fun provideCacheCleaner(
        @ApplicationContext context: Context,
        imageLoader: ImageLoader,
    ): CacheCleaner = AppCacheCleaner(context, imageLoader)

    @Provides
    @Singleton
    fun provideLocationUploader(api: CittaApi): LocationUploader =
        LocationUploadRepository(api)

    @Provides
    @Singleton
    fun provideOutboxQueue(@ApplicationContext context: Context): OutboxQueue =
        FileOutboxQueue(File(context.filesDir, "outbox.json"))

    @Provides
    @Singleton
    fun provideOutboxActionSender(api: CittaApi): OutboxActionSender =
        RetrofitOutboxActionSender(api)

    @Provides
    @Singleton
    fun provideOutboxReplayer(
        queue: OutboxQueue,
        sender: OutboxActionSender,
        crashReporter: CrashReporter,
    ): OutboxReplayer = OutboxReplayer(queue, sender, crashReporter = crashReporter)

    @Provides
    @Singleton
    fun provideLocalShiftStore(@ApplicationContext context: Context): LocalShiftStore =
        PrefsLocalShiftStore(context)

    @Provides
    @Singleton
    fun provideTrackingCoordinator(
        uploader: LocationUploader,
        outbox: OutboxQueue,
        logger: Logger,
    ): TrackingCoordinator = DefaultTrackingCoordinator(uploader, outbox, logger = logger)

    @Provides
    @Singleton
    fun provideLocationProvider(impl: FusedLocationProvider): LocationProvider = impl

    @Provides
    @Singleton
    fun provideTrackingSessionController(
        coordinator: TrackingCoordinator,
        locationProvider: LocationProvider,
    ): TrackingSessionController = TrackingSessionController(coordinator, locationProvider)

    @Provides
    @Singleton
    fun provideAuthRepository(
        api: CittaApi,
        session: SessionRepository,
        profileStore: DriverProfileStore,
    ): AuthRepository = DefaultAuthRepository(api, session, profileStore)

    @Provides
    @Singleton
    fun provideDriverRepository(api: CittaApi, outbox: OutboxQueue): DriverRepository =
        OutboxBackedDriverRepository(DefaultDriverRepository(api), outbox)

    @Provides
    @Singleton
    fun providePushEventBus(): PushEventBus = PushEventBus()

    @Provides
    @Singleton
    fun provideRegisteredTokenStore(@ApplicationContext context: Context): RegisteredTokenStore =
        PrefsRegisteredTokenStore(context)

    @Provides
    @Singleton
    fun provideFcmTokenRegistrar(api: CittaApi, store: RegisteredTokenStore): FcmTokenRegistrar =
        DefaultFcmTokenRegistrar(api, store)

    @Provides
    @Singleton
    fun provideDriverDatabase(@ApplicationContext context: Context): DriverDatabase =
        Room.databaseBuilder(context, DriverDatabase::class.java, "citta-driver.db").build()

    @Provides
    fun provideNotificationHistoryDao(database: DriverDatabase): NotificationHistoryDao =
        database.notificationHistoryDao()

    @Provides
    @Singleton
    fun provideNotificationHistoryRepository(dao: NotificationHistoryDao): NotificationHistoryRepository =
        RoomNotificationHistoryRepository(dao)

    @Provides
    @Singleton
    fun provideOkHttpClient(session: SessionRepository, profileStore: DriverProfileStore): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(AuthInterceptor { session.peekToken() })
            .addInterceptor(PasswordChangeRequiredInterceptor(profileStore))
            .authenticator(ForcedLogoutAuthenticator { session.forceClear() })
            .build()
    }

    @Provides
    @Singleton
    fun provideCittaApi(client: OkHttpClient): CittaApi =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CittaApi::class.java)
}
