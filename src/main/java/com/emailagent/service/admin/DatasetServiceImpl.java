package com.emailagent.service.admin;

import com.emailagent.domain.entity.TrainingDataset;
import com.emailagent.domain.enums.DatasetStatus;
import com.emailagent.dto.response.admin.training.DatasetCollectionCreateResponse;
import com.emailagent.dto.response.admin.training.DatasetListResponse;
import com.emailagent.repository.EmailRepository;
import com.emailagent.repository.TrainingDatasetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetServiceImpl implements DatasetService {

    private static final DateTimeFormatter VERSION_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter RESPONSE_FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final TrainingDatasetRepository trainingDatasetRepository;
    private final EmailRepository emailRepository;

    @Override
    @Transactional(readOnly = true)
    public DatasetListResponse getDatasets() {
        List<DatasetListResponse.DatasetSummary> summaries = trainingDatasetRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(DatasetListResponse.DatasetSummary::from)
                .toList();
        return new DatasetListResponse(summaries);
    }

    /**
     * 학습용 데이터셋 버전 생성.
     *
     * [처리 흐름]
     * 1. 현재 Email 테이블 전체 건수 조회 (학습 가능한 원천 데이터 규모 기록)
     * 2. 타임스탬프 기반 dataset_version 자동 생성 (예: v20260413_153045)
     * 3. training_datasets 테이블에 insert (status = READY)
     *
     * [MQ 미사용 이유]
     * 원천 데이터(Email)는 Backend DB에 존재하므로 AI 서버에 전달할 작업이 없음.
     * 이 버전 식별자를 이후 preprocessing/training 등의 Job 요청에 참조하는 방식으로 사용.
     */
    @Override
    @Transactional
    public DatasetCollectionCreateResponse createDatasetCollection() {
        int recordCount = (int) emailRepository.count();
        String datasetVersion = "v" + LocalDateTime.now().format(VERSION_FORMATTER);

        TrainingDataset dataset = TrainingDataset.builder()
                .datasetVersion(datasetVersion)
                .recordCount(recordCount)
                .status(DatasetStatus.READY)
                .build();
        trainingDatasetRepository.save(dataset);

        log.info("[DatasetService] 데이터셋 버전 생성 — version={}, recordCount={}",
                datasetVersion, recordCount);

        String createdAt = dataset.getCreatedAt() != null
                ? dataset.getCreatedAt().format(RESPONSE_FORMATTER)
                : LocalDateTime.now().format(RESPONSE_FORMATTER);

        return new DatasetCollectionCreateResponse(
                dataset.getDatasetId(),
                datasetVersion,
                recordCount,
                DatasetStatus.READY.name(),
                createdAt
        );
    }
}
