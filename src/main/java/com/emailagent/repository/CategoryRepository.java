package com.emailagent.repository;

import com.emailagent.domain.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUser_UserId(Long userId);
    boolean existsByUser_UserIdAndCategoryName(Long userId, String categoryName);
    Optional<Category> findByUser_UserIdAndCategoryName(Long userId, String categoryName);

    @Query("""
            SELECT c
            FROM Category c
            JOIN FETCH c.user u
            ORDER BY c.categoryName ASC, u.userId ASC
            """)
    List<Category> findAllWithUserOrderByCategoryNameAndUserId();

    @Query("""
            SELECT c
            FROM Category c
            JOIN FETCH c.user u
            WHERE c.categoryName = :categoryName
            ORDER BY u.userId ASC
            """)
    List<Category> findAllByCategoryNameWithUser(@Param("categoryName") String categoryName);
}
