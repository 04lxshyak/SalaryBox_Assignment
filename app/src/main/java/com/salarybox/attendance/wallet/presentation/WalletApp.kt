package com.salarybox.attendance.wallet.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.salarybox.attendance.wallet.domain.Provider
import com.salarybox.attendance.wallet.domain.Transaction
import com.salarybox.attendance.wallet.domain.asRupees
import kotlinx.coroutines.delay

private object Route {
    const val Splash = "splash"; const val Login = "login"; const val Home = "home"; const val Providers = "providers"
    const val Wallet = "wallet"; const val AddMoney = "add_money"; const val SendMoney = "send_money"; const val Transactions = "transactions"
    const val Profile = "profile"; const val Settings = "settings"
    fun provider(id: Int) = "provider/$id"; fun transaction(id: Long) = "transaction/$id"
}

@Composable
fun WalletApp(viewModel: WalletViewModel = hiltViewModel()) {
    val nav = rememberNavController()
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbars = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbars.showSnackbar(it) } }
    LaunchedEffect(loggedIn) {
        if (loggedIn && nav.currentDestination?.route == Route.Login) {
            nav.navigate(Route.Home) { popUpTo(Route.Login) { inclusive = true } }
        }
    }
    MaterialTheme(colorScheme = if (settings.darkMode) darkColorScheme() else lightColorScheme()) {
        Scaffold(snackbarHost = { SnackbarHost(snackbars) }) { padding ->
            NavHost(navController = nav, startDestination = Route.Splash, modifier = Modifier.padding(padding)) {
                composable(Route.Splash) { SplashScreen(loggedIn) { target -> nav.navigate(target) { popUpTo(Route.Splash) { inclusive = true } } } }
                composable(Route.Login) { LoginScreen(onLogin = viewModel::login) }
                composable(Route.Home) { HomeScreen(viewModel, navigate = nav::navigate) }
                composable(Route.Providers) { ProvidersScreen(viewModel, navigate = nav::navigate, back = nav::popBackStack) }
                composable("provider/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { entry -> ProviderDetailScreen(viewModel, entry.arguments?.getInt("id") ?: 0, nav::popBackStack) }
                composable(Route.Wallet) { WalletScreen(viewModel, nav::navigate, nav::popBackStack) }
                composable(Route.AddMoney) { AddMoneyScreen(viewModel::addMoney, nav::popBackStack) }
                composable(Route.SendMoney) { SendMoneyScreen(viewModel::sendMoney, nav::popBackStack) }
                composable(Route.Transactions) { TransactionsScreen(viewModel, nav::navigate, nav::popBackStack) }
                composable("transaction/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry -> TransactionDetailScreen(viewModel, entry.arguments?.getLong("id") ?: 0, nav::popBackStack) }
                composable(Route.Profile) { ProfileScreen(viewModel, nav::navigate, nav::popBackStack) }
                composable(Route.Settings) { SettingsScreen(viewModel, nav::popBackStack) {
                    nav.navigate(Route.Login) { popUpTo(Route.Home) { inclusive = true } }
                } }
            }
        }
    }
}

@Composable private fun SplashScreen(loggedIn: Boolean, continueTo: (String) -> Unit) {
    LaunchedEffect(Unit) { delay(900); continueTo(if (loggedIn) Route.Home else Route.Login) }
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Icon(Icons.Default.AccountBalanceWallet, "SalaryBox Wallet", Modifier.size(76.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp)); Text("SalaryBox Wallet", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp)); CircularProgressIndicator()
    }
}

@Composable private fun LoginScreen(onLogin: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp).testTag("login_screen"), Arrangement.Center) {
        Text("Welcome back", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Sign in to manage your wallet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth().testTag("login_email"), label = { Text("Email") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth().testTag("login_password"), label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
        Spacer(Modifier.height(20.dp))
        Button({ onLogin(email, password) }, Modifier.fillMaxWidth().testTag("login_submit")) { Text("Sign in") }
        Spacer(Modifier.height(12.dp)); Text("Demo: use any valid email and a 4+ character password.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun HomeScreen(vm: WalletViewModel, navigate: (String) -> Unit) {
    val wallet by vm.wallet.collectAsStateWithLifecycle(); val transactions by vm.transactions.collectAsStateWithLifecycle(); val profile by vm.profile.collectAsStateWithLifecycle()
    Screen("Home", null) {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp).testTag("home_screen"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Good to see you, ${profile.name.substringBefore(" ").ifBlank { "there" }}", style = MaterialTheme.typography.titleLarge) }
            item { BalanceCard(wallet.balancePaise, { navigate(Route.AddMoney) }, { navigate(Route.SendMoney) }) }
            item { Text("Quick access", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { QuickAction("Providers", Icons.Default.Business) { navigate(Route.Providers) }; QuickAction("Wallet", Icons.Default.AccountBalanceWallet) { navigate(Route.Wallet) } } }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { QuickAction("Transactions", Icons.Default.History) { navigate(Route.Transactions) }; QuickAction("Profile", Icons.Default.Person) { navigate(Route.Profile) } } }
            item { QuickAction("Settings", Icons.Default.Settings) { navigate(Route.Settings) } }
            item { Text("Recent activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            if (transactions.isEmpty()) item { EmptyState("No transactions yet") } else items(transactions.take(3), key = { it.id }) { TransactionRow(it) { navigate(Route.transaction(it.id)) } }
        }
    }
}

@Composable private fun BalanceCard(balance: Long, add: () -> Unit, send: () -> Unit) = Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
    Column(Modifier.padding(20.dp)) { Text("Available balance"); Text(balance.asRupees(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold); Spacer(Modifier.height(16.dp)); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Button(add) { Icon(Icons.Default.Add, "Add money"); Spacer(Modifier.width(6.dp)); Text("Add money") }; OutlinedButton(send) { Icon(Icons.AutoMirrored.Filled.Send, "Send money"); Spacer(Modifier.width(6.dp)); Text("Send") } } }
}

@Composable private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit) = OutlinedButton(action, Modifier.height(54.dp).weight(1f)) { Icon(icon, label); Spacer(Modifier.width(8.dp)); Text(label) }

@Composable private fun ProvidersScreen(vm: WalletViewModel, navigate: (String) -> Unit, back: () -> Boolean) {
    val providers by vm.providers.collectAsStateWithLifecycle()
    Screen("Providers", back, action = { IconButton(vm::syncProviders) { Icon(Icons.Default.Refresh, "Refresh providers") } }) {
        if (providers.isEmpty()) EmptyState("No cached providers. Refresh to sync.") else LazyColumn(Modifier.fillMaxSize()) { items(providers, key = { it.id }) { provider -> ListItem(headlineContent = { Text(provider.name) }, supportingContent = { Text(provider.category) }, leadingContent = { Icon(Icons.Default.Business, "Provider") }, modifier = Modifier.clickable { navigate(Route.provider(provider.id)) }) } }
    }
}

@Composable private fun WalletScreen(vm: WalletViewModel, navigate: (String) -> Unit, back: () -> Boolean) { val wallet by vm.wallet.collectAsStateWithLifecycle(); Screen("Wallet", back) { Column(Modifier.padding(16.dp)) { BalanceCard(wallet.balancePaise, { navigate(Route.AddMoney) }, { navigate(Route.SendMoney) }); Spacer(Modifier.height(16.dp)); OutlinedButton({ navigate(Route.Transactions) }, Modifier.fillMaxWidth()) { Icon(Icons.Default.History, "Transaction history"); Spacer(Modifier.width(8.dp)); Text("Transaction history") } } } }

@Composable private fun AddMoneyScreen(addMoney: (Long) -> Unit, back: () -> Boolean) = AmountScreen("Add money", "Amount to add", Icons.Default.ArrowDownward, { addMoney(it) }, back, "add_money")
@Composable private fun SendMoneyScreen(sendMoney: (String, Long) -> Unit, back: () -> Boolean) { var name by remember { mutableStateOf("") }; AmountScreen("Send money", "Amount to send", Icons.Default.ArrowUpward, { amount -> sendMoney(name, amount) }, back, "send_money") { OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth().testTag("send_money_name"), label = { Text("Recipient name") }, singleLine = true); Spacer(Modifier.height(12.dp)) } }

@Composable private fun AmountScreen(title: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: (Long) -> Unit, back: () -> Boolean, tag: String, prefix: @Composable () -> Unit = {}) {
    var amount by remember { mutableStateOf("") }; Screen(title, back) { Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, title, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(18.dp)); prefix(); OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, Modifier.fillMaxWidth().testTag("${tag}_amount"), label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)); Spacer(Modifier.height(16.dp)); Button({ action(amount.toLongOrNull()?.times(100) ?: 0) }, Modifier.fillMaxWidth().testTag("${tag}_submit")) { Text(title) } } }
}

@Composable private fun TransactionsScreen(vm: WalletViewModel, navigate: (String) -> Unit, back: () -> Boolean) { val transactions by vm.transactions.collectAsStateWithLifecycle(); Screen("Transactions", back) { if (transactions.isEmpty()) EmptyState("No transactions yet") else LazyColumn(Modifier.fillMaxSize()) { items(transactions, key = { it.id }) { TransactionRow(it) { navigate(Route.transaction(it.id)) } } } } }
@Composable private fun TransactionRow(tx: Transaction, open: () -> Unit) = ListItem(headlineContent = { Text(tx.title) }, supportingContent = { Text(tx.subtitle) }, leadingContent = { Icon(if (tx.isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, if (tx.isCredit) "Money received" else "Money sent") }, trailingContent = { Text((if (tx.isCredit) "+" else "-") + tx.amountPaise.asRupees(), color = if (tx.isCredit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }, modifier = Modifier.clickable(onClick = open))

@Composable private fun ProfileScreen(vm: WalletViewModel, navigate: (String) -> Unit, back: () -> Boolean) { val profile by vm.profile.collectAsStateWithLifecycle(); var name by remember(profile.name) { mutableStateOf(profile.name) }; var phone by remember(profile.phone) { mutableStateOf(profile.phone) }; Screen("Profile", back) { Column(Modifier.padding(16.dp)) { OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Name") }); Spacer(Modifier.height(12.dp)); OutlinedTextField(profile.email, { _ -> }, Modifier.fillMaxWidth(), label = { Text("Email") }, readOnly = true); Spacer(Modifier.height(12.dp)); OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("Phone") }); Spacer(Modifier.height(16.dp)); Button({ vm.updateProfile(name, phone) }, Modifier.fillMaxWidth()) { Text("Save profile") }; Spacer(Modifier.height(10.dp)); OutlinedButton({ navigate(Route.Settings) }, Modifier.fillMaxWidth()) { Icon(Icons.Default.Settings, "Settings"); Spacer(Modifier.width(8.dp)); Text("Settings") } } } }

@Composable private fun SettingsScreen(vm: WalletViewModel, back: () -> Boolean, loggedOut: () -> Unit) { val settings by vm.settings.collectAsStateWithLifecycle(); Screen("Settings", back) { Column(Modifier.padding(16.dp)) { SettingRow("Dark mode", settings.darkMode) { vm.setDarkMode(it) }; SettingRow("Transaction notifications", settings.notificationsEnabled) { vm.setNotifications(it) }; Spacer(Modifier.height(20.dp)); OutlinedButton({ vm.logout(); loggedOut() }, Modifier.fillMaxWidth()) { Icon(Icons.Default.Logout, "Log out"); Spacer(Modifier.width(8.dp)); Text("Log out") } } } }
@Composable private fun SettingRow(label: String, checked: Boolean, update: (Boolean) -> Unit) = Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f)); Switch(checked, update) }

@Composable private fun ProviderDetailScreen(vm: WalletViewModel, id: Int, back: () -> Boolean) { val providers by vm.providers.collectAsStateWithLifecycle(); Screen("Provider details", back) { providers.firstOrNull { it.id == id }?.let(::ProviderDetails) ?: EmptyState("Provider unavailable") } }
@Composable private fun TransactionDetailScreen(vm: WalletViewModel, id: Long, back: () -> Boolean) { val transactions by vm.transactions.collectAsStateWithLifecycle(); Screen("Transaction details", back) { transactions.firstOrNull { it.id == id }?.let(::TransactionDetails) ?: EmptyState("Transaction unavailable") } }
@Composable private fun ProviderDetails(provider: Provider) = Column(Modifier.padding(16.dp)) { Text(provider.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp)); Text("Category: ${provider.category}"); Spacer(Modifier.height(8.dp)); Text(provider.detail) }
@Composable private fun TransactionDetails(tx: Transaction) = Column(Modifier.padding(16.dp)) { Text(tx.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp)); Text(tx.amountPaise.asRupees(), style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(8.dp)); Text("Status: ${tx.status}"); Text(tx.subtitle) }

@OptIn(ExperimentalMaterial3Api::class) @Composable private fun Screen(title: String, back: (() -> Boolean)?, action: @Composable (() -> Unit)? = null, content: @Composable () -> Unit) = Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text(title) }, navigationIcon = { if (back != null) IconButton({ back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, actions = { action?.invoke() }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors()) }) { inner -> Column(Modifier.fillMaxSize().padding(inner)) { content() } }
@Composable private fun EmptyState(message: String) = Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) }
