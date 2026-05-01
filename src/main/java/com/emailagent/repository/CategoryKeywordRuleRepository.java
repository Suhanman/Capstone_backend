package com.emailagent.repository;

import com.emailagent.domain.entity.CategoryKeywordRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryKeywordRuleRepository extends JpaRepository<CategoryKeywordRule, Long> {

    Optional<CategoryKeywordRule> findByCategoryName(String categoryName);

    List<CategoryKeywordRule> findAllByOrderByCategoryNameAsc();
}
