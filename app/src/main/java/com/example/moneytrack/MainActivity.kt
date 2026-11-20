package com.example.moneytrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneytrack.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

val WarningAmber = Color(0xFFF39C12)

enum class TransactionType { INCOME, EXPENSE }

enum class TransactionCategory(
    val displayName: String,
    val type: TransactionType,
    val icon: ImageVector,
    val chartColor: Color
) {
    // Categorías de Gastos
    ALIMENTACION("Alimentación", TransactionType.EXPENSE, Icons.Rounded.Restaurant, Color(0xFF2ECC71)),
    TRANSPORTE("Transporte", TransactionType.EXPENSE, Icons.Rounded.DirectionsBus, Color(0xFF3498DB)),
    VIVIENDA("Vivienda", TransactionType.EXPENSE, Icons.Rounded.HomeWork, Color(0xFFE67E22)),
    OCIO("Ocio", TransactionType.EXPENSE, Icons.Rounded.SportsEsports, Color(0xFF9B59B6)),
    COMPRAS("Compras", TransactionType.EXPENSE, Icons.Rounded.ShoppingBag, Color(0xFFE74C3C)),
    OTROS_GASTOS("Otros gastos", TransactionType.EXPENSE, Icons.Rounded.MoreHoriz, Color(0xFF95A5A6)),

    // Categorías de Ingresos
    SALARIO("Salario", TransactionType.INCOME, Icons.Rounded.Work, EmeraldPrimary),
    BECAS("Becas", TransactionType.INCOME, Icons.Rounded.School, Color(0xFF1ABC9C)),
    VENTAS("Ventas", TransactionType.INCOME, Icons.Rounded.Storefront, Color(0xFF27AE60)),
    OTROS_INGRESOS("Otros ingresos", TransactionType.INCOME, Icons.Rounded.Payments, Color(0xFF58D68D));

    companion object {
        fun forType(type: TransactionType): List<TransactionCategory> =
            entries.filter { it.type == type }

        fun defaultFor(type: TransactionType): TransactionCategory =
            if (type == TransactionType.INCOME) SALARIO else ALIMENTACION
    }
}

enum class TransactionFilter(val label: String) {
    ALL("Todos"),
    INCOME("Ingresos"),
    EXPENSE("Gastos")
}

enum class AppScreen { HOME, HISTORY, BUDGETS, STATS }

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double,
    val category: TransactionCategory,
    val dateMillis: Long,
    val type: TransactionType
)

sealed class FormMode {
    data class Create(val defaultType: TransactionType) : FormMode()
    data class Edit(val transaction: Transaction) : FormMode()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoneyTrackTheme {
                MoneyTrackApp()
            }
        }
    }
}

@Composable
fun MoneyTrackApp() {
    val now = System.currentTimeMillis()
    val dayMs = 86_400_000L

    val transactions = remember {
        mutableStateListOf(
            Transaction(
                name = "Nómina mensual",
                amount = 1950.00,
                category = TransactionCategory.SALARIO,
                dateMillis = now - (dayMs * 4),
                type = TransactionType.INCOME
            ),
            Transaction(
                name = "Alquiler piso",
                amount = 650.00,
                category = TransactionCategory.VIVIENDA,
                dateMillis = now - (dayMs * 3),
                type = TransactionType.EXPENSE
            ),
            Transaction(
                name = "Compra semanal",
                amount = 145.40,
                category = TransactionCategory.ALIMENTACION,
                dateMillis = now - (dayMs * 2),
                type = TransactionType.EXPENSE
            ),
            Transaction(
                name = "Venta portátil antiguo",
                amount = 240.00,
                category = TransactionCategory.VENTAS,
                dateMillis = now - dayMs,
                type = TransactionType.INCOME
            ),
            Transaction(
                name = "Cine y cena",
                amount = 52.00,
                category = TransactionCategory.OCIO,
                dateMillis = now,
                type = TransactionType.EXPENSE
            ),
            Transaction(
                name = "Abono transporte",
                amount = 30.00,
                category = TransactionCategory.TRANSPORTE,
                dateMillis = now,
                type = TransactionType.EXPENSE
            )
        )
    }

    var monthlyBudgetLimit by remember { mutableDoubleStateOf(1200.00) }
    val categoryBudgets = remember {
        mutableStateMapOf(
            TransactionCategory.ALIMENTACION to 300.00,
            TransactionCategory.OCIO to 120.00,
            TransactionCategory.TRANSPORTE to 80.00
        )
    }

    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var formMode by remember { mutableStateOf<FormMode?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.HOME,
                    onClick = { currentScreen = AppScreen.HOME },
                    icon = { Icon(Icons.Rounded.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = navItemColors()
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.HISTORY,
                    onClick = { currentScreen = AppScreen.HISTORY },
                    icon = { Icon(Icons.Rounded.ReceiptLong, contentDescription = "Movimientos") },
                    label = { Text("Movimientos", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = navItemColors()
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.BUDGETS,
                    onClick = { currentScreen = AppScreen.BUDGETS },
                    icon = { Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = "Presupuesto") },
                    label = { Text("Presupuesto", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = navItemColors()
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.STATS,
                    onClick = { currentScreen = AppScreen.STATS },
                    icon = { Icon(Icons.Rounded.PieChart, contentDescription = "Estadísticas") },
                    label = { Text("Estadísticas", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = navItemColors()
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                AppScreen.HOME -> MoneyTrackHomeScreen(
                    transactions = transactions,
                    onOpenForm = { formMode = it },
                    onDeleteTransaction = { transactions.remove(it) },
                    onSeeAllClick = { currentScreen = AppScreen.HISTORY }
                )
                AppScreen.HISTORY -> AllTransactionsScreen(
                    transactions = transactions,
                    onOpenForm = { formMode = it },
                    onDeleteTransaction = { transactions.remove(it) }
                )
                AppScreen.BUDGETS -> BudgetsScreen(
                    transactions = transactions,
                    monthlyBudgetLimit = monthlyBudgetLimit,
                    onUpdateMonthlyLimit = { monthlyBudgetLimit = it },
                    categoryBudgets = categoryBudgets,
                    onSaveCategoryBudget = { category, limit ->
                        categoryBudgets[category] = limit
                    },
                    onRemoveCategoryBudget = { category ->
                        categoryBudgets.remove(category)
                    }
                )
                AppScreen.STATS -> StatisticsScreen(
                    transactions = transactions
                )
            }
        }

        formMode?.let { mode ->
            TransactionFormDialog(
                mode = mode,
                onDismiss = { formMode = null },
                onSave = { savedTransaction ->
                    when (mode) {
                        is FormMode.Create -> transactions.add(0, savedTransaction)
                        is FormMode.Edit -> {
                            val index = transactions.indexOfFirst { it.id == savedTransaction.id }
                            if (index != -1) transactions[index] = savedTransaction
                        }
                    }
                    formMode = null
                }
            )
        }
    }
}

@Composable
private fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = DarkBackground,
    selectedTextColor = EmeraldPrimary,
    indicatorColor = EmeraldPrimary,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
)

// ==========================================
// PANTALLA 1: INICIO (RESUMEN PRINCIPAL)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyTrackHomeScreen(
    transactions: List<Transaction>,
    onOpenForm: (FormMode) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onSeeAllClick: () -> Unit
) {
    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val availableBalance = totalIncome - totalExpense

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "MoneyTrack",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Control de finanzas personales",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        MainBalanceCard(
            availableBalance = availableBalance,
            totalIncome = totalIncome,
            totalExpense = totalExpense
        )

        Spacer(modifier = Modifier.height(16.dp))

        QuickActionButtons(
            onAddIncomeClick = { onOpenForm(FormMode.Create(TransactionType.INCOME)) },
            onAddExpenseClick = { onOpenForm(FormMode.Create(TransactionType.EXPENSE)) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Últimos movimientos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = onSeeAllClick) {
                Text(
                    text = "Ver todos",
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val recentList = transactions.sortedByDescending { it.dateMillis }.take(5)

        if (recentList.isEmpty()) {
            EmptyTransactionsCard("Aún no tienes movimientos registrados")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(recentList, key = { it.id }) { item ->
                    MovementItemCard(
                        transaction = item,
                        onEdit = { onOpenForm(FormMode.Edit(item)) },
                        onDelete = { onDeleteTransaction(item) }
                    )
                }
            }
        }
    }
}

// ==========================================
// PANTALLA 2: TODOS LOS MOVIMIENTOS + FILTROS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTransactionsScreen(
    transactions: List<Transaction>,
    onOpenForm: (FormMode) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(TransactionFilter.ALL) }

    val filteredTransactions = remember(transactions.toList(), selectedFilter) {
        transactions
            .filter { tx ->
                when (selectedFilter) {
                    TransactionFilter.ALL -> true
                    TransactionFilter.INCOME -> tx.type == TransactionType.INCOME
                    TransactionFilter.EXPENSE -> tx.type == TransactionType.EXPENSE
                }
            }
            .sortedByDescending { it.dateMillis }
    }

    val filteredTotal = remember(filteredTransactions, selectedFilter) {
        when (selectedFilter) {
            TransactionFilter.ALL -> {
                val inc = filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val exp = filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                inc - exp
            }
            TransactionFilter.INCOME -> filteredTransactions.sumOf { it.amount }
            TransactionFilter.EXPENSE -> filteredTransactions.sumOf { it.amount }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Todos los movimientos",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Consulta y filtra tus ingresos y gastos",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TransactionFilter.entries.forEach { filter ->
                val isSelected = selectedFilter == filter
                val chipAccent = when (filter) {
                    TransactionFilter.ALL -> EmeraldPrimary
                    TransactionFilter.INCOME -> EmeraldPrimary
                    TransactionFilter.EXPENSE -> ExpenseRed
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter.label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = chipAccent.copy(alpha = 0.2f),
                        selectedLabelColor = chipAccent
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MaterialTheme.colorScheme.surfaceVariant,
                        selectedBorderColor = chipAccent.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (selectedFilter) {
                            TransactionFilter.ALL -> "Balance neto (${filteredTransactions.size})"
                            TransactionFilter.INCOME -> "Total ingresos (${filteredTransactions.size})"
                            TransactionFilter.EXPENSE -> "Total gastos (${filteredTransactions.size})"
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val amountColor = when (selectedFilter) {
                        TransactionFilter.ALL -> if (filteredTotal >= 0) EmeraldPrimary else ExpenseRed
                        TransactionFilter.INCOME -> EmeraldPrimary
                        TransactionFilter.EXPENSE -> ExpenseRed
                    }
                    val prefix = when (selectedFilter) {
                        TransactionFilter.INCOME -> "+"
                        TransactionFilter.EXPENSE -> "-"
                        TransactionFilter.ALL -> ""
                    }
                    Text(
                        text = "$prefix${formatCurrency(filteredTotal)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredTransactions.isEmpty()) {
            val emptyMessage = when (selectedFilter) {
                TransactionFilter.ALL -> "No hay movimientos registrados"
                TransactionFilter.INCOME -> "No hay ingresos registrados"
                TransactionFilter.EXPENSE -> "No hay gastos registrados"
            }
            EmptyTransactionsCard(emptyMessage)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredTransactions, key = { it.id }) { item ->
                    MovementItemCard(
                        transaction = item,
                        onEdit = { onOpenForm(FormMode.Edit(item)) },
                        onDelete = { onDeleteTransaction(item) }
                    )
                }
            }
        }
    }
}

// ==========================================
// PANTALLA 3: PRESUPUESTOS MENSUALES
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    transactions: List<Transaction>,
    monthlyBudgetLimit: Double,
    onUpdateMonthlyLimit: (Double) -> Unit,
    categoryBudgets: Map<TransactionCategory, Double>,
    onSaveCategoryBudget: (TransactionCategory, Double) -> Unit,
    onRemoveCategoryBudget: (TransactionCategory) -> Unit
) {
    var showEditMonthlyDialog by remember { mutableStateOf(false) }
    var editingCategoryBudget by remember { mutableStateOf<Pair<TransactionCategory, Double?>?>(null) }

    val currentMonthExpenses = remember(transactions.toList()) {
        transactions.filter { it.type == TransactionType.EXPENSE && isCurrentMonth(it.dateMillis) }
    }

    val totalMonthlySpent = remember(currentMonthExpenses) {
        currentMonthExpenses.sumOf { it.amount }
    }

    val spentByCategory = remember(currentMonthExpenses) {
        currentMonthExpenses
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    val currentMonthName = remember {
        SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("es-ES"))
            .format(Date())
            .replaceFirstChar { it.uppercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Presupuestos",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Límites de gasto • $currentMonthName",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        MonthlyGlobalBudgetCard(
            limit = monthlyBudgetLimit,
            spent = totalMonthlySpent,
            onEditLimitClick = { showEditMonthlyDialog = true }
        )

        Spacer(modifier = Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Límites por categoría",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            FilledTonalButton(
                onClick = {
                    val unconfigured = TransactionCategory.forType(TransactionType.EXPENSE)
                        .firstOrNull { !categoryBudgets.containsKey(it) }
                        ?: TransactionCategory.ALIMENTACION
                    editingCategoryBudget = unconfigured to categoryBudgets[unconfigured]
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = EmeraldPrimary.copy(alpha = 0.18f),
                    contentColor = EmeraldPrimary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Límite", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (categoryBudgets.isEmpty()) {
            EmptyTransactionsCard("No tienes límites por categoría configurados")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                categoryBudgets.forEach { (category, limit) ->
                    val spent = spentByCategory[category] ?: 0.0
                    CategoryBudgetCard(
                        category = category,
                        spent = spent,
                        limit = limit,
                        onEdit = { editingCategoryBudget = category to limit },
                        onDelete = { onRemoveCategoryBudget(category) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showEditMonthlyDialog) {
        EditMonthlyLimitDialog(
            currentLimit = monthlyBudgetLimit,
            onDismiss = { showEditMonthlyDialog = false },
            onSave = { newLimit ->
                onUpdateMonthlyLimit(newLimit)
                showEditMonthlyDialog = false
            }
        )
    }

    editingCategoryBudget?.let { (initialCategory, initialLimit) ->
        CategoryBudgetDialog(
            initialCategory = initialCategory,
            initialLimit = initialLimit,
            existingBudgets = categoryBudgets,
            onDismiss = { editingCategoryBudget = null },
            onSave = { selectedCat, newLimit ->
                onSaveCategoryBudget(selectedCat, newLimit)
                editingCategoryBudget = null
            }
        )
    }
}

@Composable
fun MonthlyGlobalBudgetCard(
    limit: Double,
    spent: Double,
    onEditLimitClick: () -> Unit
) {
    val remaining = limit - spent
    val rawRatio = if (limit > 0) (spent / limit).toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = rawRatio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "monthlyBudgetProgress"
    )

    val statusColor = when {
        spent > limit -> ExpenseRed
        rawRatio >= 0.8f -> WarningAmber
        else -> EmeraldPrimary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Presupuesto mensual",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatCurrency(limit),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onEditLimitClick,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Editar presupuesto mensual",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(rawRatio * 100).toInt()}% utilizado",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
                if (remaining < 0) {
                    Text(
                        text = "Excedido por ${formatCurrency(-remaining)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ExpenseRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Gastado este mes",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCurrency(spent),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (remaining >= 0) "Te queda disponible" else "Sobre el límite",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCurrency(if (remaining >= 0) remaining else -remaining),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (remaining >= 0) EmeraldPrimary else ExpenseRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBudgetCard(
    category: TransactionCategory,
    spent: Double,
    limit: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val remaining = limit - spent
    val rawRatio = if (limit > 0) (spent / limit).toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = rawRatio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "catBudgetProgress"
    )

    val statusColor = when {
        spent > limit -> ExpenseRed
        rawRatio >= 0.8f -> WarningAmber
        else -> EmeraldPrimary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onEdit() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(category.chartColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = category.chartColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${formatCurrency(spent)} de ${formatCurrency(limit)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (remaining >= 0) "Quedan" else "Excedido",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(if (remaining >= 0) remaining else -remaining),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Eliminar límite",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun EditMonthlyLimitDialog(
    currentLimit: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var limitText by remember {
        mutableStateOf(String.format(Locale.US, "%.2f", currentLimit))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Presupuesto mensual",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Establece cuánto dinero máximo quieres gastar al mes en total.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Límite mensual (€)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = limitText.replace(",", ".").toDoubleOrNull()
                    if (parsed != null && parsed > 0) {
                        onSave(parsed)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = DarkBackground
                )
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryBudgetDialog(
    initialCategory: TransactionCategory,
    initialLimit: Double?,
    existingBudgets: Map<TransactionCategory, Double>,
    onDismiss: () -> Unit,
    onSave: (TransactionCategory, Double) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var limitText by remember {
        mutableStateOf(
            (initialLimit ?: existingBudgets[initialCategory])
                ?.let { String.format(Locale.US, "%.2f", it) } ?: ""
        )
    }

    val expenseCategories = remember { TransactionCategory.forType(TransactionType.EXPENSE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Límite por categoría",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Selecciona una categoría de gasto y define su tope mensual:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    expenseCategories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategory = cat
                                limitText = existingBudgets[cat]
                                    ?.let { String.format(Locale.US, "%.2f", it) } ?: ""
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(cat.displayName, fontSize = 12.sp) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = EmeraldPrimary.copy(alpha = 0.22f),
                                selectedLabelColor = EmeraldPrimary,
                                selectedLeadingIconColor = EmeraldPrimary
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Límite mensual para ${selectedCategory.displayName} (€)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = limitText.replace(",", ".").toDoubleOrNull()
                    if (parsed != null && parsed > 0) {
                        onSave(selectedCategory, parsed)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = DarkBackground
                )
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

// ==========================================
// PANTALLA 4: ESTADÍSTICAS Y GRÁFICOS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(transactions: List<Transaction>) {
    val snapshotList = transactions.toList()

    val totalIncome = remember(snapshotList) {
        snapshotList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }
    val totalExpense = remember(snapshotList) {
        snapshotList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    val expensesByCategory = remember(snapshotList) {
        snapshotList
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Estadísticas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Análisis visual de tus finanzas",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        IncomeVsExpenseChartCard(
            totalIncome = totalIncome,
            totalExpense = totalExpense
        )

        Spacer(modifier = Modifier.height(16.dp))

        ExpensesByCategoryCard(
            expensesByCategory = expensesByCategory,
            totalExpense = totalExpense
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun IncomeVsExpenseChartCard(
    totalIncome: Double,
    totalExpense: Double
) {
    val maxVal = maxOf(totalIncome, totalExpense, 1.0)
    val incomeRatio by animateFloatAsState(
        targetValue = (totalIncome / maxVal).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "incomeBar"
    )
    val expenseRatio by animateFloatAsState(
        targetValue = (totalExpense / maxVal).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "expenseBar"
    )

    val savingsRate = if (totalIncome > 0) {
        (((totalIncome - totalExpense) / totalIncome) * 100).toInt()
    } else {
        0
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ingresos vs. Gastos",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = (if (savingsRate >= 0) EmeraldPrimary else ExpenseRed).copy(alpha = 0.16f)
                ) {
                    Text(
                        text = if (totalIncome > 0) "Ahorro: $savingsRate%" else "Sin ingresos",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (savingsRate >= 0) EmeraldPrimary else ExpenseRed,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                ComparisonBarColumn(
                    label = "Ingresos",
                    amount = totalIncome,
                    fillRatio = incomeRatio,
                    barColor = EmeraldPrimary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(24.dp))
                ComparisonBarColumn(
                    label = "Gastos",
                    amount = totalExpense,
                    fillRatio = expenseRatio,
                    barColor = ExpenseRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ComparisonBarColumn(
    label: String,
    amount: Double,
    fillRatio: Float,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = formatCurrency(amount),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = barColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(56.dp)
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction = if (amount > 0) fillRatio.coerceAtLeast(0.06f) else 0f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(barColor)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ExpensesByCategoryCard(
    expensesByCategory: List<Pair<TransactionCategory, Double>>,
    totalExpense: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Gastos por categoría",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (expensesByCategory.isEmpty() || totalExpense <= 0.0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Registra algún gasto para ver la distribución por categorías",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DonutChart(
                        slices = expensesByCategory.map { (cat, amount) ->
                            cat.chartColor to (amount / totalExpense).toFloat()
                        },
                        modifier = Modifier.size(165.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total gastado",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(totalExpense),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    expensesByCategory.forEach { (category, amount) ->
                        val percentage = (amount / totalExpense).toFloat().coerceIn(0f, 1f)
                        CategoryBreakdownRow(
                            category = category,
                            amount = amount,
                            percentage = percentage
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChart(
    slices: List<Pair<Color, Float>>,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier = modifier) {
        val strokeWidth = 24.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
        val arcSize = Size(diameter, diameter)

        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth)
        )

        var startAngle = -90f
        val gapDegrees = if (slices.size > 1) 3f else 0f

        slices.forEach { (color, fraction) ->
            val sweepAngle = (fraction * 360f) - gapDegrees
            if (sweepAngle > 0f) {
                drawArc(
                    color = color,
                    startAngle = startAngle + (gapDegrees / 2f),
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
            }
            startAngle += fraction * 360f
        }
    }
}

@Composable
fun CategoryBreakdownRow(
    category: TransactionCategory,
    amount: Double,
    percentage: Float
) {
    val animatedProgress by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(durationMillis = 500),
        label = "catProgress"
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(category.chartColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = category.chartColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = category.displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(percentage * 100).toInt()}%",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = formatCurrency(amount),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = category.chartColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

// ==========================================
// COMPONENTES COMPARTIDOS
// ==========================================
@Composable
fun MainBalanceCard(
    availableBalance: Double,
    totalIncome: Double,
    totalExpense: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Dinero disponible",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatCurrency(availableBalance),
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (availableBalance >= 0) EmeraldPrimary else ExpenseRed
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricSubCard(
                    title = "Total ingresos",
                    amount = totalIncome,
                    accentColor = EmeraldPrimary,
                    icon = Icons.Rounded.ArrowUpward,
                    modifier = Modifier.weight(1f)
                )
                MetricSubCard(
                    title = "Total gastos",
                    amount = totalExpense,
                    accentColor = ExpenseRed,
                    icon = Icons.Rounded.ArrowDownward,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MetricSubCard(
    title: String,
    amount: Double,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatCurrency(amount),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun QuickActionButtons(
    onAddIncomeClick: () -> Unit,
    onAddExpenseClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onAddIncomeClick,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary,
                contentColor = DarkBackground
            )
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Ingreso", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        OutlinedButton(
            onClick = onAddExpenseClick,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, ExpenseRed.copy(alpha = 0.6f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = ExpenseRed
            )
        ) {
            Icon(Icons.Rounded.Remove, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Gasto", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun MovementItemCard(
    transaction: Transaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val accentColor = if (isIncome) EmeraldPrimary else ExpenseRed
    val sign = if (isIncome) "+" else "-"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onEdit() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = transaction.category.icon,
                    contentDescription = transaction.category.displayName,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${transaction.category.displayName} • ${formatDate(transaction.dateMillis)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "$sign${formatCurrency(transaction.amount)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Eliminar movimiento",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyTransactionsCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFormDialog(
    mode: FormMode,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    val existing = (mode as? FormMode.Edit)?.transaction
    val initialType = existing?.type ?: (mode as FormMode.Create).defaultType

    var type by remember { mutableStateOf(initialType) }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var amountText by remember {
        mutableStateOf(existing?.amount?.let { String.format(Locale.US, "%.2f", it) } ?: "")
    }
    var selectedCategory by remember {
        mutableStateOf(existing?.category ?: TransactionCategory.defaultFor(initialType))
    }
    var dateMillis by remember { mutableStateOf(existing?.dateMillis ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val isIncome = type == TransactionType.INCOME
    val accentColor = if (isIncome) EmeraldPrimary else ExpenseRed
    val availableCategories = remember(type) { TransactionCategory.forType(type) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = if (existing != null) "Editar movimiento" else "Nuevo movimiento",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isIncome,
                        onClick = {
                            if (type != TransactionType.INCOME) {
                                type = TransactionType.INCOME
                                selectedCategory = TransactionCategory.defaultFor(TransactionType.INCOME)
                            }
                        },
                        label = {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("Ingreso", fontWeight = FontWeight.SemiBold)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isIncome,
                        onClick = {
                            if (type != TransactionType.EXPENSE) {
                                type = TransactionType.EXPENSE
                                selectedCategory = TransactionCategory.defaultFor(TransactionType.EXPENSE)
                            }
                        },
                        label = {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("Gasto", fontWeight = FontWeight.SemiBold)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ExpenseRed.copy(alpha = 0.2f),
                            selectedLabelColor = ExpenseRed
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    placeholder = { Text(if (isIncome) "Ej. Nómina, Venta..." else "Ej. Supermercado...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Cantidad (€)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Categoría",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        availableCategories.forEach { categoryItem ->
                            val isCatSelected = selectedCategory == categoryItem
                            FilterChip(
                                selected = isCatSelected,
                                onClick = { selectedCategory = categoryItem },
                                leadingIcon = {
                                    Icon(
                                        imageVector = categoryItem.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = categoryItem.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isCatSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    selectedContainerColor = accentColor.copy(alpha = 0.22f),
                                    selectedLabelColor = accentColor,
                                    selectedLeadingIconColor = accentColor
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isCatSelected,
                                    borderColor = Color.Transparent,
                                    selectedBorderColor = accentColor.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }

                OutlinedCard(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Fecha",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatDate(dateMillis),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "Seleccionar fecha",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.replace(",", ".").toDoubleOrNull()
                    if (name.isNotBlank() && amount != null && amount > 0) {
                        onSave(
                            Transaction(
                                id = existing?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                amount = amount,
                                category = selectedCategory,
                                dateMillis = dateMillis,
                                type = type
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = if (isIncome) DarkBackground else Color.White
                )
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateMillis = it }
                        showDatePicker = false
                    }
                ) {
                    Text("Aceptar", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

fun isCurrentMonth(millis: Long): Boolean {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = millis }
    return now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        now.get(Calendar.MONTH) == target.get(Calendar.MONTH)
}

fun formatCurrency(amount: Double): String {
    return String.format(Locale.GERMANY, "%.2f €", amount)
}

fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}

@Preview(showBackground = true)
@Composable
fun MoneyTrackPreview() {
    MoneyTrackTheme {
        MoneyTrackApp()
    }
}