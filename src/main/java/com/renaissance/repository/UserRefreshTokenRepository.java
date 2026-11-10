package com.renaissance.repository;

import com.renaissance.entity.UserRefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRefreshTokenRepository extends MongoRepository<UserRefreshToken, String> {

    Optional<UserRefreshToken> findByToken(String token);

    List<UserRefreshToken> findByTeacherId(String teacherId);

    List<UserRefreshToken> findByEmailIgnoreCase(String email);

    void deleteByToken(String token);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);

    void deleteByTeacherId(String teacherId);

    void deleteByRotatedTrueAndRotatedAtBefore(LocalDateTime dateTime);
}
