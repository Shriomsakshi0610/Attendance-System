package com.renaissance.repository;

import com.renaissance.entity.AttendanceRecord;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AttendanceRecordRepositoryImpl implements AttendanceRecordRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public AttendanceRecordRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public long countRecordsToday(LocalDateTime startOfDay, LocalDateTime endOfDay) {
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("markedAt").gte(startOfDay).lte(endOfDay)
        ));
        return mongoTemplate.count(query, AttendanceRecord.class);
    }

    @Override
    public long countPresentByStudentId(String studentId) {
        List<Criteria> criteriaList = new ArrayList<>();
        if (studentId != null && !studentId.isBlank()) {
            if (ObjectId.isValid(studentId)) {
                criteriaList.add(new Criteria().orOperator(
                        Criteria.where("student.$id").is(studentId),
                        Criteria.where("student.$id").is(new ObjectId(studentId))
                ));
            } else {
                criteriaList.add(Criteria.where("student.$id").is(studentId));
            }
        }
        criteriaList.add(Criteria.where("status").is(AttendanceRecord.AttendanceStatus.PRESENT.name()));

        Query query = new Query(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        return mongoTemplate.count(query, AttendanceRecord.class);
    }

    @Override
    public long countPresentByStudentIdAndSubjectId(String studentId, String subjectId) {
        List<Criteria> criteriaList = new ArrayList<>();

        if (studentId != null && !studentId.isBlank()) {
            if (ObjectId.isValid(studentId)) {
                criteriaList.add(new Criteria().orOperator(
                        Criteria.where("student.$id").is(studentId),
                        Criteria.where("student.$id").is(new ObjectId(studentId))
                ));
            } else {
                criteriaList.add(Criteria.where("student.$id").is(studentId));
            }
        }

        if (subjectId != null && !subjectId.isBlank()) {
            if (ObjectId.isValid(subjectId)) {
                criteriaList.add(new Criteria().orOperator(
                        Criteria.where("subject.$id").is(subjectId),
                        Criteria.where("subject.$id").is(new ObjectId(subjectId))
                ));
            } else {
                criteriaList.add(Criteria.where("subject.$id").is(subjectId));
            }
        }

        criteriaList.add(Criteria.where("status").is(AttendanceRecord.AttendanceStatus.PRESENT.name()));

        Query query = new Query(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        return mongoTemplate.count(query, AttendanceRecord.class);
    }

    @Override
    public List<AttendanceRecord> findFilteredRecords(String classId, String subjectId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Criteria> criteriaList = new ArrayList<>();

        if (classId != null && !classId.isBlank()) {
            if (ObjectId.isValid(classId)) {
                criteriaList.add(new Criteria().orOperator(
                        Criteria.where("collegeClass.$id").is(classId),
                        Criteria.where("collegeClass.$id").is(new ObjectId(classId))
                ));
            } else {
                criteriaList.add(Criteria.where("collegeClass.$id").is(classId));
            }
        }

        if (subjectId != null && !subjectId.isBlank()) {
            if (ObjectId.isValid(subjectId)) {
                criteriaList.add(new Criteria().orOperator(
                        Criteria.where("subject.$id").is(subjectId),
                        Criteria.where("subject.$id").is(new ObjectId(subjectId))
                ));
            } else {
                criteriaList.add(Criteria.where("subject.$id").is(subjectId));
            }
        }

        if (startDate != null) {
            criteriaList.add(Criteria.where("markedAt").gte(startDate));
        }
        if (endDate != null) {
            criteriaList.add(Criteria.where("markedAt").lte(endDate));
        }

        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        return mongoTemplate.find(query, AttendanceRecord.class);
    }
}
