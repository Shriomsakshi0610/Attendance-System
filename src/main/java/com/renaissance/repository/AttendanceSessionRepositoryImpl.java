package com.renaissance.repository;

import com.renaissance.entity.AttendanceSession;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AttendanceSessionRepositoryImpl implements AttendanceSessionRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public AttendanceSessionRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<AttendanceSession> findActiveConflictingSession(String classId, String subjectId, LocalDateTime now) {
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

        criteriaList.add(Criteria.where("status").is(AttendanceSession.SessionStatus.ACTIVE.name()));
        criteriaList.add(Criteria.where("endTime").gt(now));

        Query query = new Query(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        return Optional.ofNullable(mongoTemplate.findOne(query, AttendanceSession.class));
    }

    @Override
    public long countActiveSessions(LocalDateTime now) {
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("status").is(AttendanceSession.SessionStatus.ACTIVE.name()),
                Criteria.where("endTime").gt(now)
        ));
        return mongoTemplate.count(query, AttendanceSession.class);
    }

    @Override
    public long countSessionsToday(LocalDateTime startOfDay, LocalDateTime endOfDay) {
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("status").ne(AttendanceSession.SessionStatus.EXPIRED.name()),
                Criteria.where("createdAt").gte(startOfDay).lte(endOfDay)
        ));
        return mongoTemplate.count(query, AttendanceSession.class);
    }
}
