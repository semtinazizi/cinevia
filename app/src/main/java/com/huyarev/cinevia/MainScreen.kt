package com.huyarev.cinevia

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.toRoute
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.huyarev.cinevia.navigation.*

sealed class BottomNavItem<T : Any>(val route: T, val icon: ImageVector, val labelRes: Int) {
    object Home : BottomNavItem<HomeRoute>(HomeRoute, Icons.Default.Home, R.string.home)
    object Swipe : BottomNavItem<SwipeRoute>(SwipeRoute, Icons.Default.Swipe, R.string.explore)
    object Explore : BottomNavItem<ExploreRoute>(ExploreRoute, Icons.Default.Explore, R.string.explore)
    object Social : BottomNavItem<SocialRoute>(SocialRoute, Icons.Default.PlayArrow, R.string.social)
    object Diary : BottomNavItem<DiaryRoute>(DiaryRoute, Icons.Default.Book, R.string.diary)
    object Profile : BottomNavItem<ProfileRoute>(ProfileRoute, Icons.Default.Person, R.string.profile)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    feedViewModel: FeedViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    // Kullanıcı giriş yaptığında dokümanının varlığından emin oluyoruz
    LaunchedEffect(Unit) {
        feedViewModel.ensureUserExistsInDatabase()
    }

    // Anlık olarak okunmamış sayısını dinliyoruz
    val unreadNotifCount by notificationViewModel.unreadCount
    val unreadMsgCount by notificationViewModel.unreadMessageCount

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CineRev", color = Color(0xFFE50914), fontWeight = FontWeight.ExtraBold, fontSize = 24.sp) },
                actions = {
                    IconButton(onClick = { navController.navigate(SearchRoute) }) {
                        Icon(Icons.Default.Search, contentDescription = "Ara", tint = Color.White)
                    }

                    IconButton(onClick = { navController.navigate(NotificationsRoute) }) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifCount > 0) {
                                    Badge(containerColor = Color(0xFFE50914)) { Text(unreadNotifCount.toString(), color = Color.White) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Bildirimler", tint = Color.White)
                        }
                    }

                    IconButton(onClick = {
                        notificationViewModel.markAllMessagesAsRead()
                        navController.navigate(InboxRoute)
                    }) {
                        BadgedBox(
                            badge = {
                                if (unreadMsgCount > 0) {
                                    Badge(containerColor = Color(0xFFE50914)) { Text(unreadMsgCount.toString(), color = Color.White) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Email, contentDescription = "Mesajlar", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF050505))
            )
        },
        bottomBar = { CineRevBottomNavigation(navController = navController) },
        containerColor = Color(0xFF050505)
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    navController = navController,
                    viewModel = hiltViewModel()
                )
            }
            composable<SwipeRoute> {
                SwipeScreen()
            }
            composable<ExploreRoute> {
                ExploreScreen(navController = navController)
            }
            composable<SocialRoute> {
                SocialScreen(
                    onGroupClick = { id, name ->
                        navController.navigate(GroupDetailRoute(id, name))
                    },
                    onUserClick = { email -> navController.navigate(OtherProfileRoute(email)) },
                    onInboxClick = { navController.navigate(InboxRoute) }
                )
            }

            composable<InboxRoute> {
                InboxScreen(
                    onBackClick = { navController.popBackStack() },
                    onChatClick = { email, name ->
                        navController.navigate(ChatRoute(email, name))
                    }
                )
            }

            composable<NotificationsRoute> {
                NotificationScreen(
                    onBackClick = { navController.popBackStack() },
                    onNotificationClick = { notif ->
                        when (notif.type) {
                            "follow" -> navController.navigate(OtherProfileRoute(notif.fromEmail))
                            "group_invite" -> navController.navigate(SocialRoute)
                            else -> { }
                        }
                    }
                )
            }

            composable<FollowListRoute> { backStackEntry ->
                val route: FollowListRoute = backStackEntry.toRoute()

                FollowListScreen(
                    title = route.title,
                    ownerEmail = route.ownerEmail,
                    onBackClick = { navController.popBackStack() },
                    onUserClick = { email -> navController.navigate(OtherProfileRoute(email)) }
                )
            }

            composable<OtherProfileRoute> { backStackEntry ->
                val route: OtherProfileRoute = backStackEntry.toRoute()
                OtherProfileScreen(navController = navController, targetEmail = route.email)
            }
            composable<GroupDetailRoute> { backStackEntry ->
                val route: GroupDetailRoute = backStackEntry.toRoute()

                GroupDetailScreen(
                    groupId = route.groupId,
                    groupName = route.groupName,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable<DiaryRoute> {
                DiaryScreen(
                    navController = navController,
                    viewModel = hiltViewModel()
                )
            }

            composable<SearchRoute> {
                SearchScreen(
                    onMovieClick = { movieId -> navController.navigate(MovieDetailRoute(movieId, isTvShow = false)) },
                    onUserClick = { email -> navController.navigate(OtherProfileRoute(email)) },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable<ChatRoute> { backStackEntry ->
                val route: ChatRoute = backStackEntry.toRoute()
                ChatScreen(targetUserEmail = route.email, targetUserName = route.name, onBackClick = { navController.popBackStack() })
            }

            composable<MovieDetailRoute> { backStackEntry ->
                val route: MovieDetailRoute = backStackEntry.toRoute()

                MovieDetailScreen(
                    movieId = route.movieId,
                    isTvShow = route.isTvShow,
                    onBackClick = { navController.popBackStack() },
                    navController = navController,
                    viewModel = hiltViewModel()
                )
            }
            composable<PersonDetailRoute> { backStackEntry ->
                val route: PersonDetailRoute = backStackEntry.toRoute()
                PersonDetailScreen(personId = route.personId, navController = navController)
            }
            composable<SupportRoute> {
                SupportScreen(onBackClick = { navController.popBackStack() })
            }
            composable<ProfileRoute> {
                ProfileScreen(navController = navController)
            }
        }
    }
}

@Composable
fun CineRevBottomNavigation(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Swipe,
        BottomNavItem.Explore,
        BottomNavItem.Social,
        BottomNavItem.Diary,
        BottomNavItem.Profile
    )

    NavigationBar(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(32.dp)),
        containerColor = Color(0xCC151515), // Yarı Saydam Cam Efekti
        contentColor = Color.White
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { item ->
            val isSelected = currentDestination?.hasRoute(item.route::class) == true
            NavigationBarItem(
                icon = { Icon(imageVector = item.icon, contentDescription = stringResource(id = item.labelRes)) },
                label = { Text(text = stringResource(id = item.labelRes)) },
                selected = isSelected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = Color(0xFFE50914),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}
