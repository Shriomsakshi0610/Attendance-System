package com.renaissance.teacher.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renaissance.teacher.data.api.ApiClient
import com.renaissance.teacher.data.security.SessionPreferences
import com.renaissance.teacher.ui.theme.AccentRed
import com.renaissance.teacher.ui.theme.PrimaryIndigo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException

@Composable
fun SplashScreen(
    apiClient: ApiClient,
    sessionPrefs: SessionPreferences,
    onNavigateToLogin: (String?) -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    var statusMessage by remember { mutableStateOf("Initializing Renaissance...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCheckingSession by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val validateSessionAndNavigate: () -> Unit = {
        scope.launch {
            isCheckingSession = true
            errorMessage = null
            statusMessage = "Validating secure session..."

            try {
                // Step 1 & 2: Check if local session exists
                if (!sessionPrefs.isLoggedIn()) {
                    delay(400)
                    onNavigateToLogin(null)
                    return@launch
                }

                // Step 3: Ensure session cookies are restored
                apiClient.restoreSession()

                // Step 4: Validate session with authenticated teacher profile endpoint
                val response = apiClient.teacherApi.getProfile()

                if (response.isSuccessful && response.body()?.data != null) {
                    val profile = response.body()!!.data!!
                    // Step 5: Update authenticated teacher's profile in preferences
                    val deptName = profile.departmentName ?: profile.department ?: "Faculty"
                    sessionPrefs.updateTeacherProfile(
                        name = profile.name,
                        teacherId = profile.id,
                        email = profile.email,
                        department = deptName
                    )
                    statusMessage = "Session verified. Loading dashboard..."
                    delay(300)
                    // Step 6 & 7: Proceed to Dashboard
                    onNavigateToDashboard()
                } else {
                    val code = response.code()
                    if (code == 401) {
                        // Session has expired and could not be renewed
                        apiClient.clearSessionCookies()
                        onNavigateToLogin("Your session has expired. Please log in again.")
                    } else if (code == 403) {
                        errorMessage = "Access denied (HTTP 403). Insufficient faculty privileges."
                    } else {
                        errorMessage = "Server returned error (HTTP $code). Please try again."
                    }
                }
            } catch (e: Exception) {
                val isTimeout = e is SocketTimeoutException
                errorMessage = if (isTimeout) {
                    "Connection timed out. Server backend may be warming up."
                } else if (e is IOException) {
                    "Unable to connect to server. Please check your internet connection."
                } else {
                    e.localizedMessage ?: "Failed to validate session."
                }
            } finally {
                isCheckingSession = false
            }
        }
    }

    LaunchedEffect(Unit) {
        validateSessionAndNavigate()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = "Renaissance Logo",
                tint = PrimaryIndigo,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "RENAISSANCE",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 2.sp
            )
            Text(
                text = "Teacher Portal • Smart Attendance",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (isCheckingSession) {
                CircularProgressIndicator(
                    color = PrimaryIndigo,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (errorMessage != null) {
                Card(
                    modifier = Modifier.padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AccentRed,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Connection Notice",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Row {
                            Button(
                                onClick = { validateSessionAndNavigate() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry")
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            OutlinedButton(
                                onClick = {
                                    apiClient.clearSessionCookies()
                                    onNavigateToLogin(null)
                                }
                            ) {
                                Text("Login Again")
                            }
                        }
                    }
                }
            }
        }
    }
}
