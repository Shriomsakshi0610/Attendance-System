package com.renaissance.repository;

import com.renaissance.entity.Student;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Repository
public class StudentRepositoryImpl implements StudentRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public StudentRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Student> searchStudents(String deptId, String classId, String keyword) {
        return searchStudents(deptId, classId, keyword, null, null);
    }

    @Override
    public List<Student> searchStudents(String deptId, String classId, String keyword, Integer page, Integer size) {
        List<Criteria> criteriaList = new ArrayList<>();

        if (deptId != null && !deptId.isBlank()) {
            if (ObjectId.isValid(deptId)) {
                criteriaList.add(new Criteria().orOperator(
                        Criteria.where("department.$id").is(deptId),
                        Criteria.where("department.$id").is(new ObjectId(deptId))
                ));
            } else {
                criteriaList.add(Criteria.where("department.$id").is(deptId));
            }
        }

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

        if (keyword != null && !keyword.isBlank()) {
            Pattern regex = Pattern.compile(Pattern.quote(keyword.trim()), Pattern.CASE_INSENSITIVE);
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("name").regex(regex),
                    Criteria.where("usn").regex(regex)
            ));
        }

        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        if (page != null && size != null && size > 0) {
            query.skip((long) page * size);
            query.limit(size);
        }

        return mongoTemplate.find(query, Student.class);
    }

    @Override
    public java.util.Map<String, Long> countStudentsByClassIds(java.util.Collection<String> classIds) {
        if (classIds == null || classIds.isEmpty()) return new java.util.HashMap<>();

        List<Criteria> orList = new ArrayList<>();
        for (String cId : classIds) {
            if (cId != null && !cId.isBlank()) {
                if (ObjectId.isValid(cId)) {
                    orList.add(new Criteria().orOperator(
                            Criteria.where("collegeClass.$id").is(cId),
                            Criteria.where("collegeClass.$id").is(new ObjectId(cId))
                    ));
                } else {
                    orList.add(Criteria.where("collegeClass.$id").is(cId));
                }
            }
        }
        if (orList.isEmpty()) return new java.util.HashMap<>();

        org.springframework.data.mongodb.core.aggregation.MatchOperation match =
                org.springframework.data.mongodb.core.aggregation.Aggregation.match(new Criteria().orOperator(orList.toArray(new Criteria[0])));
        org.springframework.data.mongodb.core.aggregation.GroupOperation group =
                org.springframework.data.mongodb.core.aggregation.Aggregation.group("collegeClass.$id").count().as("count");
        org.springframework.data.mongodb.core.aggregation.Aggregation agg =
                org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(match, group);

        org.springframework.data.mongodb.core.aggregation.AggregationResults<org.bson.Document> results =
                mongoTemplate.aggregate(agg, "students", org.bson.Document.class);
        java.util.Map<String, Long> map = new java.util.HashMap<>();
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
}
