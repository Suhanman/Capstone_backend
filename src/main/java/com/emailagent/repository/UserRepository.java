package com.emailagent.repository;

import com.emailagent.domain.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.userId = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);

    // 관리자 - 이름 검색 (페이징)
    Page<User> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // 관리자 - 이메일 검색 (페이징)
    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    // 관리자 - 업종 검색: BusinessProfiles JOIN (native)
    @Query(value = "SELECT u.* FROM Users u LEFT JOIN BusinessProfiles bp ON u.user_id = bp.user_id WHERE bp.industry_type LIKE CONCAT('%', :keyword, '%') ORDER BY u.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM Users u LEFT JOIN BusinessProfiles bp ON u.user_id = bp.user_id WHERE bp.industry_type LIKE CONCAT('%', :keyword, '%')",
           nativeQuery = true)
    Page<User> findByIndustryTypeKeyword(@Param("keyword") String keyword, Pageable pageable);
}
