package com.emailagent.service;

import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.repository.TemplateRepository;
import com.emailagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TemplateNumberService {

    private final UserRepository userRepository;
    private final TemplateRepository templateRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public Long nextUserTemplateNo(Long userId) {
        userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        return templateRepository.findMaxUserTemplateNoByUserId(userId) + 1;
    }
}
