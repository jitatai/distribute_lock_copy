package com.jt.redis_distribute_lock_copy.service;

import java.util.concurrent.TimeUnit;

/**
 * @author jiatai.hu
 * @version 1.0
 * @date 2021/4/7 15:41
 */
public interface RedisService {
    void pushQueue(String queue, Object value);
    Object popQueue(String queue, long timeout, TimeUnit timeUnit);

    /**
     * 尝试加锁
     * @param lockKey
     * @param value
     * @param expireTime
     * @param timeUnit
     * @return
     */
    boolean tryLock(String lockKey, Object value, long expireTime,TimeUnit timeUnit);

    /**
     * 尝试原子性加锁
     * @param lockKey
     * @param value
     * @param expireTime
     * @param timeUnit
     * @return
     */
    boolean tryLockAtomic(String lockKey, Object value, long expireTime,TimeUnit timeUnit);

    /**
     * 根据Machine_ID解锁 解锁自己对应的
     * @param lockKey
     * @param value
     * @return
     */
    boolean unLock(String lockKey, Object value);

    /**
     * 根据lockKey直接释放
     * @param lockKey
     * @return
     */
    boolean releaseLock(String lockKey);
}
