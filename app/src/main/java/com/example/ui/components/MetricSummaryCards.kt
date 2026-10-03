package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MonthlyAnalytics

@Composable
fun MonthlyHeroSummaryCard(
    analytics: MonthlyAnalytics,
    formatCurrency: (Double) -> String,
    modifier: Modifier = Modifier
) {
    // Executive Obsidian Card Design — guarantees crisp contrast in both light & dark mode
    val cardBackgroundBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0F172A), // Deep Slate 900
            Color(0xFF1E293B)  // Deep Slate 800
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_hero_summary_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBackgroundBrush)
                .padding(22.dp)
        ) {
            Column {
                // Top Row: Statement Header & Month Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF334155)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "TOTAL SPENT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = analytics.monthTitle,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }

                    // % change vs previous month pill
                    if (analytics.percentChangeFromPrev != null) {
                        val pct = analytics.percentChangeFromPrev
                        val isIncrease = pct > 0
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isIncrease) Color(0xFF3B1219) else Color(0xFF063321),
                            border = BorderStroke(
                                1.dp,
                                if (isIncrease) Color(0xFF7F1D1D) else Color(0xFF065F46)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isIncrease) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = if (isIncrease) Color(0xFFFCA5A5) else Color(0xFF6EE7B7),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = String.format("%s%.1f%% vs prev", if (isIncrease) "+" else "", pct),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIncrease) Color(0xFFFCA5A5) else Color(0xFF6EE7B7),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Big Headline Currency Amount (Pure White & Tabular figures)
                Text(
                    text = formatCurrency(analytics.totalSpent),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(18.dp))

                HorizontalDivider(
                    color = Color(0xFF334155).copy(alpha = 0.6f),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3 Financial Indicators Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricIndicator(
                        icon = Icons.Default.CalendarToday,
                        label = "Daily Avg",
                        value = formatCurrency(analytics.dailyAverage)
                    )

                    MetricIndicator(
                        icon = Icons.Default.DateRange,
                        label = "Peak Day",
                        value = if (analytics.highestDayAmount > 0)
                            "Day ${analytics.highestDay} (${formatCurrency(analytics.highestDayAmount)})"
                        else "None"
                    )

                    MetricIndicator(
                        icon = Icons.Default.ReceiptLong,
                        label = "Activity",
                        value = "${analytics.transactionCount} entries"
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricIndicator(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF1F5F9),
            fontSize = 12.sp
        )
    }
}
