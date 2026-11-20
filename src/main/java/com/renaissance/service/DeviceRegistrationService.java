package com.renaissance.service;

import com.renaissance.dto.DeviceDto;
import com.renaissance.dto.DeviceRegistrationRequest;
import com.renaissance.dto.DeviceStatusResponse;
import com.renaissance.entity.Student;
import com.renaissance.entity.StudentDevice;
import com.renaissance.exception.ResourceNotFoundException;
import com.renaissance.repository.StudentDeviceRepository;
import com.renaissance.repository.StudentRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeviceRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(DeviceRegistrationService.class);

    private final StudentDeviceRepository deviceRepository;
    private final StudentRepository studentRepository;

    public DeviceRegistrationService(StudentDeviceRepository deviceRepository,
                                   StudentRepository studentRepository) {
        this.deviceRepository = deviceRepository;
        this.studentRepository = studentRepository;
    }

    /**
     * Verifies whether the specified deviceToken is registered and approved for the student USN.
     */
    public DeviceStatusResponse checkDeviceStatus(String usn, String deviceToken) {
        if (deviceToken == null || deviceToken.isBlank()) {
            return DeviceStatusResponse.notRegistered("No device token found. Device registration is required.");
        }

        String trimmedToken = deviceToken.trim();
        Optional<StudentDevice> deviceOpt = deviceRepository.findByDeviceToken(trimmedToken);

        if (deviceOpt.isEmpty()) {
            return DeviceStatusResponse.notRegistered("Device is not registered with the institution.");
        }

        StudentDevice device = deviceOpt.get();

        // If USN is provided, verify ownership binding
        if (usn != null && !usn.isBlank()) {
            String trimmedUsn = usn.trim().toUpperCase();
            if (!device.getUsn().equalsIgnoreCase(trimmedUsn)) {
                return DeviceStatusResponse.mismatch(device.getUsn());
            }
        }

        switch (device.getStatus()) {
            case APPROVED:
                return DeviceStatusResponse.approved(device.getUsn(), device.getStudentName(), device.getId());
            case PENDING:
                return DeviceStatusResponse.pending(device.getUsn(), device.getStudentName());
            case REJECTED:
                return DeviceStatusResponse.rejected(device.getRejectedReason() != null ?
                        "Registration rejected: " + device.getRejectedReason() : "Device registration was rejected by administrator.");
            case REVOKED:
                return DeviceStatusResponse.revoked(device.getRevocationReason() != null ?
                        "Device revoked: " + device.getRevocationReason() : "Device registration has been revoked. Please submit a new registration request.");
            default:
                return DeviceStatusResponse.notRegistered("Unknown device status.");
        }
    }

    /**
     * Submits a student device registration request.
     */
    public DeviceStatusResponse requestRegistration(DeviceRegistrationRequest req, HttpServletRequest httpRequest) {
        if (req.getUsn() == null || req.getUsn().isBlank()) {
            throw new IllegalArgumentException("USN is required for device registration.");
        }
        if (req.getDeviceToken() == null || req.getDeviceToken().isBlank()) {
            throw new IllegalArgumentException("Device token is required.");
        }

        String usn = req.getUsn().trim().toUpperCase();
        String token = req.getDeviceToken().trim();

        Student student = studentRepository.findByUsnIgnoreCase(usn)
                .orElseThrow(() -> new IllegalArgumentException("No registered student found with USN: " + usn));

        String ip = httpRequest != null ? httpRequest.getRemoteAddr() : "unknown";
        String ua = httpRequest != null ? httpRequest.getHeader("User-Agent") : "unknown";

        // Check if this device token already exists
        Optional<StudentDevice> existingOpt = deviceRepository.findByDeviceToken(token);
        StudentDevice device;

        if (existingOpt.isPresent()) {
            device = existingOpt.get();
            // If device was previously registered to another student, reject hijack attempt
            if (!device.getUsn().equalsIgnoreCase(usn) && device.getStatus() == StudentDevice.DeviceStatus.APPROVED) {
                return DeviceStatusResponse.mismatch(device.getUsn());
            }
            device.setUsn(usn);
            device.setStudentId(student.getId());
            device.setStudentName(student.getName());
            device.setStatus(StudentDevice.DeviceStatus.PENDING);
            device.setRequestedAt(LocalDateTime.now());
            device.setDeviceModel(req.getDeviceModel());
            device.setOperatingSystem(req.getOperatingSystem());
            device.setBrowser(req.getBrowser());
            device.setDeviceFingerprint(req.getDeviceFingerprint());
            device.setIpAddress(ip);
            device.setUserAgent(ua);
        } else {
            device = new StudentDevice(
                    student.getId(),
                    usn,
                    student.getName(),
                    token,
                    req.getDeviceFingerprint(),
                    req.getDeviceModel(),
                    req.getOperatingSystem(),
                    req.getBrowser(),
                    ip,
                    ua
            );
        }

        StudentDevice saved = deviceRepository.save(device);
        log.info("Created device registration request ID {} for student {} ({})", saved.getId(), student.getName(), usn);

        return DeviceStatusResponse.pending(usn, student.getName());
    }

    /**
     * Admin: Retrieves all registered devices with optional status and search filters.
     */
    public List<DeviceDto> getAllDevices(String status, String keyword) {
        List<StudentDevice> devices;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try {
                StudentDevice.DeviceStatus ds = StudentDevice.DeviceStatus.valueOf(status.trim().toUpperCase());
                devices = deviceRepository.findByStatusOrderByRequestedAtDesc(ds);
            } catch (Exception e) {
                devices = deviceRepository.findAllByOrderByRequestedAtDesc();
            }
        } else {
            devices = deviceRepository.findAllByOrderByRequestedAtDesc();
        }

        if (keyword != null && !keyword.isBlank()) {
            String lower = keyword.trim().toLowerCase();
            devices = devices.stream().filter(d ->
                    (d.getUsn() != null && d.getUsn().toLowerCase().contains(lower)) ||
                    (d.getStudentName() != null && d.getStudentName().toLowerCase().contains(lower)) ||
                    (d.getDeviceModel() != null && d.getDeviceModel().toLowerCase().contains(lower)) ||
                    (d.getOperatingSystem() != null && d.getOperatingSystem().toLowerCase().contains(lower))
            ).collect(Collectors.toList());
        }

        return devices.stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Admin: Approves a student device registration.
     * Automatically revokes any previously approved device for the same student (1 active device policy).
     */
    public DeviceDto approveDevice(String deviceId, String adminEmail) {
        StudentDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device registration not found with ID: " + deviceId));

        // Revoke older approved devices for the same student
        List<StudentDevice> previousDevices = deviceRepository.findByUsnIgnoreCase(device.getUsn());
        for (StudentDevice prev : previousDevices) {
            if (!prev.getId().equals(device.getId()) && prev.getStatus() == StudentDevice.DeviceStatus.APPROVED) {
                prev.setStatus(StudentDevice.DeviceStatus.REVOKED);
                prev.setRevokedAt(LocalDateTime.now());
                prev.setRevocationReason("Replaced by newly approved device ID: " + device.getId());
                deviceRepository.save(prev);
                log.info("Revoked previous device {} for student {}", prev.getId(), device.getUsn());
            }
        }

        device.setStatus(StudentDevice.DeviceStatus.APPROVED);
        device.setApprovedAt(LocalDateTime.now());
        device.setApprovedBy(adminEmail != null ? adminEmail : "Administrator");
        device.setRejectedAt(null);
        device.setRejectedReason(null);
        device.setRevokedAt(null);
        device.setRevocationReason(null);

        StudentDevice saved = deviceRepository.save(device);
        log.info("Approved device {} for student {} by {}", saved.getId(), saved.getUsn(), adminEmail);
        return toDto(saved);
    }

    /**
     * Admin: Rejects a student device registration.
     */
    public DeviceDto rejectDevice(String deviceId, String reason, String adminEmail) {
        StudentDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device registration not found with ID: " + deviceId));

        device.setStatus(StudentDevice.DeviceStatus.REJECTED);
        device.setRejectedAt(LocalDateTime.now());
        device.setRejectedReason(reason != null && !reason.isBlank() ? reason.trim() : "Rejected by administrator");
        device.setApprovedAt(null);
        device.setApprovedBy(null);

        StudentDevice saved = deviceRepository.save(device);
        log.info("Rejected device {} for student {} by {}: {}", saved.getId(), saved.getUsn(), adminEmail, reason);
        return toDto(saved);
    }

    /**
     * Admin: Revokes an approved student device.
     */
    public DeviceDto revokeDevice(String deviceId, String reason, String adminEmail) {
        StudentDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device registration not found with ID: " + deviceId));

        device.setStatus(StudentDevice.DeviceStatus.REVOKED);
        device.setRevokedAt(LocalDateTime.now());
        device.setRevocationReason(reason != null && !reason.isBlank() ? reason.trim() : "Revoked by administrator");

        StudentDevice saved = deviceRepository.save(device);
        log.info("Revoked device {} for student {} by {}: {}", saved.getId(), saved.getUsn(), adminEmail, reason);
        return toDto(saved);
    }

    /**
     * Admin: Aggregated stats for device registrations.
     */
    public Map<String, Long> getDeviceStats() {
        Map<String, Long> stats = new HashMap<>();
        long total = deviceRepository.count();
        long pending = deviceRepository.countByStatus(StudentDevice.DeviceStatus.PENDING);
        long approved = deviceRepository.countByStatus(StudentDevice.DeviceStatus.APPROVED);
        long rejected = deviceRepository.countByStatus(StudentDevice.DeviceStatus.REJECTED);
        long revoked = deviceRepository.countByStatus(StudentDevice.DeviceStatus.REVOKED);

        stats.put("total", total);
        stats.put("pending", pending);
        stats.put("approved", approved);
        stats.put("rejected", rejected);
        stats.put("revoked", revoked);

        stats.put("TOTAL", total);
        stats.put("PENDING", pending);
        stats.put("APPROVED", approved);
        stats.put("REJECTED", rejected);
        stats.put("REVOKED", revoked);
        return stats;
    }

    public DeviceDto toDto(StudentDevice d) {
        if (d == null) return null;
        DeviceDto dto = new DeviceDto(
                d.getId(),
                d.getStudentId(),
                d.getUsn(),
                d.getStudentName(),
                d.getDeviceToken(),
                d.getDeviceModel(),
                d.getOperatingSystem(),
                d.getBrowser(),
                d.getStatus() != null ? d.getStatus().name() : "PENDING",
                d.getRequestedAt(),
                d.getApprovedAt(),
                d.getApprovedBy(),
                d.getLastUsedAt()
        );
        dto.setRejectedAt(d.getRejectedAt());
        dto.setRejectedReason(d.getRejectedReason());
        dto.setRevokedAt(d.getRevokedAt());
        dto.setRevocationReason(d.getRevocationReason());
        dto.setIpAddress(d.getIpAddress());
        return dto;
    }
}
