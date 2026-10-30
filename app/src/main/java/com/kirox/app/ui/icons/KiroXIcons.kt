package com.kirox.app.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class KiroXIcon {
    Contacts, Chat, Settings, Profile, Search, ReadAll, Compose, Add, Call, Chevron,
    Palette, Folder, Bell, Shield, Storage, Devices, Star, Sliders, Refresh, Info,
    Edit, More, Stories, Media, Bookmark, Check, Pin, CallIncoming, CallOutgoing, CallMissed,
    Camera, At, Phone, Cake, File, Link, Music, Voice, Verified, Photo,
    Back, Video, Attach, Send, Emoji, Copy, Forward, Delete, Location, Reply,
    Chats, Tune, ProfileCircle, Qr, Speaker, CallEnd, FlipCamera, Menu, Lock, Eye, Backspace
}

/** Material Rounded glyph for each app icon; directional ones mirror in right-to-left layouts. */
private val KiroXIcon.vector: ImageVector
    get() = when (this) {
        KiroXIcon.Contacts -> Icons.Rounded.People
        KiroXIcon.Chat -> Icons.Rounded.ChatBubble
        KiroXIcon.Settings -> Icons.Rounded.Settings
        KiroXIcon.Profile -> Icons.Rounded.Person
        KiroXIcon.Search -> Icons.Rounded.Search
        KiroXIcon.ReadAll -> Icons.Rounded.DoneAll
        KiroXIcon.Compose -> Icons.Rounded.Create
        KiroXIcon.Add -> Icons.Rounded.Add
        KiroXIcon.Call -> Icons.Rounded.Call
        KiroXIcon.Chevron -> Icons.AutoMirrored.Rounded.KeyboardArrowRight
        KiroXIcon.Palette -> Icons.Rounded.Palette
        KiroXIcon.Folder -> Icons.Rounded.Folder
        KiroXIcon.Bell -> Icons.Rounded.Notifications
        KiroXIcon.Shield -> Icons.Rounded.Shield
        KiroXIcon.Storage -> Icons.Rounded.Storage
        KiroXIcon.Devices -> Icons.Rounded.Devices
        KiroXIcon.Star -> Icons.Rounded.Star
        KiroXIcon.Sliders, KiroXIcon.Tune -> Icons.Rounded.Tune
        KiroXIcon.Refresh -> Icons.Rounded.Refresh
        KiroXIcon.Info -> Icons.Rounded.Info
        KiroXIcon.Edit -> Icons.Rounded.Edit
        KiroXIcon.More -> Icons.Rounded.MoreVert
        KiroXIcon.Stories -> Icons.Rounded.Adjust
        KiroXIcon.Media -> Icons.Rounded.PhotoLibrary
        KiroXIcon.Bookmark -> Icons.Rounded.Bookmark
        KiroXIcon.Check -> Icons.Rounded.Check
        KiroXIcon.Pin -> Icons.Rounded.PushPin
        KiroXIcon.CallIncoming -> Icons.AutoMirrored.Rounded.CallReceived
        KiroXIcon.CallOutgoing -> Icons.AutoMirrored.Rounded.CallMade
        KiroXIcon.CallMissed -> Icons.AutoMirrored.Rounded.CallMissed
        KiroXIcon.Camera -> Icons.Rounded.PhotoCamera
        KiroXIcon.At -> Icons.Rounded.AlternateEmail
        KiroXIcon.Phone -> Icons.Rounded.PhoneAndroid
        KiroXIcon.Cake -> Icons.Rounded.Cake
        KiroXIcon.File -> Icons.Rounded.Description
        KiroXIcon.Link -> Icons.Rounded.Link
        KiroXIcon.Music -> Icons.Rounded.MusicNote
        KiroXIcon.Voice -> Icons.Rounded.Mic
        KiroXIcon.Verified -> Icons.Rounded.Verified
        KiroXIcon.Photo -> Icons.Rounded.Image
        KiroXIcon.Back -> Icons.AutoMirrored.Rounded.ArrowBack
        KiroXIcon.Video -> Icons.Rounded.Videocam
        KiroXIcon.Attach -> Icons.Rounded.AttachFile
        KiroXIcon.Send -> Icons.AutoMirrored.Rounded.Send
        KiroXIcon.Emoji -> Icons.Rounded.EmojiEmotions
        KiroXIcon.Copy -> Icons.Rounded.ContentCopy
        KiroXIcon.Forward -> Icons.AutoMirrored.Rounded.Forward
        KiroXIcon.Delete -> Icons.Rounded.Delete
        KiroXIcon.Location -> Icons.Rounded.LocationOn
        KiroXIcon.Reply -> Icons.AutoMirrored.Rounded.Reply
        KiroXIcon.Chats -> Icons.Rounded.Forum
        KiroXIcon.ProfileCircle -> Icons.Rounded.AccountCircle
        KiroXIcon.Qr -> Icons.Rounded.QrCode
        KiroXIcon.Speaker -> Icons.AutoMirrored.Rounded.VolumeUp
        KiroXIcon.CallEnd -> Icons.Rounded.CallEnd
        KiroXIcon.FlipCamera -> Icons.Rounded.FlipCameraAndroid
        KiroXIcon.Menu -> Icons.Rounded.Menu
        KiroXIcon.Lock -> Icons.Rounded.Lock
        KiroXIcon.Eye -> Icons.Rounded.Visibility
        KiroXIcon.Backspace -> Icons.AutoMirrored.Rounded.Backspace
    }

@Composable
fun KiroXIcon(
    icon: KiroXIcon,
    description: String?,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Icon(icon.vector, description, modifier, tint = color)
}
