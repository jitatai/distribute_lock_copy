package com.jt.redis_distribute_lock_copy;

import com.jt.redis_distribute_lock_copy.constant.RedisKeyConst;
import com.jt.redis_distribute_lock_copy.pojo.ResumeCollectionDTO;
import com.jt.redis_distribute_lock_copy.service.RedisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DistributeLockCopyApplicationTests {

    @Autowired
    private RedisService redisService;

    /**
     * 作为失败案例（因为不存在777L这个解析任务，AsyncResumeParse.results会返回null）
     * 观察RedisMessageQueueConsumer的处理方式
     */
    @Test
    void contextLoads() {
        ResumeCollectionDTO resumeCollectionDTO = new ResumeCollectionDTO();
        resumeCollectionDTO.setId(666L);
        resumeCollectionDTO.setAsyncPredictId(777L);
        resumeCollectionDTO.setName("测试1号");

        redisService.pushQueue(RedisKeyConst.RESUME_PARSE_TASK_QUEUE, resumeCollectionDTO);

    }

}
