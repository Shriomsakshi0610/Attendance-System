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
}
