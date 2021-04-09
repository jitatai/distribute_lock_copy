package com.jt.redis_distribute_lock_copy.third_party;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jt.redis_distribute_lock_copy.pojo.ResumeCollectionDO;
import com.jt.redis_distribute_lock_copy.pojo.ResumeCollectionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author jiatai.hu
 * @version 1.0
 * @date 2021/4/7 16:11
 */
@Slf4j
@Component
public class AsyncResumeParser {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 异步任务结果ID
     */
    public static final AtomicLong ASYNC_RESULT_ID = new AtomicLong(1000);

    /**
     * 解析结果 简历ID 解析简历JSON
     */
    public static final Map<Long,String> RESULTS = new HashMap<>();


    /**
     * 异步解析 返回解析结果ID
     * @param resumeCollectionDO
     */
    public Long asyncParse(ResumeCollectionDO resumeCollectionDO){
        long resultId = ASYNC_RESULT_ID.incrementAndGet();
        try {
            String resultJson = objectMapper.writeValueAsString(resumeCollectionDO);
            RESULTS.put(resultId,resultJson);
            return resultId;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return -1L;
    }

    /**
     * 解析状态
     * 0 初始化
     * 1 处理中
     * 2 调用成功
     * 3 调用失败
     * @param resultID
     * @return
     * @throws InterruptedException
     */
    public PredictResult getResult(Long resultID) throws InterruptedException, ParseException {
        int mock = ThreadLocalRandom.current().nextInt(100);
        if (mock >= 85){
            // 模拟解析完成
            String resultJson = RESULTS.get(resultID);
            TimeUnit.SECONDS.sleep(2);
            return new PredictResult(resultJson,2);
        }else if (mock <= 5){
            // 模拟解析异常
            TimeUnit.SECONDS.sleep(1);
            throw new ParseException("简历解析异常");
        }
        // 模拟正在解析中
        TimeUnit.SECONDS.sleep(1);
        return new PredictResult("",1);
    }

}
