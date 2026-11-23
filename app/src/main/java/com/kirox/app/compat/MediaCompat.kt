package com.kirox.app.compat

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

/**
 * MediaStore calls that are newer than Android 7 (minSdk 24), with the old way below. On Android 10+
 * the new APIs run exactly as before.
 */
object MediaCompat {
    /** Newest-first rows of [collection] matching [selection], at most [limit]. */
    fun queryNewest(resolver: ContentResolver, collection: Uri, projection: Array<String>, selection: String, sortColumn: String, limit: Int): Cursor? {
        if (Build.VERSION.SDK_INT >= 29) {
            val args = android.os.Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(sortColumn))
                putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
                putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            }
            return resolver.query(collection, projection, args, null)
        }
        // Android 7–9: the media provider takes LIMIT inside the sort order (Bundle limits are honoured from 10).
        return resolver.query(collection, projection, selection, null, "$sortColumn DESC LIMIT $limit")
    }

    /** A thumbnail about [px] square of a MediaStore image or video uri; null when there is none. */
    fun thumbnail(resolver: ContentResolver, uri: Uri, px: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= 29) return resolver.loadThumbnail(uri, android.util.Size(px, px), null)
        val id = runCatching { ContentUris.parseId(uri) }.getOrDefault(-1L)
        if (id < 0) return null
        // MINI_KIND is 512×384: enough for the strip and the picker grids.
        @Suppress("DEPRECATION")
        return if (uri.toString().contains("/video/")) MediaStore.Video.Thumbnails.getThumbnail(resolver, id, MediaStore.Video.Thumbnails.MINI_KIND, null)
        else MediaStore.Images.Thumbnails.getThumbnail(resolver, id, MediaStore.Images.Thumbnails.MINI_KIND, null)
    }
}
