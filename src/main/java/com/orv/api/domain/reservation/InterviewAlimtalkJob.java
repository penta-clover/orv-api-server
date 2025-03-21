package com.orv.api.domain.reservation;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
public class InterviewAlimtalkJob implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
      log.info("alimtalk job executed!");
    }
}
