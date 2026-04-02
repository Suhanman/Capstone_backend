package com.emailagent.repository;

import com.emailagent.domain.entity.Integration;
import com.emailagent.domain.enums.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IntegrationRepository extends JpaRepository<Integration, Long> {

    Optional<Integration> findByUser_UserId(Long userId);

    // Pub/Sub 알림의 emailAddress로 연동 정보 조회
    Optional<Integration> findByConnectedEmail(String connectedEmail);

    boolean existsByUser_UserId(Long userId);

    void deleteByUser_UserId(Long userId);

    // 관리자 대시보드: 연동 상태별 사용자 수
    long countBySyncStatus(SyncStatus syncStatus);

    // 관리자 대시보드: Gmail / Calendar 연동 완료 사용자 수 (Granular Consent)
    long countByIsGmailConnectedTrue();
    long countByIsCalendarConnectedTrue();
}
