package ru.byteforge.xrayvpnclient

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.with
import ru.byteforge.xrayvpnclient.ui.theme.DarkBlue
import ru.byteforge.xrayvpnclient.ui.theme.GoydaBlue
import ru.byteforge.xrayvpnclient.ui.theme.GoydaYellow
import ru.byteforge.xrayvpnclient.ui.theme.MidnightBlue

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.byteforge.xrayvpnclient.ui.components.AnimatedTimeDisplay
import ru.byteforge.xrayvpnclient.ui.components.NetworkConnectionsBackground
import java.io.File
import java.io.FileOutputStream

@Composable
fun MainScreen() {
    val context = LocalContext.current.applicationContext as Application


    val viewModel: ConnectionViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            context
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkBlue,
                        MidnightBlue
                    )
                )
            )
            .padding(vertical = 16.dp)
    ) {

        NetworkConnectionsBackground()

        // Main content
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
//            Header()


            // Connection status and timer
            ConnectionStatus(viewModel)

            // Network stats
            NetworkStats(viewModel)

            Spacer(modifier = Modifier.weight(1f))


            // Disconnect button
//            DisconnectButton(
//                modifier = Modifier
//                    .align(Alignment.CenterHorizontally)
//                    .padding(vertical = 24.dp)
//            )
            ConnectionButton(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 24.dp),
                viewModel = viewModel
            )

            // Server information
            ServerInfo(viewModel)

//            Spacer(modifier = Modifier.weight(1f))

            // Bottom navigation
            BottomNavigation()
        }
    }
}

@Composable
fun Header() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.goyda_logo),
            contentDescription = "ГОЙДА",
            modifier = Modifier.height(128.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(
//                    painter = painterResource(id = R.drawable.ic_diamond),
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = GoydaYellow,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Pro",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ConnectionStatus(viewModel: ConnectionViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        AnimatedContent(
            targetState = when {
                viewModel.isConnecting -> "Connecting"
                viewModel.isConnected -> "Connected"
                else -> "Not Connected"
            },
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) with
                        fadeOut(animationSpec = tween(300))
            }
        ) { state ->
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val iconTint = when {
                    state.contains("Connect") -> GoydaYellow
                    else -> Color.White.copy(alpha = 0.7f)
                }

                val infiniteTransition = rememberInfiniteTransition()
                val pulseScale = if (viewModel.isConnecting) {
                    infiniteTransition.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600),
                            repeatMode = RepeatMode.Reverse
                        )
                    ).value
                } else {
                    1f
                }

                Icon(
                    painter = painterResource(
                        id = if (state.contains("Connect")) R.drawable.ic_power_off else R.drawable.ic_shield_off
                    ),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .size(18.dp)
                        .scale(pulseScale)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = state,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = iconTint
                )
            }
        }

        if (viewModel.isConnected) {
            val timeString = viewModel.formatTime(viewModel.connectionTime)
            val parts = timeString.split(":")

            AnimatedTimeDisplay(
                hours = parts[0],
                minutes = parts[1],
                seconds = parts[2],
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            AnimatedTimeDisplay(
                hours = "00",
                minutes = "00",
                seconds = "00",
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun NetworkStats(viewModel: ConnectionViewModel) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        AnimatedVisibility(
            visible = viewModel.isConnected,
            enter = fadeIn(animationSpec = tween(500)) + expandHorizontally(),
            exit = fadeOut(animationSpec = tween(300)) + shrinkHorizontally()
        ) {
            Row {
                // Download stats
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "Download",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = GoydaBlue,
                            modifier = Modifier.size(16.dp)
                        )

                        val animatedDownload by animateFloatAsState(
                            targetValue = viewModel.downloadSpeed.toFloat()
                        )

                        Text(
                            text = String.format("%.1f Mbps", animatedDownload),
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "Upload",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = GoydaBlue,
                            modifier = Modifier.size(16.dp)
                        )

                        val animatedUpload by animateFloatAsState(
                            targetValue = viewModel.uploadSpeed.toFloat()
                        )

                        Text(
                            text = String.format("%.1f Mbps", animatedUpload),
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionButton(modifier: Modifier = Modifier, viewModel: ConnectionViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        var buttonPressed by rememberSaveable { mutableStateOf(false) }
        val buttonScale by animateFloatAsState(
            targetValue = if (buttonPressed) 0.9f else 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )

        val buttonColor by animateColorAsState(
            targetValue = when {
                viewModel.isConnected -> GoydaYellow.copy(alpha = 0.9f)
                viewModel.isConnecting -> Color.Gray
                else -> GoydaBlue.copy(alpha = 0.9f)
            }
        )

        val infiniteTransition = rememberInfiniteTransition()
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
            )
        )

        Button(
            onClick = {
                buttonPressed = true
                viewModel.toggleConnection()
            },
            modifier = Modifier
                .size(80.dp)
                .scale(buttonScale)
                .graphicsLayer {
                    if (viewModel.isConnecting) {
                        rotationZ = rotation
                    }
                },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor
            ),
            enabled = !viewModel.isConnecting
        ) {
            Icon(
//                painter = painterResource(
//                    id = if (viewModel.isConnected) R.drawable.ic_power else R.drawable.ic_power_off
//                ),
                imageVector = if (viewModel.isConnected) Icons.Filled.Shield else Icons.Outlined.Shield,
                contentDescription = if (viewModel.isConnected) "Disconnect" else "Connect",
                tint = if (viewModel.isConnected) Color(0xFF0A1428) else Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedContent(
            targetState = when {
                viewModel.isConnecting -> "Connecting..."
                viewModel.isConnected -> "Disconnect"
                else -> "Connect"
            }
        ) { text ->
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp
            )
        }

        LaunchedEffect(buttonPressed) {
            if (buttonPressed) {
                delay(100)
                buttonPressed = false
            }
        }
    }
}

@Composable
fun ServerInfo(viewModel: ConnectionViewModel) {
    val server = viewModel.selectedServer

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val flagId = remember(server.flag) {
                    val res = server.flag
                    val resId = try {
                        val field = R.drawable::class.java.getDeclaredField(res)
                        field.getInt(null)
                    } catch (e: Exception) {
                        R.drawable.flag_fl
                    }
                    resId
                }

                Image(
                    painter = painterResource(id = flagId),
//                    imageVector = Icons.Default.Place,
                    contentDescription = "${server.country} flag",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = server.country,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        text = server.city,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) { i ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 1.dp)
                                    .size(width = 3.dp, height = 6.dp + (i * 2).dp)
                                    .background(
                                        if (i < 3) GoydaYellow else Color.Gray,
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "${server.pingMs} ms",
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Details",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun BottomNavigation() {
    NavigationBar(
        containerColor = Color(0xFF1A2235),
        contentColor = Color.White
    ) {
        val navigationItems = listOf(
            NavItem("Home", Icons.Default.Home, true),
            NavItem("Server", Icons.Default.Language, false),
            NavItem("Speed", Icons.Default.Speed, false),
            NavItem("Settings", Icons.Default.Settings, false)
        )

        navigationItems.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) },
                selected = item.isSelected,
                onClick = { /* Handle navigation */ },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GoydaYellow,
                    unselectedIconColor = Color.White.copy(alpha = 0.6f),
                    selectedTextColor = GoydaYellow,
                    unselectedTextColor = Color.White.copy(alpha = 0.6f),
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

data class NavItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isSelected: Boolean
)