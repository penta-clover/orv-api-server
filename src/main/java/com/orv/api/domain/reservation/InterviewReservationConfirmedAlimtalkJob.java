package com.orv.api.domain.reservation;

import com.orv.api.global.bizgo.AlimtalkService;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InterviewReservationConfirmedAlimtalkJob implements Job {
    @Autowired
    private AlimtalkService alimtalkService;

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();

        try {
            String msgKey = alimtalkService.sendAlimtalk(
                    jobDataMap.getString("phoneNumber"),
                    "orv-reservation-confirmed",
                    "오브 인터뷰 예약 완료",
                    "인터뷰가 확정되었어요 예약된 시간이 다가오면 다시 알려드릴게요."
            );

            log.info("Sent alimtalk with msgKey: {}", msgKey);
        } catch (Exception e) {
            log.error("Failed to send alimtalk", e);
            return;
        }

    }
}
