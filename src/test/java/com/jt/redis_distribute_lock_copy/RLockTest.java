package com.jt.redis_distribute_lock_copy;

import io.lettuce.core.RedisClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author jiatai.hu
 * @version 1.0
 * @date 2021/4/9 15:48
 */
@SpringBootTest
@Slf4j
class RLockTest {

    @Autowired
    RedissonClient redissonClient;

    @Test
    public void testRlock() throws InterruptedException {
        CompletableFuture.runAsync(this::taskLockOne);
        CompletableFuture.runAsync(this::taskLockTwo);

        TimeUnit.SECONDS.sleep(200);
    }

    private void taskLockOne() {
        try {
            RLock lock = redissonClient.getLock("jiata_distribute_lock");
            log.info("taskLockOne尝试加锁...");
            lock.lock();
            log.info("taskLockOne加锁成功...");
            log.info("taskLockOne业务开始...");


            TimeUnit.SECONDS.sleep(50);
            log.info("taskLockOne任务执行结束...");

            lock.unlock();
            log.info("taskLockOne解锁成功..");

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void taskLockTwo() {
        try {
            RLock lock = redissonClient.getLock("jiata_distribute_lock");
            log.info("taskLockTwo尝试加锁...");
            lock.lock();
            log.info("taskLockTwo加锁成功...");
            log.info("taskLockTwo业务开始...");

            TimeUnit.SECONDS.sleep(50);
            log.info("taskLockTwo任务执行结束...");

            lock.unlock();
            log.info("taskLockTwo解锁成功..");

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
