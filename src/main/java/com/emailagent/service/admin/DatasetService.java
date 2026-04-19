package com.emailagent.service.admin;

import com.emailagent.dto.response.admin.training.DatasetCollectionCreateResponse;
import com.emailagent.dto.response.admin.training.DatasetListResponse;

public interface DatasetService {

    /** 등록된 데이터셋 버전, 건수, 상태 목록 조회 (최신순) */
    DatasetListResponse getDatasets();

    /**
     * 원천 데이터(Email) 기반 학습용 데이터셋 버전 생성.
     * MQ 없이 Backend 단독 처리 — 현재 이메일 수를 스냅샷으로 기록하고 버전을 발급함.
     */
    DatasetCollectionCreateResponse createDatasetCollection();
}
