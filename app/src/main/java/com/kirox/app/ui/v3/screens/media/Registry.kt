package com.kirox.app.ui.v3.screens.media

import com.kirox.app.ui.v3.KxScreenFn

/** Screens of this section by board name (mockup file name without .dc.html). */
val MediaScreens: Map<String, KxScreenFn> = mapOf(
    "Music" to { ctx -> MusicScreen(ctx) },
    // music: the full player (opened from the mini player of the Music tab / board).
    "MusicPlayer" to { ctx -> MusicPlayerScreen(ctx) },
    "Files" to { ctx -> FilesScreen(ctx) },
    "Editor" to { ctx -> EditorScreen(ctx) },
    "StoryPost" to { ctx -> StoryPostScreen(ctx) },
    "VideoCall" to { ctx -> VideoCallScreen(ctx) },
    "GroupCall" to { ctx -> GroupCallScreen(ctx) },
    "Browser" to { ctx -> BrowserScreen(ctx) },
    "Viewer" to { ctx -> ViewerScreen(ctx) },
    "Incoming" to { ctx -> IncomingScreen(ctx) },
    "CallEnd" to { ctx -> CallEndScreen(ctx) },
    "Voice" to { ctx -> VoiceScreen(ctx) },
    "Downloads" to { ctx -> DownloadsScreen(ctx) },
    "VideoTrim" to { ctx -> VideoTrimScreen(ctx) },
    "StoryViews" to { ctx -> StoryViewsScreen(ctx) },
    "StoryArchive" to { ctx -> StoryArchiveScreen(ctx) },
    "VideoPlayer" to { ctx -> VideoPlayerScreen(ctx) },
    "Album" to { ctx -> AlbumScreen(ctx) },
    "Gallery" to { ctx -> GalleryScreen(ctx) },
    "VideoConf" to { ctx -> VideoConfScreen(ctx) },
    "Pip" to { ctx -> PipScreen(ctx) },
)
