/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jackchen.compose.table.ui.provider

import android.content.ContentProvider
import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentUris
import android.content.ContentValues
import android.content.OperationApplicationException
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import com.github.jackchen.compose.table.ui.data.Cheese
import com.github.jackchen.compose.table.ui.data.SampleDatabase

/**
 * A [ContentProvider] based on a Room database.
 *
 *
 * Note that you don't need to implement a ContentProvider unless you want to expose the data
 * outside your process or your application already uses a ContentProvider.
 */
class SampleContentProvider : ContentProvider() {
    companion object {
        /** The authority of this content provider.  */
        const val AUTHORITY = "com.github.jackchen.android.provider.example"

        /** The URI for the Cheese table.  */
        val URI_CHEESE = Uri.parse(
            "content://" + AUTHORITY + "/" + Cheese.TABLE_NAME
        )

        /** The match code for some items in the Cheese table.  */
        private const val CODE_CHEESE_DIR = 1

        /** The match code for an item in the Cheese table.  */
        private const val CODE_CHEESE_ITEM = 2

        /** The URI matcher.  */
        private val MATCHER = UriMatcher(UriMatcher.NO_MATCH)

        init {
            MATCHER.addURI(AUTHORITY, Cheese.TABLE_NAME, CODE_CHEESE_DIR)
            MATCHER.addURI(AUTHORITY, Cheese.TABLE_NAME + "/*", CODE_CHEESE_ITEM)
        }
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? {
        val code = MATCHER.match(uri)
        return if (code == CODE_CHEESE_DIR || code == CODE_CHEESE_ITEM) {
            val context = context ?: return null
            val cheese = SampleDatabase.getInstance(context)!!.cheese()
            val cursor: Cursor
            cursor = if (code == CODE_CHEESE_DIR) {
                cheese.selectAll()
            } else {
                cheese.selectById(ContentUris.parseId(uri))
            }
            cursor.setNotificationUri(context.contentResolver, uri)
            cursor
        } else {
            throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun getType(uri: Uri): String? {
        return when (MATCHER.match(uri)) {
            CODE_CHEESE_DIR -> "vnd.android.cursor.dir/" + AUTHORITY + "." + Cheese.TABLE_NAME
            CODE_CHEESE_ITEM -> "vnd.android.cursor.item/" + AUTHORITY + "." + Cheese.TABLE_NAME
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return when (MATCHER.match(uri)) {
            CODE_CHEESE_DIR -> {
                val context = context ?: return null
                val id = SampleDatabase.getInstance(context)!!.cheese()
                    .insert(Cheese.fromContentValues(values))
                context.contentResolver.notifyChange(uri, null)
                ContentUris.withAppendedId(uri, id)
            }
            CODE_CHEESE_ITEM -> throw IllegalArgumentException("Invalid URI, cannot insert with ID: $uri")
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun delete(
        uri: Uri, selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        return when (MATCHER.match(uri)) {
            CODE_CHEESE_DIR -> throw IllegalArgumentException(
                "Invalid URI, cannot update without ID$uri"
            )
            CODE_CHEESE_ITEM -> {
                val context = context ?: return 0
                val count = SampleDatabase.getInstance(context)!!.cheese()
                    .deleteById(ContentUris.parseId(uri))
                context.contentResolver.notifyChange(uri, null)
                count
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        return when (MATCHER.match(uri)) {
            CODE_CHEESE_DIR -> throw IllegalArgumentException(
                "Invalid URI, cannot update without ID$uri"
            )
            CODE_CHEESE_ITEM -> {
                val context = context ?: return 0
                val cheese = Cheese.fromContentValues(values)
                cheese.id = ContentUris.parseId(uri)
                val count = SampleDatabase.getInstance(context)!!.cheese()
                    .update(cheese)
                context.contentResolver.notifyChange(uri, null)
                count
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    @Throws(OperationApplicationException::class)
    override fun applyBatch(
        operations: ArrayList<ContentProviderOperation>
    ): Array<ContentProviderResult> {
        val context = context ?: return emptyArray()
        val database = SampleDatabase.getInstance(context)
        return database!!.runInTransaction<Array<ContentProviderResult>> {
            super@SampleContentProvider.applyBatch(
                operations
            )
        }
    }

    override fun bulkInsert(uri: Uri, valuesArray: Array<ContentValues>): Int {
        return when (MATCHER.match(uri)) {
            CODE_CHEESE_DIR -> {
                val context = context ?: return 0
                val database = SampleDatabase.getInstance(context)
                val cheeses = arrayOfNulls<Cheese>(valuesArray.size)
                var i = 0
                while (i < valuesArray.size) {
                    cheeses[i] = Cheese.fromContentValues(valuesArray[i])
                    i++
                }
                database!!.cheese().insertAll(cheeses).size
            }
            CODE_CHEESE_ITEM -> throw IllegalArgumentException("Invalid URI, cannot insert with ID: $uri")
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }
}