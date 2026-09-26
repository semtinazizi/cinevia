package com.huyarev.cinevia.navigation

import kotlinx.serialization.Serializable

// --- SEKMELER (BOTTOM NAV ROUTES) ---
@Serializable
object HomeRoute

@Serializable
object SwipeRoute

@Serializable
object ExploreRoute

@Serializable
object SocialRoute

@Serializable
object DiaryRoute

@Serializable
object ProfileRoute

// --- DİĞER EKRAN KANALLARI (DESTINATIONS) ---
@Serializable
object SearchRoute

@Serializable
object InboxRoute

@Serializable
object NotificationsRoute

@Serializable
object SupportRoute

@Serializable
data class FollowListRoute(
    val title: String,
    val ownerEmail: String
)

@Serializable
data class OtherProfileRoute(
    val email: String
)

@Serializable
data class GroupDetailRoute(
    val groupId: String,
    val groupName: String
)

@Serializable
data class ChatRoute(
    val email: String,
    val name: String
)

@Serializable
data class MovieDetailRoute(
    val movieId: Int,
    val isTvShow: Boolean = false
)

@Serializable
data class PersonDetailRoute(
    val personId: Int
)
