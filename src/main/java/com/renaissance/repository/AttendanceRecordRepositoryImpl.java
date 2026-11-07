package com.renaissance.repository;

import com.renaissance.entity.AttendanceRecord;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public Map<String, Long> countPresentByStudentsInClass(String classId, String subjectId) {
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("status").is(AttendanceRecord.AttendanceStatus.PRESENT.name()));

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

        MatchOperation match = Aggregation.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        GroupOperation group = Aggregation.group("student.$id").count().as("count");
        Aggregation agg = Aggregation.newAggregation(match, group);

        AggregationResults<org.bson.Document> results = mongoTemplate.aggregate(agg, "attendance_records", org.bson.Document.class);
        Map<String, Long> map = new HashMap<>();
        for (org.bson.Document doc : results) {
            Object idObj = doc.get("_id");
            if (idObj != null) {
                String idStr = idObj.toString();
                Number countNum = (Number) doc.get("count");
                map.put(idStr, countNum != null ? countNum.longValue() : 0L);
            }
        }
        return map;
    }

    @Override
    public Map<String, Long> countRecordsByDay(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("markedAt").gte(startDateTime).lte(endDateTime)
        ));
        query.fields().include("markedAt");
        List<AttendanceRecord> records = mongoTemplate.find(query, AttendanceRecord.class);

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("EEE");
        Map<String, Long> map = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate d = java.time.LocalDate.now().minusDays(i);
            map.put(d.format(fmt), 0L);
        }

        for (AttendanceRecord r : records) {
            if (r.getMarkedAt() != null) {
                String day = r.getMarkedAt().format(fmt);
                map.put(day, map.getOrDefault(day, 0L) + 1L);
            }
        }
        return map;
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
