package com.orv.api.global.bizgo;

import com.orv.api.domain.reservation.dto.AlimtalkContent;

public interface AlimtalkService {
    String sendAlimtalk(AlimtalkContent alimtalkContent) throws Exception;
}
