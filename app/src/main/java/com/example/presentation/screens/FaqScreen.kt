package com.example.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class FaqItem(
    val question: String,
    val answer: String
)

@Composable
fun FaqScreen(
    modifier: Modifier = Modifier
) {
    val faqList = remember {
        listOf(
            FaqItem(
                question = "How does Next Pay send money offline without internet?",
                answer = "Next Pay uses the National Unified USSD Platform (NUUP) built by NPCI (National Payments Corporation of India) accessible via code *99#. Unlike regular UPI apps that need 4G/5G or Wi-Fi, *99# communicates directly over standard cellular GSM voice/signaling bands. Next Pay automatically navigates the USSD menus for you so you don't have to type anything manually."
            ),
            FaqItem(
                question = "Do I need an active internet or data plan?",
                answer = "No! You do not need any mobile data or Wi-Fi connection. As long as your phone has cellular signal bars, you can send money and check your balance anywhere, anytime."
            ),
            FaqItem(
                question = "Is offline UPI safe and bank-approved?",
                answer = "Yes! *99# is an official Government of India and RBI-backed service managed by NPCI. Your UPI PIN is entered directly in encrypted form into the bank USSD session. Next Pay never stores or uploads your UPI PIN."
            ),
            FaqItem(
                question = "What is the maximum amount I can transfer offline?",
                answer = "Per NPCI / RBI guidelines, the offline *99# transaction limit is up to ₹5,000 per transaction, making it ideal for daily payments, groceries, emergencies, and rural transactions."
            ),
            FaqItem(
                question = "Which telecom operators support *99#?",
                answer = "All major Indian GSM operators support *99# NUUP: Airtel, Vodafone Idea (Vi), BSNL, and MTNL. For Jio users, ensure your device supports VoLTE and *99# dialing on your active SIM."
            ),
            FaqItem(
                question = "What is the Accessibility Service used for?",
                answer = "Next Pay uses Android's Accessibility Service strictly to read the incoming *99# dialog text and automatically click the appropriate options (Send Money -> Amount -> Note -> PIN). This turns a tedious 6-step USSD menu into a 1-tap experience. It never reads or accesses any personal data outside of phone USSD dialogs."
            ),
            FaqItem(
                question = "Can I scan standard UPI QR codes with Next Pay?",
                answer = "Yes! Use the built-in Scan QR tab to point your camera at any BharatPe, Google Pay, PhonePe, or Paytm QR code. Next Pay extracts the UPI ID and amount offline and prefills your offline transfer."
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero tip card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Did you know?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "*99# works on any phone across India, even when mobile internet towers are down.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        Text(
            text = "Frequently Asked Questions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )

        faqList.forEach { item ->
            FaqExpandableCard(item = item)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun FaqExpandableCard(item: FaqItem) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.question,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = item.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
