package org.umn.jalankita.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.umn.jalankita.ui.navigation.Routes

data class BottomNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    /** Filled variant, shown only for the active tab. */
    val selectedIcon: ImageVector,
)

val bottomNavDestinations = listOf(
    BottomNavDestination(Routes.HOME, "Beranda", Icons.Outlined.Home, Icons.Rounded.Home),
    BottomNavDestination(Routes.MAP, "Peta", Icons.Outlined.Map, Icons.Rounded.Map),
    BottomNavDestination(
        route = Routes.HISTORY,
        label = "Laporan",
        icon = Icons.AutoMirrored.Outlined.ListAlt,
        selectedIcon = Icons.AutoMirrored.Rounded.ListAlt,
    ),
    BottomNavDestination(
        route = Routes.NOTIFICATIONS,
        label = "Notifikasi",
        icon = Icons.Outlined.Notifications,
        selectedIcon = Icons.Rounded.Notifications,
    ),
    BottomNavDestination(Routes.PROFILE, "Profil", Icons.Outlined.Person, Icons.Rounded.Person),
)

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onSelect: (String) -> Unit,
    unreadNotifications: Int = 0,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = modifier,
    ) {
        bottomNavDestinations.forEach { destination ->
            val selected = currentRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected) onSelect(destination.route) },
                icon = {
                    val icon = if (selected) destination.selectedIcon else destination.icon
                    if (destination.route == Routes.NOTIFICATIONS && unreadNotifications > 0) {
                        BadgedBox(
                            badge = {
                                Badge { Text(unreadNotifications.coerceAtMost(99).toString()) }
                            },
                        ) {
                            Icon(icon, contentDescription = destination.label)
                        }
                    } else {
                        Icon(icon, contentDescription = destination.label)
                    }
                },
                label = { Text(destination.label, style = MaterialTheme.typography.labelSmall) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
