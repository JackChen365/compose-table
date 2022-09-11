package com.github.jackchen.compose.table

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.github.jackchen.compose.table.ui.builder.initialAndDisplayDemoList
import com.github.jackchen.compose.table.ui.example.DatabaseDemo
import com.github.jackchen.compose.table.ui.example.SimpleTableDemo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialAndDisplayDemoList {
            demo("SimpleTable") {
                SimpleTableDemo()
            }
            demo("Database") {
                DatabaseDemo()
            }
        }
    }
}