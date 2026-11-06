package com.renaissance.repository;

import com.renaissance.entity.TimetableSchedule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimetableScheduleRepository extends MongoRepository<TimetableSchedule, String> {

    List<TimetableSchedule> findByCollegeClass_Id(String classId);

    List<TimetableSchedule> findByTeacher_Id(String teacherId);

    List<TimetableSchedule> findByDepartment_Id(String departmentId);

    List<TimetableSchedule> findByDayOfWeek(String dayOfWeek);

    List<TimetableSchedule> findByCollegeClass_IdAndDayOfWeek(String classId, String dayOfWeek);

    List<TimetableSchedule> findByTeacher_IdAndDayOfWeek(String teacherId, String dayOfWeek);

    List<TimetableSchedule> findByRoomNumberIgnoreCaseAndDayOfWeek(String roomNumber, String dayOfWeek);

    long countBySubject_Id(String subjectId);

    long countByCollegeClass_Id(String classId);

    long countByTeacher_Id(String teacherId);
}
