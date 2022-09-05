package com.dreamers.listplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.dreamers.listplayer.ui.theme.ListPlayerTheme
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheEvictor
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache

class MainActivity : ComponentActivity() {

    private lateinit var cache: Cache
    private lateinit var cacheEvictor: CacheEvictor
    private lateinit var databaseProvider: DatabaseProvider
    private val cacheSize: Long = 100 * 1024 * 1024

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cacheEvictor = LeastRecentlyUsedCacheEvictor(cacheSize)
        databaseProvider = StandaloneDatabaseProvider(this)
        cache = SimpleCache(cacheDir, cacheEvictor, databaseProvider)

        setContent {
            ListPlayerTheme {
                Player(
                    cache = cache,
                )
            }
        }
    }
}