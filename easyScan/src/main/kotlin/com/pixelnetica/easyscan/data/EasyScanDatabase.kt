package com.pixelnetica.easyscan.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Page::class,
        Representation::class,
        Input::class,
        Original::class,
        Pending::class,
        Complete::class,
        Output::class,
        Text::class,
        Recognition::class,
        DataFile::class,
        ShareSession::class,
        ShareItem::class,
    ],
    views = [
        UselessFile::class,
        PageState::class,
        ShareItemState::class,
        PendingShares::class,
            ],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        // "Pending" table and page status was added
        AutoMigration(from = 1, to = 2),
        // "UselessFile" db-view was changed
        AutoMigration(from = 2, to = 3),
        // "detectOrientation" field was added
        AutoMigration(from = 3, to = 4),
            ],
    )
@TypeConverters(EasyScanConverters::class)
abstract class EasyScanDatabase: RoomDatabase() {
    abstract fun easyScanDao(): EasyScanDao
}