package com.kirox.app.ui.v3.data

import com.kirox.app.settings.Tweaks
import com.kirox.app.telegram.KeepMedia
import com.kirox.app.telegram.NotifyScope
import com.kirox.app.telegram.PrivacyLevel
import com.kirox.app.telegram.SharedMediaKind
import com.kirox.app.telegram.StorageCategory
import com.kirox.app.telegram.TdException
import com.kirox.app.telegram.TelegramPanelItem
import com.kirox.app.telegram.TelegramPoll
import com.kirox.app.telegram.TelegramSession
import com.kirox.app.telegram.UsernameCheck
import com.kirox.app.telegram.humanError
import com.kirox.app.ui.v3.model.*

/*
 * Pure pieces of the round-5 store members (settings, tools): TDLib-side values → Kx models, and
 * the kiroX tweak keys. Kept apart from TelegramKxStore so they can be unit-tested.
 */

internal object KxExtrasMapping {
    fun session(s: TelegramSession, now: Long = System.currentTimeMillis()) = KxSession(
        id = s.id,
        device = s.device.ifBlank { s.platform },
        app = s.application,
        place = s.location,
        time = if (s.isCurrent) "в сети" else KxFormat.chatTime(s.lastActive, now),
        current = s.isCurrent,
        passwordPending = s.unconfirmed,
        official = s.isOfficial,
    )

    fun level(l: PrivacyLevel) = when (l) { PrivacyLevel.Everybody -> 0; PrivacyLevel.Contacts -> 1; PrivacyLevel.Nobody -> 2 }
    fun level(i: Int) = when (i) { 0 -> PrivacyLevel.Everybody; 1 -> PrivacyLevel.Contacts; else -> PrivacyLevel.Nobody }

    fun scopeKey(s: NotifyScope) = when (s) { NotifyScope.Private -> "private"; NotifyScope.Groups -> "groups"; NotifyScope.Channels -> "channels" }
    fun scope(key: String) = NotifyScope.entries.firstOrNull { scopeKey(it) == key }

    fun shared(tab: KxSharedTab): SharedMediaKind? = when (tab) {
        KxSharedTab.Media -> SharedMediaKind.Media
        KxSharedTab.Files -> SharedMediaKind.Files
        KxSharedTab.Music -> SharedMediaKind.Music
        KxSharedTab.Links -> SharedMediaKind.Links
        KxSharedTab.Voice -> SharedMediaKind.Voice
        KxSharedTab.Gif -> null // TelegramChatRepository.sharedMedia has no GIF filter yet
    }

    fun username(c: UsernameCheck) = when (c) {
        UsernameCheck.Free -> KxUsernameState.Free
        UsernameCheck.Occupied -> KxUsernameState.Taken
        UsernameCheck.Purchasable -> KxUsernameState.Purchasable
        UsernameCheck.TooMany -> KxUsernameState.TooMany
        UsernameCheck.Invalid -> KxUsernameState.Invalid
    }

    fun storageCategory(title: String) = StorageCategory.entries.firstOrNull { it.title == title }

    fun keepMediaIndex(k: KeepMedia) = KeepMedia.entries.indexOf(k)

    fun poll(p: TelegramPoll, question: String = p.question) = KxPoll(
        question = question,
        options = p.options.map { KxPollOption(it.text, it.percent, it.voters, it.chosen, it.correct) },
        total = p.totalVoters, closed = p.isClosed, multiple = p.multipleAnswers, quiz = p.isQuiz,
        anonymous = p.isAnonymous, canVote = p.canVote, voted = p.voted,
    )

    /** Sticker / GIF ids for KxSticker.id: "f" + TDLib file id (sent again without uploading). */
    fun stickerId(item: TelegramPanelItem) = "f${item.fileId}"

    /** kiroX's own switches, by the names the v3 screens use; the repositories read the same KiroxTweaks. */
    val tweakKeys: Map<String, Pair<(Tweaks) -> Boolean, (Tweaks, Boolean) -> Tweaks>> = mapOf(
        "ghost" to ({ t: Tweaks -> t.ghost } to { t: Tweaks, v: Boolean -> t.copy(ghost = v) }),
        "ghostNoRead" to ({ t: Tweaks -> t.ghostNoRead } to { t: Tweaks, v: Boolean -> t.copy(ghostNoRead = v) }),
        "ghostNoTyping" to ({ t: Tweaks -> t.ghostNoTyping } to { t: Tweaks, v: Boolean -> t.copy(ghostNoTyping = v) }),
        "ghostOffline" to ({ t: Tweaks -> t.ghostOffline } to { t: Tweaks, v: Boolean -> t.copy(ghostOffline = v) }),
        "ghostStories" to ({ t: Tweaks -> t.ghostStories } to { t: Tweaks, v: Boolean -> t.copy(ghostStories = v) }),
        "keepDeleted" to ({ t: Tweaks -> t.keepDeleted } to { t: Tweaks, v: Boolean -> t.copy(keepDeleted = v) }),
        "keepEdits" to ({ t: Tweaks -> t.keepEdits } to { t: Tweaks, v: Boolean -> t.copy(keepEdits = v) }),
        "fileGuard" to ({ t: Tweaks -> t.fileGuard } to { t: Tweaks, v: Boolean -> t.copy(fileGuard = v) }),
        "inAppBrowser" to ({ t: Tweaks -> t.inAppBrowser } to { t: Tweaks, v: Boolean -> t.copy(inAppBrowser = v) }),
        "showIds" to ({ t: Tweaks -> t.showIds } to { t: Tweaks, v: Boolean -> t.copy(showIds = v) }),
        "forwardWithoutAuthor" to ({ t: Tweaks -> t.forwardWithoutAuthor } to { t: Tweaks, v: Boolean -> t.copy(forwardWithoutAuthor = v) }),
        "confirmSticker" to ({ t: Tweaks -> t.confirmSticker } to { t: Tweaks, v: Boolean -> t.copy(confirmSticker = v) }),
        "confirmGif" to ({ t: Tweaks -> t.confirmGif } to { t: Tweaks, v: Boolean -> t.copy(confirmGif = v) }),
        "confirmVoice" to ({ t: Tweaks -> t.confirmVoice } to { t: Tweaks, v: Boolean -> t.copy(confirmVoice = v) }),
        "confirmCall" to ({ t: Tweaks -> t.confirmCall } to { t: Tweaks, v: Boolean -> t.copy(confirmCall = v) }),
        "hideCallsTab" to ({ t: Tweaks -> t.hideCallsTab } to { t: Tweaks, v: Boolean -> t.copy(hideCallsTab = v) }),
        "hideStoriesTab" to ({ t: Tweaks -> t.hideStoriesTab } to { t: Tweaks, v: Boolean -> t.copy(hideStoriesTab = v) }),
        "hideKeyboardOnScroll" to ({ t: Tweaks -> t.hideKeyboardOnScroll } to { t: Tweaks, v: Boolean -> t.copy(hideKeyboardOnScroll = v) }),
        "browserSaveTraffic" to ({ t: Tweaks -> t.browserSaveTraffic } to { t: Tweaks, v: Boolean -> t.copy(browserSaveTraffic = v) }),
        "browserPrivate" to ({ t: Tweaks -> t.browserPrivate } to { t: Tweaks, v: Boolean -> t.copy(browserPrivate = v) }),
    )

    fun tweaks(t: Tweaks): Map<String, Boolean> = tweakKeys.mapValues { (_, accessor) -> accessor.first(t) }

    /** A human Russian error for a failed TDLib call; null when it is a cancellation (rethrown by the caller). */
    fun errorText(t: Throwable): String = (t as? TdException)?.let { humanError(it.error) } ?: "Не получилось"
}

/** Runs [block]; null on success, the human error text on failure. Cancellation is rethrown. */
internal suspend fun attempt(block: suspend () -> Unit): String? = try {
    block(); null
} catch (e: kotlinx.coroutines.CancellationException) {
    throw e
} catch (e: Exception) {
    KxExtrasMapping.errorText(e)
}

/** Value of [block], or null on failure (cancellation rethrown). */
internal suspend fun <T> orNull(block: suspend () -> T): T? = try {
    block()
} catch (e: kotlinx.coroutines.CancellationException) {
    throw e
} catch (e: Exception) {
    null
}
