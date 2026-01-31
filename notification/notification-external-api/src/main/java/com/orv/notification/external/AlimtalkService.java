package com.orv.notification.external;

import com.orv.notification.domain.AlimtalkContent;

public interface AlimtalkService {
    String sendAlimtalk(AlimtalkContent alimtalkContent) throws Exception;
}
