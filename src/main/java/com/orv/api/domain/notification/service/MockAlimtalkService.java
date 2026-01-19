package com.orv.api.domain.notification.service;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.orv.api.domain.reservation.service.dto.AlimtalkContent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Primary // 임시로 모든 환경에서 알림톡 비활성화
public class MockAlimtalkService implements AlimtalkService {
    @Override
    public String sendAlimtalk(AlimtalkContent alimtalkContent) throws Exception {
        // do nothing
        return "this is mock alimtalkservice response";
    }
}