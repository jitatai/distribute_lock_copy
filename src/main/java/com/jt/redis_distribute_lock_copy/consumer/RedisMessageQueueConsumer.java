package com.jt.redis_distribute_lock_copy.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jt.redis_distribute_lock_copy.constant.RedisKeyConst;
import com.jt.redis_distribute_lock_copy.pojo.ResumeCollectionDO;
import com.jt.redis_distribute_lock_copy.pojo.ResumeCollectionDTO;
import com.jt.redis_distribute_lock_copy.service.RedisService;
import com.jt.redis_distribute_lock_copy.third_party.AsyncResumeParser;
import com.jt.redis_distribute_lock_copy.third_party.ParseException;
import com.jt.redis_distribute_lock_copy.third_party.PredictResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author jiatai.hu
 * @version 1.0
 * @date 2021/4/8 10:30
 */
@Component
@Slf4j
public class RedisMessageQueueConsumer implements ApplicationListener<ContextRefreshedEvent> {

    @Resource
    private RedisService redisService;
    @Resource
    private AsyncResumeParser asyncResumeParser;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        log.info("开始监听RedisMessageQueue...");
        // 新建一个异步任务监听
        CompletableFuture.runAsync(()->{
            // 大循环 不断监听消息队列中的消息 阻塞式
            while (true){
                // 阻塞监听 每5秒获取一次 不为空返回
                ResumeCollectionDTO resumeCollectionDTO = (ResumeCollectionDTO) redisService
                        .popQueue(RedisKeyConst.RESUME_PARSE_TASK_QUEUE,5, TimeUnit.SECONDS);
                // 获取到队列消息时处理
                if (resumeCollectionDTO != null){
                    int rePullCount = 0;
                    int reTryCount = 0;

                    log.info("从队列中取出简历：{}",resumeCollectionDTO.getName());
                    log.info("----------开始拉取简历：{}------",resumeCollectionDTO.getName());

                    // 获取异步解析简历结果ID
                    Long asyncPredictId = resumeCollectionDTO.getAsyncPredictId();

                    // 每次任务多次调用第三方解析接口 即拉取解析好的简历 直到获取最终结果或丢弃任务
                    while (true){
                        try {
                            PredictResult result = asyncResumeParser.getResult(asyncPredictId);
                            // 拉取次数累加
                            rePullCount++;
                            // 简历解析完毕后
                            if (2 == result.getStatus()){
                                // 保存数据库
                                try {
                                    log.info("解析简历成功：{}",resumeCollectionDTO.getName());
                                    log.info("解析的Json为：{}",result.getResultJson());
                                    ResumeCollectionDO resumeCollectionDO = objectMapper.readValue(result.getResultJson(), ResumeCollectionDO.class);
                                    log.info("简历：{}存入数据库",resumeCollectionDO);
                                    rePullCount = 0;
                                    reTryCount = 0;
                                    break;
                                }catch (Exception e){
                                    discardTask(resumeCollectionDTO);
                                    e.printStackTrace();
                                    log.info("简历拉取/解析失败...丢弃该任务");
                                    rePullCount = 0;
                                    reTryCount = 0;
                                    break;
                                }
                            }else {
                                // 第三方解析还未完成解析 则重新拉取
                                try {

                                }catch (Exception e){
                                    int timeout = 1;
                                    if (rePullCount <= 3){
                                        // 前三次 等待1s后重试
                                        timeout = 1;
                                        TimeUnit.SECONDS.sleep(timeout);
                                    }else if (rePullCount <= 6){
                                        timeout = 2;
                                        TimeUnit.SECONDS.sleep(timeout);
                                    }else if (rePullCount <= 9){
                                        timeout = 2;
                                        TimeUnit.SECONDS.sleep(timeout);
                                    }else {
                                        discardTask(resumeCollectionDTO);
                                        log.info("多次拉取解析的简历仍未获得结果，丢弃简历：{}");
                                        rePullCount = 0;
                                        reTryCount = 0;
                                        break;
                                    }
                                    log.info("简历：{}尚未解析完毕，正进行第{}次重试，停顿后{}秒执行...",resumeCollectionDTO.getName(),rePullCount,timeout);
                                }
                            }

                        } catch (Exception e) {
                            if (reTryCount > 3){
                                discardTask(resumeCollectionDTO);
                                log.info("<<<<<<<<<<<<<<<<<<<简历:{}重试{}次后放弃, rePullCount:{}, retryCount:{}", resumeCollectionDTO.getName(), reTryCount, rePullCount, reTryCount);
                                rePullCount = 0;
                                reTryCount = 0;
                                break;
                            }
                            reTryCount++;
                            log.info("简历：{}远程调用异常，正准备第{}次重试",resumeCollectionDTO.getName(),reTryCount,e);
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private void discardTask(ResumeCollectionDTO resumeCollectionDTO) {
        log.info("------丢弃任务：{}------",resumeCollectionDTO.getName());
    }
}
