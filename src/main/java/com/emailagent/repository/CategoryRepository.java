package com.emailagent.repository;

import com.emailagent.domain.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUser_UserId(Long userId);
    boolean existsByUser_UserIdAndCategoryName(Long userId, String categoryName);
}
