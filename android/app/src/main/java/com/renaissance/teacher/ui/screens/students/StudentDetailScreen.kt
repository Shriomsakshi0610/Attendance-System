package com.renaissance.teacher.ui.screens.students

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renaissance.teacher.data.api.ApiClient
import com.renaissance.teacher.data.model.StudentHistoryRecordItem
import com.renaissance.teacher.data.model.StudentProfileItem
import com.renaissance.teacher.data.model.SubjectAttendanceItem
import com.renaissance.teacher.ui.components.RenaissanceTopBar
import com.renaissance.teacher.ui.theme.*
import com.renaissance.teacher.util.CsvShareHelper
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun StudentDetailScreen(
    studentId: String,
    apiClient: ApiClient,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var profile by remember { mutableStateOf<StudentProfileItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isExporting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Overview & Subjects, 1: Date-wise History

    fun loadProfile() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val res = apiClient.reportApi.getStudentProfile(studentId)
                if (res.isSuccessful && res.body()?.data != null) {
                    profile = res.body()!!.data
                } else {
                    errorMessage = "Failed to load student details (${res.code()})"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "An error occurred"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(studentId) {
        loadProfile()
    }

    fun handleExportCsv() {
        coroutineScope.launch {
            isExporting = true
            try {
                val res = apiClient.reportApi.downloadStudentCsv(studentId)
                if (res.isSuccessful && res.body() != null) {
                    val usn = profile?.usn ?: studentId
                    val filename = "Attendance_Student_${usn}_${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.csv"
                    CsvShareHelper.saveAndShareCsv(context, res.body()!!, filename)
                } else {
                    errorMessage = "Failed to export student CSV (${res.code()})"
                }
            } catch (e: Exception) {
                errorMessage = "Export failed: ${e.message}"
            } finally {
                isExporting = false
            }
        }
    }

    Scaffold(
        topBar = {
            RenaissanceTopBar(
                title = profile?.name ?: "Student Profile",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                onRefresh = { loadProfile() }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            if (isLoading && profile == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryIndigo)
                }
            } else if (profile == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMessage ?: "Student profile not available.",
                            color = AccentRed,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { loadProfile() }) {
                            Text("Retry")
                        }
                    }
                }
            } else {
                val p = profile!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Profile Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryIndigo.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (p.name.isNotBlank()) p.name.first().uppercase() else "S",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                        color = PrimaryIndigo
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = p.name,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "USN: ${p.usn}",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = PrimaryIndigo
                                    )
                                    Text(
                                        text = "${p.department ?: "Dept"} • Sem ${p.semester} (${p.section ?: "-"})",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Overall Percentage Badge
                                val pct = p.overallPercentage
                                val (bgCol, txtCol) = when {
                                    pct >= 75.0 -> Pair(AccentGreen.copy(alpha = 0.15f), AccentGreen)
                                    pct >= 60.0 -> Pair(AccentAmber.copy(alpha = 0.15f), AccentAmber)
                                    else -> Pair(AccentRed.copy(alpha = 0.15f), AccentRed)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(bgCol)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = String.format("%.1f%%", pct),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = txtCol
                                        )
                                        Text(
                                            text = if (pct >= 75.0) "Eligible" else "Shortage",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = txtCol
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Overall Stats Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                StatMini(label = "Total Classes", value = p.totalConducted.toString(), color = MaterialTheme.colorScheme.onSurface)
                                StatMini(label = "Attended", value = p.totalPresent.toString(), color = AccentGreen)
                                StatMini(label = "Missed", value = p.totalAbsent.toString(), color = if (p.totalAbsent > 0) AccentRed else MaterialTheme.colorScheme.onSurface)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Export Button inside Header Card
                            Button(
                                onClick = { handleExportCsv() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isExporting,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryViolet)
                            ) {
                                if (isExporting) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Exporting...", fontSize = 13.sp)
                                } else {
                                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Export Student History CSV", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tab Selector: Subject Breakdown vs Date-wise History
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = PrimaryIndigo
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Subject Breakdown (${p.subjectWiseAttendance.size})", fontWeight = FontWeight.SemiBold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Date History (${p.attendanceHistory.size})", fontWeight = FontWeight.SemiBold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (selectedTab == 0) {
                        // Subject-wise attendance list
                        if (p.subjectWiseAttendance.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No subject attendance recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(p.subjectWiseAttendance) { subj ->
                                    SubjectAttendanceCard(subj)
                                }
                            }
                        }
                    } else {
                        // Date-wise attendance history
                        if (p.attendanceHistory.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No attendance sessions recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(p.attendanceHistory) { rec ->
                                    AttendanceHistoryItemCard(rec)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMini(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = color)
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SubjectAttendanceCard(subject: SubjectAttendanceItem) {
    val pct = subject.percentage
    val (badgeBg, badgeTextColor) = when {
        pct >= 75.0 -> Pair(AccentGreen.copy(alpha = 0.15f), AccentGreen)
        pct >= 60.0 -> Pair(AccentAmber.copy(alpha = 0.15f), AccentAmber)
        else -> Pair(AccentRed.copy(alpha = 0.15f), AccentRed)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.subjectName ?: "Subject",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Code: ${subject.subjectCode ?: "-"}",
                        fontSize = 11.sp,
                        color = PrimaryIndigo,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = String.format("%.1f%%", pct),
                        color = badgeTextColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { (pct / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = badgeTextColor,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Conducted: ${subject.totalConducted}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Present: ${subject.totalPresent}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = AccentGreen
                )
                Text(
                    text = "Absent: ${subject.totalAbsent}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (subject.totalAbsent > 0) AccentRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AttendanceHistoryItemCard(record: StudentHistoryRecordItem) {
    val isPresent = record.status.equals("PRESENT", ignoreCase = true)
    val statusBg = if (isPresent) AccentGreen.copy(alpha = 0.15f) else AccentRed.copy(alpha = 0.15f)
    val statusColor = if (isPresent) AccentGreen else AccentRed

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.subjectName ?: "Session",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.sessionDate ?: "-",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${record.sessionTime ?: "-"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (record.sessionCode != null && record.sessionCode != "-") {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Session: ${record.sessionCode}",
                        fontSize = 11.sp,
                        color = PrimaryIndigo
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusBg)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isPresent) "PRESENT" else "ABSENT",
                    color = statusColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                )
            }
        }
    }
}
