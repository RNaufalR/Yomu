package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "books",
    indices = [
        Index(value = ["fileHash"], unique = true),
        Index(value = ["titleSort"]),
        Index(value = ["seriesId"])
    ]
)
data class BookEntity(
    @PrimaryKey val id: String,
    val fileHash: String,
    val title: String,
    val titleSort: String,
    val subtitle: String? = null,
    val altTitles: String = "[]",
    val seriesId: String? = null,
    val seriesName: String? = null,
    val seriesIndex: Float? = null,
    val language: String = "ja",
    val primaryFormat: String,
    val storageMode: String = "in_place",
    val coverPath: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis(),
    val publicationDate: String? = null,
    val readingStatus: String = "unread",
    val isFavorite: Boolean = false,
    val rating: Int? = null,
    val rawMetadataJson: String = "{}",
    val author: String? = null,
    val illustrator: String? = null,
    val translator: String? = null,
    val publisher: String? = null,
    val filePath: String,
    val fileSize: Long = 0L,
    val description: String? = null
)

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bookId", "indexInBook"])]
)
data class ChapterEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val indexInBook: Int,
    val title: String?,
    val locator: String,
    val charCount: Int = 0,
    val contentPreview: String? = null
)

@Entity(
    tableName = "reading_progress",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ReadingProgressEntity(
    @PrimaryKey val bookId: String,
    val currentChapterIndex: Int = 0,
    val currentPosition: String = "0",
    val progressPercentage: Float = 0f,
    val lastReadAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bookId"])]
)
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val chapterIndex: Int,
    val position: String,
    val label: String?,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "highlights",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bookId"])]
)
data class HighlightEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val chapterIndex: Int,
    val positionStart: String,
    val positionEnd: String,
    val color: String = "#FFF59D",
    val selectedText: String,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val isSmart: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "collection_books",
    primaryKeys = ["collectionId", "bookId"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bookId"])]
)
data class CollectionBookEntity(
    val collectionId: String,
    val bookId: String,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "reading_history",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bookId"])]
)
data class ReadingHistoryEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val sessionStart: Long,
    val sessionEnd: Long,
    val durationSeconds: Long
)

@Entity(
    tableName = "settings",
    primaryKeys = ["key", "scope"]
)
data class SettingEntity(
    val key: String,
    val scope: String = "global",
    val value: String
)

@Entity(
    tableName = "custom_metadata",
    primaryKeys = ["bookId", "namespace", "key"],
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CustomMetadataEntity(
    val bookId: String,
    val namespace: String,
    val key: String,
    val value: String
)
