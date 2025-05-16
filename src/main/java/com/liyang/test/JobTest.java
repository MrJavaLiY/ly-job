package com.liyang.test;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * JobTest 类的简要描述
 *
 * @author liyang
 * @since 2025/5/16
 */
@Component
public class JobTest {
    @Scheduled(cron = "0/5 * * * * ?")
    public void test() {
        System.out.println("test");
    }
}
