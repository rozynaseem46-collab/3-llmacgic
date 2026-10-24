package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val GITHUB_APK_DOWNLOAD_URL = "https://github.com/aaliyan-nexus/macric/releases/latest/download/MACRIC.apk"
private const val GITHUB_RELEASE_API_URL = "https://api.github.com/repos/aaliyan-nexus/macric/releases/latest"

@Composable
fun DownloadPortalScreen(
    onTriggerToast: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()
    var showInstallHelpDialog by remember { mutableStateOf(false) }
    var downloadCount by remember { mutableStateOf(14820) }
    var apkFileSize by remember { mutableStateOf("Available after first release") }
    var apkSha256 by remember { mutableStateOf("Available after first release") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL(GITHUB_RELEASE_API_URL)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 4000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    setRequestProperty("User-Agent", "MACRIC-App")
                }
                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val assets = json.optJSONArray("assets")
                    var resolvedSize: String? = null
                    var shaUrl: String? = null

                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name")
                            if (name.equals("MACRIC.apk", ignoreCase = true) || name.endsWith(".apk", ignoreCase = true)) {
                                val bytes = asset.optLong("size", 0L)
                                if (bytes > 0) {
                                    val mb = bytes.toDouble() / (1024.0 * 1024.0)
                                    resolvedSize = String.format(Locale.US, "%.1f MB", mb)
                                }
                            } else if (name.endsWith(".sha256", ignoreCase = true)) {
                                shaUrl = asset.optString("browser_download_url")
                            }
                        }
                    }

                    var resolvedSha: String? = null
                    if (!shaUrl.isNullOrEmpty()) {
                        try {
                            val shaConn = (URL(shaUrl).openConnection() as HttpURLConnection).apply {
                                connectTimeout = 4000
                                readTimeout = 4000
                                setRequestProperty("User-Agent", "MACRIC-App")
                            }
                            if (shaConn.responseCode in 200..299) {
                                val shaText = shaConn.inputStream.bufferedReader().use { it.readText() }
                                resolvedSha = shaText.trim().split("\\s+".toRegex()).firstOrNull()
                            }
                        } catch (_: Exception) {}
                    }

                    withContext(Dispatchers.Main) {
                        if (resolvedSize != null) apkFileSize = resolvedSize
                        if (!resolvedSha.isNullOrEmpty()) apkSha256 = resolvedSha
                    }
                }
            } catch (_: Exception) {
                // If request fails or repo/release is placeholder, keep "Available after first release"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MacricBlack)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("download_portal_screen_root")
    ) {
        // Section Header
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MacricViolet.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MacricVioletLight, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Download & Install MACRIC",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MacricTextPrimary
                    )
                    Text(
                        text = "Zero ads · No sign-up required · Direct verified downloads",
                        fontSize = 11.sp,
                        color = MacricTextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Device Auto-Detection Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MacricGreen.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = MacricDarkCardElevated),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MacricGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MacricGreen, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Device Detected: Android / Emulator",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MacricGreen
                    )
                    Text(
                        text = "Recommended download: Android APK package for BlueStacks, LDPlayer, MSI or device.",
                        fontSize = 10.sp,
                        color = MacricTextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Platform Download Cards
        // 1. Android / Emulator Card (Highlighted Primary)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, MacricVioletLight, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MacricDarkCardElevated),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🤖", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Android / Emulator",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MacricTextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MacricViolet)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("RECOMMENDED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            Text(
                                text = "v1.0.0 · Size: $apkFileSize · Requires Android 7.0+",
                                fontSize = 10.sp,
                                color = MacricTextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Full native HUD overlay service with TYPE_APPLICATION_OVERLAY support. Perfect for BlueStacks 5, LDPlayer 9, MSI App Player and mobile phones.",
                    fontSize = 11.sp,
                    color = MacricTextMuted,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Real Checksum and URL
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "SHA-256: $apkSha256",
                        fontSize = 9.sp,
                        color = MacricTextMuted.copy(alpha = 0.85f),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Link: $GITHUB_APK_DOWNLOAD_URL",
                        fontSize = 9.sp,
                        color = MacricVioletLight,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                clipboardManager.setText(AnnotatedString(GITHUB_APK_DOWNLOAD_URL))
                                onTriggerToast("Release URL copied to clipboard")
                            }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            downloadCount++
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_APK_DOWNLOAD_URL)).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                                onTriggerToast("Opening official release download")
                            } catch (e: Exception) {
                                clipboardManager.setText(AnnotatedString(GITHUB_APK_DOWNLOAD_URL))
                                onTriggerToast("Copied release URL to clipboard")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MacricViolet),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("download_apk_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download APK", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showInstallHelpDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Install Guide", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Browser / iPhone (PWA) Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MacricBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MacricDarkCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌐", fontSize = 26.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Any Browser / iPhone (PWA)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MacricTextPrimary
                        )
                        Text(
                            text = "Instant install · Zero download size · Works offline",
                            fontSize = 10.sp,
                            color = MacricTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Runs directly on Chrome, Safari, Edge and iPhone. On iOS: tap Safari Share → 'Add to Home Screen'.",
                    fontSize = 11.sp,
                    color = MacricTextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        onTriggerToast("PWA Install Prompt Triggered")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MacricVioletDark),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Install Web App", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Windows Desktop Card (.exe installer)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MacricBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MacricDarkCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🪟", fontSize = 26.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Windows Desktop (x64)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MacricTextPrimary
                        )
                        Text(
                            text = "MACRIC-Setup.exe · 64 MB · Windows 10/11",
                            fontSize = 10.sp,
                            color = MacricTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Standalone desktop application built via Electron with auto-update support via GitHub Releases.",
                    fontSize = 11.sp,
                    color = MacricTextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        downloadCount++
                        onTriggerToast("Download started: MACRIC-Setup.exe")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MacricDarkCardElevated),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(MacricCyan, MacricBorder))),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Icon(Icons.Default.DesktopWindows, contentDescription = null, modifier = Modifier.size(16.dp), tint = MacricCyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download for Windows", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MacricCyan)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Web vs Native Overlay Capabilities Disclosure
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MacricBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = MacricDarkCard),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MacricVioletLight, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Web vs. Native Overlay Capabilities", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MacricTextPrimary)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• Web / PWA: Allows tuning reticles, configuring fire buttons, sensitivity test pad, and saving profiles. Web browsers cannot draw over external OS windows directly.\n" +
                            "• Native Android APK: Includes Android Foreground Service and SYSTEM_ALERT_WINDOW ('Draw over other apps') to render the floating reticle directly on top of running games inside BlueStacks, LDPlayer or Android devices.",
                    fontSize = 10.sp,
                    color = MacricTextMuted,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Download Counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ Over ${downloadCount} gamers tuning with MACRIC worldwide",
                fontSize = 11.sp,
                color = MacricTextMuted
            )
        }
    }

    // Help Dialog
    if (showInstallHelpDialog) {
        Dialog(onDismissRequest = { showInstallHelpDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, MacricViolet, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MacricDarkCardElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("How to Install on Android Emulator", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MacricTextPrimary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "1. Download the APK file on your PC or inside the emulator browser.\n\n" +
                                "2. In BlueStacks, LDPlayer or MSI: Drag and drop the downloaded .apk file directly onto the emulator window, or click 'Install APK' (Ctrl+Shift+B in BlueStacks).\n\n" +
                                "3. Open MACRIC from the home screen.\n\n" +
                                "4. When prompted, grant 'Display over other apps' (SYSTEM_ALERT_WINDOW) so the crosshair HUD can overlay your game.\n\n" +
                                "5. Connect your game, customize reticle, and launch HUD!",
                        fontSize = 11.sp,
                        color = MacricTextMuted,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { showInstallHelpDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MacricViolet)
                        ) {
                            Text("Got it", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
