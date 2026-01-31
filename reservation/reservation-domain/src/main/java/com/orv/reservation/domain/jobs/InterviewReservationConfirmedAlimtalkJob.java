package com.orv.reservation.domain.jobs;

import com.orv.notification.domain.AlimtalkContent;
import com.orv.notification.external.AlimtalkService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InterviewReservationConfirmedAlimtalkJob implements Job{

    @Autowired
    private AlimtalkService alimtalkService;

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();

        try {
            AlimtalkContent alimtalkContent = new AlimtalkContent();
            alimtalkContent.setMsgType("AI");
            alimtalkContent.setTo(jobDataMap.getString("phoneNumber"));
            alimtalkContent.setTemplateCode("orv-reservation-confirmed-v1");
            alimtalkContent.setText("[오브] 인터뷰 예약이 확정되었어요.\n예약된 시간이 다가오면 다시 알려드릴게요.");

            String msgKey = alimtalkService.sendAlimtalk(alimtalkContent);
            log.info("Sent reservation confirm alimtalk with msgKey: {}", msgKey);
        } catch (Exception e) {
            log.error("Failed to send alimtalk", e);
            return;
        }

    }
}
