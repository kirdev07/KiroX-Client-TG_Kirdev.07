package com.kirox.app.ui.v3.screens.groups

import com.kirox.app.ui.v3.KxScreenFn

/** Screens of this section by board name (mockup file name without .dc.html). */
val GroupScreens: Map<String, KxScreenFn> = mapOf(
    "Group" to { ctx -> GroupScreen(ctx) },
    "Channel" to { ctx -> ChannelScreen(ctx) },
    "NewGroup" to { ctx -> NewGroupScreen(ctx) },
    "Invite" to { ctx -> InviteScreen(ctx) },
    "AdminRights" to { ctx -> AdminRightsScreen(ctx) },
    "Restrict" to { ctx -> RestrictScreen(ctx) },
    "ChannelSettings" to { ctx -> ChannelSettingsScreen(ctx) },
    "ChannelStats" to { ctx -> ChannelStatsScreen(ctx) },
    "Members" to { ctx -> MembersScreen(ctx) },
    "AdminLog" to { ctx -> AdminLogScreen(ctx) },
    "SlowMode" to { ctx -> SlowModeScreen(ctx) },
    "JoinRequest" to { ctx -> JoinRequestScreen(ctx) },
    "PostStats" to { ctx -> PostStatsScreen(ctx) },
    "AllowedReacts" to { ctx -> AllowedReactsScreen(ctx) },
    "GroupStickers" to { ctx -> GroupStickersScreen(ctx) },
    "Boosts" to { ctx -> BoostsScreen(ctx) },
    "JoinQueue" to { ctx -> JoinQueueScreen(ctx) },
    "GroupStats" to { ctx -> GroupStatsScreen(ctx) },
    "PinnedMsgs" to { ctx -> PinnedMsgsScreen(ctx) },
    "Requests" to { ctx -> RequestsScreen(ctx) },
)
