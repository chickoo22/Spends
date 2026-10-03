package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategorySpending
import com.example.data.model.DailySpending
import com.example.data.model.MonthlyTrendPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Interactive Donut Chart showing spending breakdown by category.
 * Tapping on slices highlights them and reveals exact category spending in center/detail.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveCategoryDonutChart(
    categoryBreakdowns: List<CategorySpending>,
    totalSpent: Double,
    formattedTotal: String,
    onCategorySelected: (CategorySpending?) -> Unit,
    formatCurrency: (Double) -> String,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<CategorySpending?>(null) }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(categoryBreakdowns) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_donut_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tap slice to inspect",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (categoryBreakdowns.isEmpty() || totalSpent <= 0.0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No expenses recorded this month",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(categoryBreakdowns) {
                                detectTapGestures { tapOffset ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val dx = tapOffset.x - center.x
                                    val dy = tapOffset.y - center.y
                                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                                    val outerRadius = size.width / 2f
                                    val innerRadius = outerRadius * 0.58f

                                    if (distance in innerRadius..outerRadius) {
                                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        if (angle < 0) angle += 360f

                                        // Sweep through categories
                                        var currentStartAngle = 270f
                                        var found: CategorySpending? = null
                                        for (item in categoryBreakdowns) {
                                            val sweep = (item.percentage / 100f) * 360f
                                            val normalizedStart = (currentStartAngle % 360 + 360) % 360
                                            val normalizedEnd = (normalizedStart + sweep)

                                            val isInRange = if (normalizedEnd > 360) {
                                                angle >= normalizedStart || angle <= (normalizedEnd % 360)
                                            } else {
                                                angle >= normalizedStart && angle <= normalizedEnd
                                            }

                                            if (isInRange) {
                                                found = item
                                                break
                                            }
                                            currentStartAngle += sweep
                                        }

                                        selectedCategory = if (selectedCategory == found) null else found
                                        onCategorySelected(selectedCategory)
                                    } else {
                                        selectedCategory = null
                                        onCategorySelected(null)
                                    }
                                }
                            }
                    ) {
                        val strokeWidth = 36.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                        val arcSize = Size(diameter, diameter)

                        var startAngle = 270f
                        categoryBreakdowns.forEach { item ->
                            val sweepAngle = (item.percentage / 100f) * 360f * animationProgress.value
                            val isSelected = selectedCategory?.categoryId == item.categoryId
                            val currentStroke = if (isSelected) strokeWidth * 1.25f else strokeWidth
                            val sliceColor = Color(item.colorHex)

                            drawArc(
                                color = sliceColor,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle - 2f, // subtle gap
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = currentStroke, cap = StrokeCap.Round)
                            )
                            startAngle += sweepAngle
                        }
                    }

                    // Center Content Info
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        if (selectedCategory != null) {
                            Text(
                                text = selectedCategory!!.categoryName,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(selectedCategory!!.colorHex),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatCurrency(selectedCategory!!.totalAmount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format("%.1f%% • %d tx", selectedCategory!!.percentage, selectedCategory!!.count),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formattedTotal,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${categoryBreakdowns.size} Categories",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Legend Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categoryBreakdowns.forEach { cat ->
                        val isSelected = selectedCategory?.categoryId == cat.categoryId
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(cat.colorHex).copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(cat.colorHex)) else null,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedCategory = if (selectedCategory == cat) null else cat
                                    onCategorySelected(selectedCategory)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(cat.colorHex))
                                )
                                Text(
                                    text = cat.categoryName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = String.format("%.0f%%", cat.percentage),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Daily Spending Bar Chart.
 * Displays each day's expenses in a scrollable bar chart with peak indicator and bar tap tooltips.
 */
@Composable
fun InteractiveDailyBarChart(
    dailySpendings: List<DailySpending>,
    dailyAverage: Double,
    formatCurrency: (Double) -> String,
    modifier: Modifier = Modifier
) {
    var selectedDay by remember { mutableStateOf<DailySpending?>(null) }
    val maxSpent = remember(dailySpendings) {
        val max = dailySpendings.maxOfOrNull { it.totalAmount } ?: 0.0
        if (max > 0.0) max else 100.0
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val peakColor = MaterialTheme.colorScheme.tertiary
    val barBaseColor = MaterialTheme.colorScheme.surfaceVariant
    val avgLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_bar_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Spending Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Average: ${formatCurrency(dailyAverage)} / day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (selectedDay != null && selectedDay!!.totalAmount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Day ${selectedDay!!.dayOfMonth}: ${formatCurrency(selectedDay!!.totalAmount)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val scrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                dailySpendings.forEach { dayItem ->
                    val isSelected = selectedDay?.dayOfMonth == dayItem.dayOfMonth
                    val barHeightFraction = if (maxSpent > 0) (dayItem.totalAmount / maxSpent).toFloat().coerceIn(0.04f, 1f) else 0.04f

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(26.dp)
                            .clickable {
                                selectedDay = if (selectedDay == dayItem) null else dayItem
                            }
                    ) {
                        // Bar Container
                        Box(
                            modifier = Modifier
                                .height(140.dp)
                                .width(18.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Background track
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(barBaseColor.copy(alpha = 0.4f))
                            )

                            // Active Fill Bar
                            val barColor = when {
                                dayItem.isHighest -> peakColor
                                isSelected -> MaterialTheme.colorScheme.secondary
                                dayItem.totalAmount > dailyAverage -> primaryColor
                                dayItem.totalAmount > 0 -> primaryColor.copy(alpha = 0.7f)
                                else -> Color.Transparent
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((140 * barHeightFraction).dp)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                    .background(barColor)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Day label
                        Text(
                            text = "${dayItem.dayOfMonth}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected || dayItem.isHighest) FontWeight.Bold else FontWeight.Normal,
                            color = if (dayItem.isHighest) peakColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chart Indicator legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(primaryColor))
                    Text("Daily Spending", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(peakColor))
                    Text("Peak Day", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * Interactive 6-Month Spending Trend Line & Area Chart.
 */
@Composable
fun InteractiveMultiMonthTrendChart(
    trends: List<MonthlyTrendPoint>,
    currentMonthIndex: Int,
    onSelectMonth: (year: Int, month: Int) -> Unit,
    formatCurrency: (Double) -> String,
    modifier: Modifier = Modifier
) {
    var activePoint by remember { mutableStateOf<MonthlyTrendPoint?>(null) }
    val maxSpend = remember(trends) {
        val max = trends.maxOfOrNull { it.totalSpent } ?: 0.0
        if (max > 0) max * 1.2 else 1000.0
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_trend_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly Spending Trajectory",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Last 6 months spending comparison",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (activePoint != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "${activePoint!!.label}: ${formatCurrency(activePoint!!.totalSpent)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (trends.size < 2) {
                Text(
                    text = "Not enough monthly data for trend line",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(trends) {
                                detectTapGestures { tapOffset ->
                                    val stepX = size.width / (trends.size - 1)
                                    val tappedIndex = ((tapOffset.x + (stepX / 2f)) / stepX).toInt()
                                        .coerceIn(0, trends.size - 1)
                                    val point = trends[tappedIndex]
                                    activePoint = point
                                    onSelectMonth(point.year, point.month)
                                }
                            }
                    ) {
                        val width = size.width
                        val height = size.height - 30.dp.toPx()
                        val stepX = width / (trends.size - 1)

                        val points = trends.mapIndexed { index, point ->
                            val x = index * stepX
                            val yFraction = (point.totalSpent / maxSpend).toFloat().coerceIn(0f, 1f)
                            val y = height - (yFraction * (height - 20.dp.toPx()))
                            Offset(x, y)
                        }

                        // Draw smooth filled gradient under curve
                        val fillPath = Path()
                        fillPath.moveTo(points.first().x, points.first().y)
                        for (i in 0 until points.size - 1) {
                            val p1 = points[i]
                            val p2 = points[i + 1]
                            val midPoint = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
                            fillPath.quadraticTo(p1.x, p1.y, midPoint.x, midPoint.y)
                        }
                        fillPath.lineTo(points.last().x, points.last().y)
                        fillPath.lineTo(points.last().x, height)
                        fillPath.lineTo(points.first().x, height)
                        fillPath.close()

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.35f),
                                    primaryColor.copy(alpha = 0.02f)
                                )
                            )
                        )

                        // Draw curve line
                        val strokePath = Path()
                        strokePath.moveTo(points.first().x, points.first().y)
                        for (i in 0 until points.size - 1) {
                            val p1 = points[i]
                            val p2 = points[i + 1]
                            val midPoint = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
                            strokePath.quadraticTo(p1.x, p1.y, midPoint.x, midPoint.y)
                        }
                        strokePath.lineTo(points.last().x, points.last().y)

                        drawPath(
                            path = strokePath,
                            color = primaryColor,
                            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Draw points
                        points.forEachIndexed { i, offset ->
                            val isTapped = activePoint?.month == trends[i].month && activePoint?.year == trends[i].year
                            val isCurrentMonth = trends[i].month == currentMonthIndex

                            val radius = if (isTapped) 8.dp.toPx() else if (isCurrentMonth) 6.dp.toPx() else 4.5.dp.toPx()
                            val color = if (isTapped) tertiaryColor else if (isCurrentMonth) primaryColor else primaryColor.copy(alpha = 0.8f)

                            drawCircle(
                                color = onSurfaceColor,
                                radius = radius + 2.dp.toPx(),
                                center = offset
                            )
                            drawCircle(
                                color = color,
                                radius = radius,
                                center = offset
                            )
                        }
                    }
                }

                // Month labels along X axis
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    trends.forEach { point ->
                        val isSelected = (activePoint?.month == point.month && activePoint?.year == point.year) ||
                                (activePoint == null && point.month == currentMonthIndex)
                        Text(
                            text = point.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable {
                                    activePoint = point
                                    onSelectMonth(point.year, point.month)
                                }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
