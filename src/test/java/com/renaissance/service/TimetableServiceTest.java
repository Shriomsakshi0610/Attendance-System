package com.renaissance.service;

import com.renaissance.dto.CreateTimetableScheduleRequest;
import com.renaissance.dto.TimetableScheduleDto;
import com.renaissance.entity.*;
import com.renaissance.repository.ClassRepository;
import com.renaissance.repository.SubjectRepository;
import com.renaissance.repository.TeacherRepository;
import com.renaissance.repository.TimetableScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimetableServiceTest {

    @Mock
    private TimetableScheduleRepository timetableRepository;
    @Mock
    private ClassRepository classRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private TeacherRepository teacherRepository;

    private TimetableService timetableService;

    private Department dept;
    private CollegeClass collegeClass;
    private CollegeClass otherClass;
    private Subject subject;
    private Teacher teacher;
    private Teacher otherTeacher;

    @BeforeEach
    void setUp() {
        timetableService = new TimetableService(
                timetableRepository,
                classRepository,
                subjectRepository,
                teacherRepository
        );

        dept = new Department("CSE", "Computer Science");
        dept.setId("dept-1");

        collegeClass = new CollegeClass(dept, 5, "A", "2024-2028");
        collegeClass.setId("class-1");

        otherClass = new CollegeClass(dept, 5, "B", "2024-2028");
        otherClass.setId("class-2");

        subject = new Subject("21CS51", "DBMS", dept, 5);
        subject.setId("sub-1");

        teacher = new Teacher("Dr. Turing", "turing@renaissance.edu", "pass", "EMP001", dept);
        teacher.setId("teacher-1");

        otherTeacher = new Teacher("Dr. Lovelace", "lovelace@renaissance.edu", "pass", "EMP002", dept);
        otherTeacher.setId("teacher-2");
    }

    @Test
    @DisplayName("Successfully create schedule when no conflicts exist")
    void testCreateScheduleSuccess() {
        CreateTimetableScheduleRequest req = new CreateTimetableScheduleRequest();
        req.setClassId("class-1");
        req.setSubjectId("sub-1");
        req.setTeacherId("teacher-1");
        req.setDayOfWeek("MONDAY");
        req.setStartTime("09:00");
        req.setEndTime("10:00");
        req.setRoomNumber("LH-101");

        when(classRepository.findById("class-1")).thenReturn(Optional.of(collegeClass));
        when(subjectRepository.findById("sub-1")).thenReturn(Optional.of(subject));
        when(teacherRepository.findById("teacher-1")).thenReturn(Optional.of(teacher));
        when(timetableRepository.findByDayOfWeek("MONDAY")).thenReturn(List.of());
        when(timetableRepository.save(any(TimetableSchedule.class))).thenAnswer(invocation -> {
            TimetableSchedule s = invocation.getArgument(0);
            s.setId("tt-1");
            return s;
        });

        TimetableScheduleDto result = timetableService.createSchedule(req);

        assertNotNull(result);
        assertEquals("class-1", result.getClassId());
        assertEquals("teacher-1", result.getTeacherId());
        assertEquals("LH-101", result.getRoomNumber());
        assertEquals(LocalTime.of(9, 0), result.getStartTime());
        assertEquals(LocalTime.of(10, 0), result.getEndTime());
    }

    @Test
    @DisplayName("Fail when startTime is after or equal to endTime")
    void testInvalidTimeInterval() {
        CreateTimetableScheduleRequest req = new CreateTimetableScheduleRequest();
        req.setClassId("class-1");
        req.setSubjectId("sub-1");
        req.setTeacherId("teacher-1");
        req.setDayOfWeek("MONDAY");
        req.setStartTime("10:00");
        req.setEndTime("09:00");

        when(classRepository.findById("class-1")).thenReturn(Optional.of(collegeClass));
        when(subjectRepository.findById("sub-1")).thenReturn(Optional.of(subject));
        when(teacherRepository.findById("teacher-1")).thenReturn(Optional.of(teacher));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            timetableService.createSchedule(req);
        });

        assertTrue(ex.getMessage().contains("must be before end time"));
    }

    @Test
    @DisplayName("Detect teacher collision when teacher is already booked on same day and overlapping time")
    void testTeacherConflict() {
        TimetableSchedule existing = new TimetableSchedule(
                dept, otherClass, subject, teacher, "MONDAY",
                LocalTime.of(9, 0), LocalTime.of(10, 0), "LH-201"
        );
        existing.setId("existing-1");

        CreateTimetableScheduleRequest req = new CreateTimetableScheduleRequest();
        req.setClassId("class-1");
        req.setSubjectId("sub-1");
        req.setTeacherId("teacher-1");
        req.setDayOfWeek("MONDAY");
        req.setStartTime("09:30");
        req.setEndTime("10:30"); // overlaps 09:30 - 10:00
        req.setRoomNumber("LH-101");

        when(classRepository.findById("class-1")).thenReturn(Optional.of(collegeClass));
        when(subjectRepository.findById("sub-1")).thenReturn(Optional.of(subject));
        when(teacherRepository.findById("teacher-1")).thenReturn(Optional.of(teacher));
        when(timetableRepository.findByDayOfWeek("MONDAY")).thenReturn(List.of(existing));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            timetableService.createSchedule(req);
        });

        assertTrue(ex.getMessage().contains("Teacher Dr. Turing is already scheduled"));
    }

    @Test
    @DisplayName("Detect class collision when class section already has a lecture on same day and overlapping time")
    void testClassConflict() {
        TimetableSchedule existing = new TimetableSchedule(
                dept, collegeClass, subject, otherTeacher, "MONDAY",
                LocalTime.of(9, 0), LocalTime.of(10, 0), "LH-101"
        );
        existing.setId("existing-2");

        CreateTimetableScheduleRequest req = new CreateTimetableScheduleRequest();
        req.setClassId("class-1");
        req.setSubjectId("sub-1");
        req.setTeacherId("teacher-1");
        req.setDayOfWeek("MONDAY");
        req.setStartTime("09:00");
        req.setEndTime("10:00");
        req.setRoomNumber("LH-102");

        when(classRepository.findById("class-1")).thenReturn(Optional.of(collegeClass));
        when(subjectRepository.findById("sub-1")).thenReturn(Optional.of(subject));
        when(teacherRepository.findById("teacher-1")).thenReturn(Optional.of(teacher));
        when(timetableRepository.findByDayOfWeek("MONDAY")).thenReturn(List.of(existing));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            timetableService.createSchedule(req);
        });

        assertTrue(ex.getMessage().contains("already has") && ex.getMessage().contains("scheduled on MONDAY"));
    }

    @Test
    @DisplayName("Detect room collision when another class occupies same room at same time")
    void testRoomConflict() {
        TimetableSchedule existing = new TimetableSchedule(
                dept, otherClass, subject, otherTeacher, "TUESDAY",
                LocalTime.of(11, 0), LocalTime.of(12, 0), "LH-101"
        );
        existing.setId("existing-3");

        CreateTimetableScheduleRequest req = new CreateTimetableScheduleRequest();
        req.setClassId("class-1");
        req.setSubjectId("sub-1");
        req.setTeacherId("teacher-1");
        req.setDayOfWeek("TUESDAY");
        req.setStartTime("11:30");
        req.setEndTime("12:30");
        req.setRoomNumber("LH-101");

        when(classRepository.findById("class-1")).thenReturn(Optional.of(collegeClass));
        when(subjectRepository.findById("sub-1")).thenReturn(Optional.of(subject));
        when(teacherRepository.findById("teacher-1")).thenReturn(Optional.of(teacher));
        when(timetableRepository.findByDayOfWeek("TUESDAY")).thenReturn(List.of(existing));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            timetableService.createSchedule(req);
        });

        assertTrue(ex.getMessage().contains("Room LH-101 is already occupied on TUESDAY"));
    }

    @Test
    @DisplayName("Requirement 6: Faculty-Wise Timetable Isolation - Faculty A only sees their own schedule")
    void testFacultyWiseTimetableIsolation_FacultyA_OnlySeesOwn() {
        // Faculty A schedules: Monday and Wednesday
        TimetableSchedule aMon = new TimetableSchedule(dept, collegeClass, subject, teacher, "MONDAY", 1,
                LocalTime.of(9, 0), LocalTime.of(10, 0), "LH-101");
        aMon.setId("sched-a1");

        TimetableSchedule aWed = new TimetableSchedule(dept, collegeClass, subject, teacher, "WEDNESDAY", 4,
                LocalTime.of(14, 0), LocalTime.of(15, 0), "LH-101");
        aWed.setId("sched-a2");

        when(timetableRepository.findByTeacher_Id("teacher-1")).thenReturn(List.of(aMon, aWed));

        List<TimetableScheduleDto> result = timetableService.getSchedulesByTeacher("teacher-1");

        assertNotNull(result);
        assertEquals(2, result.size(), "Faculty A must see exactly their 2 assigned classes");
        assertEquals("teacher-1", result.get(0).getTeacherId());
        assertEquals("teacher-1", result.get(1).getTeacherId());
        assertEquals("MONDAY", result.get(0).getDayName());
        assertEquals("WEDNESDAY", result.get(1).getDayName());
        assertEquals(1, result.get(0).getPeriodNumber());
        assertEquals(4, result.get(1).getPeriodNumber());
    }

    @Test
    @DisplayName("Requirement 6: Faculty-Wise Timetable Isolation - Faculty B only sees their own schedule")
    void testFacultyWiseTimetableIsolation_FacultyB_OnlySeesOwn() {
        // Faculty B schedules: Tuesday and Thursday
        TimetableSchedule bTue = new TimetableSchedule(dept, otherClass, subject, otherTeacher, "TUESDAY", 2,
                LocalTime.of(10, 0), LocalTime.of(11, 0), "LH-201");
        bTue.setId("sched-b1");

        TimetableSchedule bThu = new TimetableSchedule(dept, otherClass, subject, otherTeacher, "THURSDAY", 3,
                LocalTime.of(11, 15), LocalTime.of(12, 15), "LH-201");
        bThu.setId("sched-b2");

        when(timetableRepository.findByTeacher_Id("teacher-2")).thenReturn(List.of(bTue, bThu));

        List<TimetableScheduleDto> result = timetableService.getSchedulesByTeacher("teacher-2");

        assertNotNull(result);
        assertEquals(2, result.size(), "Faculty B must see exactly their 2 assigned classes");
        assertEquals("teacher-2", result.get(0).getTeacherId());
        assertEquals("teacher-2", result.get(1).getTeacherId());
        assertEquals("TUESDAY", result.get(0).getDayName());
        assertEquals("THURSDAY", result.get(1).getDayName());
        assertEquals(2, result.get(0).getPeriodNumber());
        assertEquals(3, result.get(1).getPeriodNumber());
    }

    @Test
    @DisplayName("Requirement 2: Faculty with no assigned timetable returns empty list")
    void testUnassignedFaculty_returnsEmptyList() {
        when(timetableRepository.findByTeacher_Id("unassigned-teacher")).thenReturn(List.of());

        List<TimetableScheduleDto> result = timetableService.getSchedulesByTeacher("unassigned-teacher");

        assertNotNull(result);
        assertTrue(result.isEmpty(), "Faculty with no timetable must return empty list");
    }
}
