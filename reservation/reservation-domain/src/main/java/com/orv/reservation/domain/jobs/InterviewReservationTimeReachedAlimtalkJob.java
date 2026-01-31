package com.orv.reservation.domain.jobs;

import com.orv.notification.domain.AlimtalkButton;
import com.orv.notification.domain.AlimtalkContent;
import com.orv.notification.external.AlimtalkService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class InterviewReservationTimeReachedAlimtalkJob implements Job {
    @Autowired
    private AlimtalkService alimtalkService;

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();

        try {
            AlimtalkContent alimtalkContent = new AlimtalkContent();
            alimtalkContent.setMsgType("AI");
            alimtalkContent.setTo(jobDataMap.getString("phoneNumber"));
            alimtalkContent.setTemplateCode("orv-reservation-noti-v1");
            alimtalkContent.setText("[오브] 잠시 후 인터뷰가 시작됩니다.\n안녕하세요. 현준님을 위한 인터뷰가 준비됐어요.\n'인터뷰 시작하기' 버튼을 클릭하면 바로 시작할 수 있어요.");
            alimtalkContent.setButtons(List.of(new AlimtalkButton("인터뷰 시작하기", "WL", "https://www.orv.im", "https://www.orv.im")));

            String msgKey = alimtalkService.sendAlimtalk(alimtalkContent);
            log.info("Sent alimtalk with msgKey: {}", msgKey);
        } catch (Exception e) {
            log.error("Failed to send alimtalk", e);
            return;
        }

    }
}
