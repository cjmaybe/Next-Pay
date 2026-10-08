package com.example.presentation.screens.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ModeRouter
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val highlight: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingFlow(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })

    val pages = listOf(
        OnboardingPage(
            title = "Welcome to Next Pay",
            description = "Send money and check your bank balance completely offline without any internet connection or Wi-Fi.",
            icon = Icons.Default.WifiOff,
            highlight = "100% Offline UPI"
        ),
        OnboardingPage(
            title = "Powered by NPCI *99#",
            description = "Transactions travel directly over secure GSM cellular signals via India's official National Unified USSD Platform.",
            icon = Icons.Default.CellTower,
            highlight = "Government RBI & NPCI Backed"
        ),
        OnboardingPage(
            title = "Automated with Accessibility",
            description = "Next Pay automates dialing and menu navigation so you don't have to type into confusing USSD popups.",
            icon = Icons.Default.Accessibility,
            highlight = "1-Tap Effortless Payments"
        )
    )

    // Phone permissions launcher
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Continue
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (pagerState.currentPage < 2) {
                    TextButton(onClick = onComplete) {
                        Text("Skip")
                    }
                } else {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                val page = pages[pageIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(52.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = page.highlight,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = page.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }

            // Pager indicator
            Row(
                modifier = Modifier.padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                for (i in 0 until 3) {
                    val isSelected = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .width(if (isSelected) 24.dp else 8.dp)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            // Action Buttons
            if (pagerState.currentPage < 2) {
                Button(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Next", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            permissionsLauncher.launch(
                                arrayOf(
                                    Manifest.permission.CALL_PHONE,
                                    Manifest.permission.READ_PHONE_STATE,
                                    Manifest.permission.CAMERA
                                )
                            )
                            onComplete()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("get_started_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Get Started & Grant Permissions", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            ModeRouter.openAccessibilitySettings(context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Accessibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enable Accessibility Automation")
                    }
                }
            }
        }
    }
}
