package com.salarybox.attendance.wallet.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.PrimaryKey
import com.salarybox.attendance.wallet.domain.AppSettings
import com.salarybox.attendance.wallet.domain.Provider
import com.salarybox.attendance.wallet.domain.Transaction as WalletTransaction
import com.salarybox.attendance.wallet.domain.User
import com.salarybox.attendance.wallet.domain.Wallet
import com.salarybox.attendance.wallet.domain.WalletInputValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.http.GET
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore("wallet_settings")

@Entity(tableName = "wallet")
data class WalletEntity(@PrimaryKey val id: Int = 1, val balancePaise: Long)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subtitle: String,
    val amountPaise: Long,
    val isCredit: Boolean,
    val timestamp: Long,
    val status: String
)

@Entity(tableName = "providers")
data class ProviderEntity(@PrimaryKey val id: Int, val name: String, val detail: String, val category: String)

@Entity(tableName = "profile")
data class UserEntity(@PrimaryKey val id: Int = 1, val name: String, val email: String, val phone: String)

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallet WHERE id = 1") fun wallet(): Flow<WalletEntity?>
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC") fun transactions(): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE id = :id") fun transaction(id: Long): Flow<TransactionEntity?>
    @Query("SELECT * FROM providers ORDER BY name") fun providers(): Flow<List<ProviderEntity>>
    @Query("SELECT * FROM providers WHERE id = :id") fun provider(id: Int): Flow<ProviderEntity?>
    @Query("SELECT * FROM profile WHERE id = 1") fun profile(): Flow<UserEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putWallet(wallet: WalletEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putProfile(profile: UserEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putProviders(providers: List<ProviderEntity>)
    @Insert suspend fun insertTransaction(transaction: TransactionEntity): Long
    @Transaction suspend fun addBalance(amountPaise: Long, transaction: TransactionEntity) {
        val current = wallet().first()?.balancePaise ?: 0L
        putWallet(WalletEntity(balancePaise = current + amountPaise))
        insertTransaction(transaction)
    }
}

@Database(entities = [WalletEntity::class, TransactionEntity::class, ProviderEntity::class, UserEntity::class], version = 1, exportSchema = false)
abstract class WalletDatabase : RoomDatabase() { abstract fun walletDao(): WalletDao }

data class RemoteProvider(val id: Int, val name: String, val email: String, val company: RemoteCompany?)
data class RemoteCompany(val name: String?)
interface ProviderApi { @GET("users") suspend fun providers(): List<RemoteProvider> }

@Singleton
class AppPreferences @Inject constructor(private val context: Context) {
    private val loggedIn = booleanPreferencesKey("logged_in")
    private val darkMode = booleanPreferencesKey("dark_mode")
    private val notifications = booleanPreferencesKey("notifications")
    val isLoggedIn: Flow<Boolean> = context.settingsDataStore.data.map { it[loggedIn] ?: false }
    val settings: Flow<AppSettings> = context.settingsDataStore.data.map {
        AppSettings(it[darkMode] ?: false, it[notifications] ?: true)
    }
    suspend fun setLoggedIn(value: Boolean) { context.settingsDataStore.edit { it[loggedIn] = value } }
    suspend fun setDarkMode(value: Boolean) { context.settingsDataStore.edit { it[darkMode] = value } }
    suspend fun setNotifications(value: Boolean) { context.settingsDataStore.edit { it[notifications] = value } }
}

interface WalletRepository {
    val wallet: Flow<Wallet>
    val transactions: Flow<List<WalletTransaction>>
    val providers: Flow<List<Provider>>
    val profile: Flow<User>
    suspend fun transaction(id: Long): WalletTransaction?
    suspend fun provider(id: Int): Provider?
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun logout()
    suspend fun addMoney(amountPaise: Long): Result<Unit>
    suspend fun sendMoney(name: String, amountPaise: Long): Result<Unit>
    suspend fun syncProviders(): Result<Unit>
    suspend fun updateProfile(name: String, phone: String)
}

@Singleton
class WalletRepositoryImpl @Inject constructor(
    private val dao: WalletDao,
    private val api: ProviderApi,
    private val preferences: AppPreferences
) : WalletRepository {
    override val wallet = dao.wallet().map { Wallet(it?.balancePaise ?: 0) }
    override val transactions = dao.transactions().map { list -> list.map { it.toDomain() } }
    override val providers = dao.providers().map { list -> list.map { it.toDomain() } }
    override val profile = dao.profile().map { it?.let(UserEntity::toDomain) ?: User("Lakshya Kumar", "lakshya@example.com", "+91 98765 43210") }

    override suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        require(WalletInputValidator.loginError(email, password) == null) { WalletInputValidator.loginError(email, password)!! }
        seedIfNeeded()
        preferences.setLoggedIn(true)
    }
    override suspend fun logout() = preferences.setLoggedIn(false)
    override suspend fun addMoney(amountPaise: Long): Result<Unit> = changeBalance("Added money", amountPaise, true)
    override suspend fun sendMoney(name: String, amountPaise: Long): Result<Unit> {
        WalletInputValidator.recipientError(name)?.let { return Result.failure(IllegalArgumentException(it)) }
        return changeBalance("Sent to ${name.trim()}", amountPaise, false)
    }
    private suspend fun changeBalance(title: String, amountPaise: Long, isCredit: Boolean): Result<Unit> = runCatching {
        require(WalletInputValidator.amountError(amountPaise) == null) { WalletInputValidator.amountError(amountPaise)!! }
        val balance = wallet.first().balancePaise
        require(isCredit || balance >= amountPaise) { "Insufficient wallet balance" }
        dao.addBalance(if (isCredit) amountPaise else -amountPaise, TransactionEntity(title = title, subtitle = "SalaryBox Wallet", amountPaise = amountPaise, isCredit = isCredit, timestamp = System.currentTimeMillis(), status = "Completed"))
    }
    override suspend fun syncProviders(): Result<Unit> = runCatching {
        val remote = api.providers().map { ProviderEntity(it.id, it.name, it.email, it.company?.name ?: "Partner") }
        require(remote.isNotEmpty()) { "No provider data received" }
        dao.putProviders(remote)
    }
    override suspend fun transaction(id: Long): WalletTransaction? = dao.transaction(id).first()?.toDomain()
    override suspend fun provider(id: Int): Provider? = dao.provider(id).first()?.toDomain()
    override suspend fun updateProfile(name: String, phone: String) {
        require(name.trim().length in 2..60) { "Enter a valid name" }
        require(phone.filter(Char::isDigit).length in 10..15) { "Enter a valid phone number" }
        val existing = profile.first()
        dao.putProfile(UserEntity(name = name.trim(), email = existing.email, phone = phone.trim()))
    }
    private suspend fun seedIfNeeded() {
        if (dao.wallet().first() == null) dao.putWallet(WalletEntity(balancePaise = 125_000))
        if (dao.profile().first() == null) dao.putProfile(UserEntity(name = "Lakshya Kumar", email = "lakshya@example.com", phone = "+91 98765 43210"))
        if (dao.transactions().first().isEmpty()) dao.insertTransaction(TransactionEntity(title = "Welcome bonus", subtitle = "SalaryBox Wallet", amountPaise = 125_000, isCredit = true, timestamp = System.currentTimeMillis(), status = "Completed"))
    }
}

private fun TransactionEntity.toDomain() = WalletTransaction(id, title, subtitle, amountPaise, isCredit, timestamp, status)
private fun ProviderEntity.toDomain() = Provider(id, name, detail, category)
private fun UserEntity.toDomain() = User(name, email, phone)
