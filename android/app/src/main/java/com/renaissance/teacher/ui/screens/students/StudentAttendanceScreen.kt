package com.renaissance.teacher.ui.screens.students

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renaissance.teacher.data.api.ApiClient
import com.renaissance.teacher.data.model.AttendanceSummaryItem
import com.renaissance.teacher.data.model.ClassItem
import com.renaissance.teacher.data.model.SubjectItem
import com.renaissance.teacher.ui.components.RenaissanceTopBar
import com.renaissance.teacher.ui.theme.*
import com.renaissance.teacher.util.CsvShareHelper
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentAttendanceScreen(
    apiClient: ApiClient,
    onNavigateBack: () -> Unit,
    onSelectStudent: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var assignedClasses by remember { mutableStateOf<List<ClassItem>>(emptyList()) }
    var assignedSubjects by remember { mutableStateOf<List<SubjectItem>>(emptyList()) }
    var summaryList by remember { mutableStateOf<List<AttendanceSummaryItem>>(emptyList()) }

    var selectedClass by remember { mutableStateOf<ClassItem?>(null) }
    var selectedSubject by remember { mutableStateOf<SubjectItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateFilter by remember { mutableStateOf("All Time") }

    var isLoading by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var classDropdownExpanded by remember { mutableStateOf(false) }
    var subjectDropdownExpanded by remember { mutableStateOf(false) }
    var dateDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Date computation helper
    fun getDateRange(): Pair<String?, String?> {
        val today = LocalDate.now()
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        return when (selectedDateFilter) {
            "Today" -> Pair(today.format(fmt), today.format(fmt))
            "Last 7 Days" -> Pair(today.minusDays(7).format(fmt), today.format(fmt))
            "Last 30 Days" -> Pair(today.minusDays(30).format(fmt), today.format(fmt))
            else -> Pair(null, null)
        }
    }

    fun loadData() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                // 1. Fetch metadata if empty
                if (assignedClasses.isEmpty()) {
                    val classesRes = apiClient.teacherApi.getAssignedClasses()
                    if (classesRes.isSuccessful && classesRes.body()?.data != null) {
                        assignedClasses = classesRes.body()!!.data!!
                    }
                }
                if (assignedSubjects.isEmpty()) {
                    val subjRes = apiClient.teacherApi.getAssignedSubjects()
                    if (subjRes.isSuccessful && subjRes.body()?.data != null) {
                        assignedSubjects = subjRes.body()!!.data!!
                    }
                }

                // 2. Fetch attendance summary
                val (startDate, endDate) = getDateRange()
                val summaryRes = apiClient.reportApi.getAttendanceSummary(
                    classId = selectedClass?.id,
                    subjectId = selectedSubject?.id,
                    startDate = startDate,
                    endDate = endDate
                )

                if (summaryRes.isSuccessful && summaryRes.body()?.data != null) {
                    summaryList = summaryRes.body()!!.data!!
                } else {
                    summaryList = emptyList()
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load attendance data"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    fun handleExportSummaryCsv() {
        coroutineScope.launch {
            isExporting = true
            showExportDialog = false
            try {
                val (startDate, endDate) = getDateRange()
                val res = apiClient.reportApi.downloadSummaryCsv(
                    classId = selectedClass?.id,
                    subjectId = selectedSubject?.id,
                    startDate = startDate,
                    endDate = endDate
                )
                if (res.isSuccessful && res.body() != null) {
                    val className = selectedClass?.displayName?.replace(" ", "_") ?: "AllClasses"
                    val filename = "Attendance_Summary_${className}_${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.csv"
                    CsvShareHelper.saveAndShareCsv(context, res.body()!!, filename)
                } else {
                    errorMessage = "Export failed with HTTP ${res.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Export failed: ${e.message}"
            } finally {
                isExporting = false
            }
        }
    }

    fun handleExportDateWiseCsv() {
        coroutineScope.launch {
            isExporting = true
            showExportDialog = false
            try {
                val (startDate, endDate) = getDateRange()
                val res = apiClient.reportApi.downloadDateWiseCsv(
                    classId = selectedClass?.id,
                    subjectId = selectedSubject?.id,
                    startDate = startDate,
                    endDate = endDate
                )
                if (res.isSuccessful && res.body() != null) {
                    val className = selectedClass?.displayName?.replace(" ", "_") ?: "AllClasses"
                    val filename = "Attendance_Datewise_${className}_${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.csv"
                    CsvShareHelper.saveAndShareCsv(context, res.body()!!, filename)
                } else {
                    errorMessage = "Export failed with HTTP ${res.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Export failed: ${e.message}"
            } finally {
                isExporting = false
            }
        }
    }

    val filteredList = remember(summaryList, searchQuery) {
        if (searchQuery.isBlank()) {
            summaryList
        } else {
            val q = searchQuery.trim().lowercase()
            summaryList.filter {
                it.studentName.lowercase().contains(q) ||
                it.usn.lowercase().contains(q) ||
                it.studentId.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        topBar = {
            RenaissanceTopBar(
                title = "Student Attendance",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                onRefresh = { loadData() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                placeholder = { Text("Search by name, USN, or ID...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = PrimaryIndigo
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Filter Row (Class, Subject, Date)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Class Selector
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { classDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = selectedClass?.let { "Sem ${it.semester} ${it.section}" } ?: "All Classes",
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = classDropdownExpanded,
                        onDismissRequest = { classDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Classes") },
                            onClick = {
                                selectedClass = null
                                classDropdownExpanded = false
                                loadData()
                            }
                        )
                        assignedClasses.forEach { cls ->
                            DropdownMenuItem(
                                text = { Text(cls.displayName ?: "Sem ${cls.semester} ${cls.section}") },
                                onClick = {
                                    selectedClass = cls
                                    classDropdownExpanded = false
                                    loadData()
                                }
                            )
                        }
                    }
                }

                // Subject Selector
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { subjectDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = selectedSubject?.code ?: "All Subjects",
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = subjectDropdownExpanded,
                        onDismissRequest = { subjectDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Subjects") },
                            onClick = {
                                selectedSubject = null
                                subjectDropdownExpanded = false
                                loadData()
                            }
                        )
                        assignedSubjects.forEach { subj ->
                            DropdownMenuItem(
                                text = { Text("${subj.code} - ${subj.name}") },
                                onClick = {
                                    selectedSubject = subj
                                    subjectDropdownExpanded = false
                                    loadData()
                                }
                            )
                        }
                    }
                }

                // Date Range Filter
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { dateDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = selectedDateFilter,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = dateDropdownExpanded,
                        onDismissRequest = { dateDropdownExpanded = false }
                    ) {
                        listOf("All Time", "Today", "Last 7 Days", "Last 30 Days").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedDateFilter = option
                                    dateDropdownExpanded = false
                                    loadData()
                                }
                            )
                        }
                    }
                }
            }

            // Stats & Export CSV Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredList.size} Students",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { showExportDialog = true },
                    enabled = !isExporting && filteredList.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryViolet),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exporting...", fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export CSV",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = errorMessage!!,
                        color = AccentRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Student List Content
            if (isLoading && summaryList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryIndigo)
                }
            } else if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching students found." else "No student attendance records found.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList, key = { it.studentId }) { student ->
                        StudentAttendanceCard(
                            student = student,
                            onClick = { onSelectStudent(student.studentId) }
                        )
                    }
                }
            }
        }
    }

    // Export CSV Selection Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Text("Export Attendance CSV", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Choose the export format for current filter (${selectedClass?.let { "Sem ${it.semester} ${it.section}" } ?: "All Classes"}, $selectedDateFilter):",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { handleExportSummaryCsv() },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Assessment, contentDescription = null, tint = PrimaryIndigo)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Student-wise Summary CSV", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Summary with total, present, absent & percentage", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { handleExportDateWiseCsv() },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = AccentGreen)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Date-wise Records CSV", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Session-by-session presence logs with status", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StudentAttendanceCard(
    student: AttendanceSummaryItem,
    onClick: () -> Unit
) {
    val pct = student.percentage
    val (badgeBg, badgeTextColor) = when {
        pct >= 75.0 -> Pair(AccentGreen.copy(alpha = 0.15f), AccentGreen)
        pct >= 60.0 -> Pair(AccentAmber.copy(alpha = 0.15f), AccentAmber)
        else -> Pair(AccentRed.copy(alpha = 0.15f), AccentRed)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Student Avatar Initials
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PrimaryIndigo.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                val initial = if (student.studentName.isNotBlank()) student.studentName.first().uppercase() else "S"
                Text(
                    text = initial,
                    color = PrimaryIndigo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Student Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.studentName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = student.usn,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryIndigo
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•  Sem ${student.semester} (${student.section ?: "-"})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                // Stats row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Total: ${student.totalClasses}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Attended: ${student.attended}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentGreen
                    )
                    Text(
                        text = "Missed: ${student.absent}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (student.absent > 0) AccentRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Percentage Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeBg)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = String.format("%.1f%%", pct),
                    color = badgeTextColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
