package com.orv.api.domain.notification.service;

import com.orv.api.domain.reservation.service.dto.AlimtalkContent;

public interface AlimtalkService {
    String sendAlimtalk(AlimtalkContent alimtalkContent) throws Exception;
}
