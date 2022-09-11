package com.github.jackchen.compose.table.ui.data

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The Room database.
 */
@Database(entities = [Cheese::class], version = 1)
abstract class SampleDatabase : RoomDatabase() {
    companion object {
        /** The only instance  */
        private var sInstance: SampleDatabase? = null

        /**
         * Gets the singleton instance of SampleDatabase.
         *
         * @param context The context.
         * @return The singleton instance of SampleDatabase.
         */
        @Synchronized
        fun getInstance(context: Context): SampleDatabase? {
            if (sInstance == null) {
                sInstance = Room
                    .databaseBuilder(context.applicationContext, SampleDatabase::class.java, "ex")
                    .build()
                sInstance!!.populateInitialData()
            }
            return sInstance
        }

        /**
         * Switches the internal implementation with an empty in-memory database.
         *
         * @param context The context.
         */
        @VisibleForTesting
        fun switchToInMemory(context: Context) {
            sInstance = Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                SampleDatabase::class.java
            ).build()
        }
    }
    /**
     * @return The DAO for the Cheese table.
     */
    abstract fun cheese(): CheeseDao

    /**
     * Inserts the dummy data into the database if it is currently empty.
     */
    private fun populateInitialData() {
        if (cheese().count() == 0) {
            runInTransaction {
                val cheese = Cheese()
                for (i in Cheese.CHEESES.indices) {
                    cheese.name = Cheese.CHEESES[i]
                    cheese.field1 = "field1:$i"
                    cheese.field2 = "field2:$i"
                    cheese.field3 = "field3:$i"
                    cheese.field4 = "field4:$i"
                    cheese.field5 = "field5:$i"
                    cheese.field6 = "field6:$i"
                    cheese.field7 = "field7:$i"
                    cheese.field8 = "field8:$i"
                    cheese.field9 = "field9:$i"
                    cheese.field10 = "field10:$i"
                    cheese.field11 = "field11:$i"
                    cheese.field12 = "field12:$i"
                    cheese.field13 = "field13:$i"
                    cheese.field14 = "field14:$i"
                    cheese.field15 = "field15:$i"
                    cheese.field16 = "field16:$i"
                    cheese.field17 = "field17:$i"
                    cheese.field18 = "field18:$i"
                    cheese.field19 = "field19:$i"
                    cheese.field20 = "field20:$i"
                    cheese().insert(cheese)
                }
            }
        }
    }
}