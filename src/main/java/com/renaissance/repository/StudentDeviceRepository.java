package com.renaissance.repository;

import com.renaissance.entity.StudentDevice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentDeviceRepository extends MongoRepository<StudentDevice, String> {

    Optional<StudentDevice> findByDeviceToken(String deviceToken);

    List<StudentDevice> findByUsnIgnoreCase(String usn);

    Optional<StudentDevice> findByDeviceTokenAndUsnIgnoreCase(String deviceToken, String usn);

    List<StudentDevice> findByStatus(StudentDevice.DeviceStatus status);

    List<StudentDevice> findByStatusOrderByRequestedAtDesc(StudentDevice.DeviceStatus status);

    List<StudentDevice> findAllByOrderByRequestedAtDesc();

    long countByUsnIgnoreCaseAndStatus(String usn, StudentDevice.DeviceStatus status);

    long countByStatus(StudentDevice.DeviceStatus status);
}
