package com.kirox.app.telegram

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.tdlib.TdApi

/**
 * Account-wide updates TDLib sends once (right after sign-in), kept so screens opened later still
 * see them: accent colours for names and profiles, and new terms of service waiting for acceptance.
 * Registered with the client when the services are built, before TDLib starts.
 */
class TelegramAccountUpdates(client: TelegramClient) {
    private val _accentColors = MutableStateFlow<List<TdApi.AccentColor>>(emptyList())
    /** Colours the user may pick for the name, in Telegram's order (availableAccentColorIds). */
    val accentColors: StateFlow<List<TdApi.AccentColor>> = _accentColors.asStateFlow()
    private val _profileColors = MutableStateFlow<List<TdApi.ProfileAccentColor>>(emptyList())
    val profileColors: StateFlow<List<TdApi.ProfileAccentColor>> = _profileColors.asStateFlow()
    private val _terms = MutableStateFlow<TdApi.UpdateTermsOfService?>(null)
    val terms: StateFlow<TdApi.UpdateTermsOfService?> = _terms.asStateFlow()

    init {
        client.addUpdateHandler { update ->
            when (update) {
                is TdApi.UpdateAccentColors -> {
                    val byId = update.colors.associateBy { it.id }
                    val picked: List<TdApi.AccentColor> = update.availableAccentColorIds.toList().mapNotNull { id -> byId[id] }
                    _accentColors.value = if (picked.isEmpty()) update.colors.toList() else picked
                }
                is TdApi.UpdateProfileAccentColors -> {
                    val byId = update.colors.associateBy { it.id }
                    val picked: List<TdApi.ProfileAccentColor> = update.availableAccentColorIds.toList().mapNotNull { id -> byId[id] }
                    _profileColors.value = if (picked.isEmpty()) update.colors.toList() else picked
                }
                is TdApi.UpdateTermsOfService -> _terms.value = update
                is TdApi.UpdateAuthorizationState -> if (update.authorizationState is TdApi.AuthorizationStateClosed) _terms.value = null
                else -> Unit
            }
        }
    }

    /** The terms were accepted (or the account changed). */
    fun termsDone() { _terms.value = null }
}
