package com.github.jackchen.compose.table.ui.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.jackchen.compose.table.library.SimpleTable

@Composable
fun SimpleTableDemo() {
    SimpleTable(
        modifier = Modifier
    ) {
        val list = (0 until 200).map { "$it" }.toList()
        val header = (0 until 10).map { "Header:$it" }.toList()
        header(header) { item, column ->
            Text(
                text = item,
                modifier = Modifier
                    .drawWithContent {
                        drawContent()
                        if (0 != column) {
                            drawLine(
                                color = Color.White,
                                strokeWidth = 2f,
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height)
                            )
                        }
                    }
                    .background(color = Color.Gray)
                    .padding(16.dp),
                color = Color.Black,
                fontSize = 24.sp
            )
        }
        items(list) { _, row, column ->
            Box(
                modifier = Modifier
                    .border(width = 1.dp, color = Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Item[$row:$column]",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 16.sp
                )
                if (2 == row && 1 == column) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        modifier = Modifier.align(Alignment.BottomEnd),
                        contentDescription = null
                    )
                }
            }
        }
    }
}