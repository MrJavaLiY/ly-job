package com.liyang.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * JobTest 类的简要描述
 *
 * @author liyang
 * @since 2025/5/16
 */
@Component
@Slf4j
public class JobTest {
    @Scheduled(cron = "0/5 * * * * ?")
    public void test() throws InterruptedException {
        Thread.sleep(10000);
        log.info("test");
    }
}
