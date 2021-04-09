package com.jt.redis_distribute_lock_copy.service.impl;

import com.jt.redis_distribute_lock_copy.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author jiatai.hu
 * @version 1.0
 * @date 2021/4/7 15:51
 */
@Slf4j
@Service
public class RedisServiceImpl implements RedisService {

    @Autowired
    RedisTemplate redisTemplate;

    @Override
    public void pushQueue(String queue, Object value) {
        redisTemplate.opsForList().leftPush(queue,value);
    }

    @Override
    public Object popQueue(String queue, long timeout, TimeUnit timeUnit) {
        return redisTemplate.opsForList().rightPop(queue,timeout,timeUnit);
    }

    @Override
    public boolean tryLock(String lockKey, Object value, long expireTime, TimeUnit timeUnit) {
        // 先setnx
        boolean lock = redisTemplate.opsForValue().setIfAbsent(lockKey, value);
        if (lock){
            // 给lockKey设置过期时间 再expire
            redisTemplate.expire(lockKey,expireTime,timeUnit);
        }
        return lock;
    }

    @Override
    public boolean tryLockAtomic(String lockKey, Object value, long expireTime, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(lockKey,value,expireTime,timeUnit);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean unLock(String lockKey, Object value) {
        Object originValue = redisTemplate.opsForValue().get(lockKey);
        if (originValue != null && value.equals(originValue)){
            redisTemplate.delete(lockKey);
            return true;
        }
        return false;
    }

    @Override
    public boolean releaseLock(String lockKey) {
        Boolean delete = redisTemplate.delete(lockKey);
        if (Boolean.TRUE.equals(delete)){
            Object originValue = redisTemplate.opsForValue().get(lockKey);
            log.info("Spring启动，节点：{}成功释放上次简历汇聚定时任务锁",originValue);
            return true;
        }
        return false;
    }
}
