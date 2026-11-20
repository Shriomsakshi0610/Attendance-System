package com.renaissance.service;

import com.renaissance.dto.DeviceDto;
import com.renaissance.dto.DeviceRegistrationRequest;
import com.renaissance.dto.DeviceStatusResponse;
import com.renaissance.entity.Department;
import com.renaissance.entity.Student;
import com.renaissance.entity.StudentDevice;
import com.renaissance.exception.ResourceNotFoundException;
import com.renaissance.repository.StudentDeviceRepository;
import com.renaissance.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceRegistrationTest {

    @Mock
    private StudentDeviceRepository deviceRepository;

    @Mock
    private StudentRepository studentRepository;

    private DeviceRegistrationService deviceService;

    private Student student;
    private StudentDevice device;

    @BeforeEach
    void setUp() {
        deviceService = new DeviceRegistrationService(deviceRepository, studentRepository);

        Department dept = new Department("CSE", "Computer Science", "CSE Dept");
        student = new Student("2GI24IS042", "Aarav Sharma", "aarav@renaissance.edu", dept, null);
        student.setId("stud-42");

        device = new StudentDevice();
        device.setId("dev-1");
        device.setDeviceToken("token-uuid-12345");
        device.setUsn("2GI24IS042");
        device.setStudentName("Aarav Sharma");
        device.setStatus(StudentDevice.DeviceStatus.PENDING);
    }

    @Test
    @DisplayName("Check status returns NOT_REGISTERED if token not found")
    void testCheckStatusNotRegistered() {
        when(deviceRepository.findByDeviceToken("token-unknown")).thenReturn(Optional.empty());

        DeviceStatusResponse resp = deviceService.checkDeviceStatus("2GI24IS042", "token-unknown");
        assertNotNull(resp);
        assertEquals("NOT_REGISTERED", resp.getStatus());
        assertFalse(resp.isApproved());
    }

    @Test
    @DisplayName("Check status returns PENDING when device is awaiting admin approval")
    void testCheckStatusPending() {
        when(deviceRepository.findByDeviceToken("token-uuid-12345")).thenReturn(Optional.of(device));

        DeviceStatusResponse resp = deviceService.checkDeviceStatus("2GI24IS042", "token-uuid-12345");
        assertNotNull(resp);
        assertEquals("PENDING", resp.getStatus());
        assertFalse(resp.isApproved());
    }

    @Test
    @DisplayName("Check status returns APPROVED when admin has approved device")
    void testCheckStatusApproved() {
        device.setStatus(StudentDevice.DeviceStatus.APPROVED);
        when(deviceRepository.findByDeviceToken("token-uuid-12345")).thenReturn(Optional.of(device));

        DeviceStatusResponse resp = deviceService.checkDeviceStatus("2GI24IS042", "token-uuid-12345");
        assertNotNull(resp);
        assertEquals("APPROVED", resp.getStatus());
        assertTrue(resp.isApproved());
    }

    @Test
    @DisplayName("Check status detects student USN mismatch on shared or transferred device")
    void testCheckStatusMismatch() {
        when(deviceRepository.findByDeviceToken("token-uuid-12345")).thenReturn(Optional.of(device));

        DeviceStatusResponse resp = deviceService.checkDeviceStatus("2GI24IS999", "token-uuid-12345");
        assertNotNull(resp);
        assertEquals("MISMATCH", resp.getStatus());
        assertFalse(resp.isApproved());
    }

    @Test
    @DisplayName("Request registration creates new device with PENDING status")
    void testRequestRegistrationSuccess() {
        DeviceRegistrationRequest req = new DeviceRegistrationRequest();
        req.setUsn("2GI24IS042");
        req.setDeviceToken("token-uuid-12345");
        req.setDeviceModel("OnePlus Nord CE 3");
        req.setOperatingSystem("Android 14");
        req.setBrowser("Chrome Mobile");

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        httpReq.setRemoteAddr("192.168.1.100");
        httpReq.addHeader("User-Agent", "Mozilla/5.0 Android");

        when(studentRepository.findByUsnIgnoreCase("2GI24IS042")).thenReturn(Optional.of(student));
        when(deviceRepository.findByDeviceToken("token-uuid-12345")).thenReturn(Optional.empty());
        when(deviceRepository.save(any(StudentDevice.class))).thenAnswer(inv -> inv.getArgument(0));

        DeviceStatusResponse resp = deviceService.requestRegistration(req, httpReq);
        assertNotNull(resp);
        assertEquals("PENDING", resp.getStatus());
        verify(deviceRepository, times(1)).save(any(StudentDevice.class));
    }

    @Test
    @DisplayName("Admin approve transitions status to APPROVED with timestamp and reviewer")
    void testApproveDevice() {
        when(deviceRepository.findById("dev-1")).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(StudentDevice.class))).thenAnswer(inv -> inv.getArgument(0));

        DeviceDto dto = deviceService.approveDevice("dev-1", "admin@renaissance.edu");
        assertNotNull(dto);
        assertEquals("APPROVED", dto.getStatus());
        assertEquals("admin@renaissance.edu", dto.getApprovedBy());
        assertNotNull(dto.getApprovedAt());
    }

    @Test
    @DisplayName("Admin reject sets status to REJECTED with reason")
    void testRejectDevice() {
        when(deviceRepository.findById("dev-1")).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(StudentDevice.class))).thenAnswer(inv -> inv.getArgument(0));

        DeviceDto dto = deviceService.rejectDevice("dev-1", "Suspected emulator", "admin@renaissance.edu");
        assertNotNull(dto);
        assertEquals("REJECTED", dto.getStatus());
        assertEquals("Suspected emulator", dto.getRejectedReason());
    }

    @Test
    @DisplayName("Admin revoke transitions approved device to REVOKED")
    void testRevokeDevice() {
        device.setStatus(StudentDevice.DeviceStatus.APPROVED);
        when(deviceRepository.findById("dev-1")).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(StudentDevice.class))).thenAnswer(inv -> inv.getArgument(0));

        DeviceDto dto = deviceService.revokeDevice("dev-1", "Student lost phone", "admin@renaissance.edu");
        assertNotNull(dto);
        assertEquals("REVOKED", dto.getStatus());
        assertEquals("Student lost phone", dto.getRevocationReason());
    }

    @Test
    @DisplayName("Device stats counts devices by status")
    void testGetDeviceStats() {
        when(deviceRepository.countByStatus(StudentDevice.DeviceStatus.PENDING)).thenReturn(3L);
        when(deviceRepository.countByStatus(StudentDevice.DeviceStatus.APPROVED)).thenReturn(10L);
        when(deviceRepository.countByStatus(StudentDevice.DeviceStatus.REJECTED)).thenReturn(1L);
        when(deviceRepository.countByStatus(StudentDevice.DeviceStatus.REVOKED)).thenReturn(2L);

        Map<String, Long> stats = deviceService.getDeviceStats();
        assertEquals(3L, stats.get("PENDING"));
        assertEquals(10L, stats.get("APPROVED"));
        assertEquals(1L, stats.get("REJECTED"));
        assertEquals(2L, stats.get("REVOKED"));
    }
}
