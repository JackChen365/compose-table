package com.github.jackchen.compose.table.ui.example

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.github.jackchen.compose.table.library.SimpleTable
import com.github.jackchen.compose.table.ui.data.Cheese
import com.github.jackchen.compose.table.ui.provider.SampleContentProvider

private const val LOADER_CHEESES = 1

@Composable
fun DatabaseDemo() {
    val context = LocalContext.current
    var cursor by remember { mutableStateOf<Cursor?>(null) }
    val loaderCallback = rememberLoaderCallback(context) {
        cursor = it
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    if (lifecycleOwner is ViewModelStoreOwner) {
        LoaderManager.getInstance(lifecycleOwner).initLoader(LOADER_CHEESES, null, loaderCallback)
    }
    if (null == cursor) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        DatabaseTable(cursor)
    }
}

@Composable
fun DatabaseTable(cursor: Cursor?) {
    if (null == cursor) return
    var selectedRow by remember {
        mutableStateOf(-1)
    }
    SimpleTable<String>{
        header(cursor.columnNames.toList()) { item, _ ->
            Text(
                text = item,
                modifier = Modifier
                    .background(color = Color.White)
                    .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 4.dp),
                color = Color.Black,
                fontSize = 24.sp
            )
        }
        items(cursor.count) { row, column ->
            cursor.moveToPosition(row)
            val item = cursor.getString(column)
            Box(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                selectedRow = row
                            }
                        )
                    }
                    .background(
                        if (selectedRow == row) Color.Cyan else
                            if (0 == row % 2) Color.White else Color.LightGray
                    )
                    .border(width = 0.5.dp, color = Color.Gray)
                    .padding(top = 8.dp, bottom = 8.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.CenterStart),
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun rememberLoaderCallback(context: Context, block: (Cursor?) -> Unit): LoaderManager.LoaderCallbacks<Cursor> {
    val loaderCallback by remember {
        mutableStateOf(
            object : LoaderManager.LoaderCallbacks<Cursor> {
                override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor?> {
                    val fields = mutableListOf<String>()
                    fields.add(Cheese.COLUMN_NAME)
                    repeat(20) { index ->
                        fields.add("field:${index + 1}")
                    }
                    return CursorLoader(
                        context.applicationContext,
                        SampleContentProvider.URI_CHEESE, fields.toTypedArray(),
                        null, null, null
                    )
                }

                override fun onLoadFinished(loader: Loader<Cursor?>, data: Cursor?) {
                    block(data)
                }

                override fun onLoaderReset(loader: Loader<Cursor?>) {
                    block(null)
                }
            }
        )
    }
    return loaderCallback
}