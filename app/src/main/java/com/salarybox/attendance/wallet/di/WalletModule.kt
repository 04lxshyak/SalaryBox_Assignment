package com.salarybox.attendance.wallet.di

import android.content.Context
import androidx.room.Room
import com.salarybox.attendance.wallet.data.ProviderApi
import com.salarybox.attendance.wallet.data.WalletDao
import com.salarybox.attendance.wallet.data.WalletDatabase
import com.salarybox.attendance.wallet.data.WalletRepository
import com.salarybox.attendance.wallet.data.WalletRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WalletBindings {
    @Binds @Singleton abstract fun bindRepository(impl: WalletRepositoryImpl): WalletRepository
}

@Module
@InstallIn(SingletonComponent::class)
object WalletModule {
    @Provides @Singleton fun provideDatabase(@ApplicationContext context: Context): WalletDatabase =
        Room.databaseBuilder(context, WalletDatabase::class.java, "salarybox_wallet.db").fallbackToDestructiveMigration().build()
    @Provides fun provideWalletDao(database: WalletDatabase): WalletDao = database.walletDao()
    @Provides @Singleton fun provideHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.NONE }).build()
    @Provides @Singleton fun provideApi(client: OkHttpClient): ProviderApi = Retrofit.Builder()
        .baseUrl("https://jsonplaceholder.typicode.com/").client(client)
        .addConverterFactory(GsonConverterFactory.create()).build().create(ProviderApi::class.java)
}
