package com.renaissance.repository;

import com.renaissance.entity.HistoricalAttendanceImportLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoricalAttendanceImportLogRepository extends MongoRepository<HistoricalAttendanceImportLog, String> {

    List<HistoricalAttendanceImportLog> findAllByOrderByImportTimestampDesc();

    List<HistoricalAttendanceImportLog> findByAdminEmailOrderByImportTimestampDesc(String adminEmail);
}
