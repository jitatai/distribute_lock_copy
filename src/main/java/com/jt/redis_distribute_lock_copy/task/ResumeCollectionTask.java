package com.jt.redis_distribute_lock_copy.task;

import com.jt.redis_distribute_lock_copy.constant.RedisKeyConst;
import com.jt.redis_distribute_lock_copy.pojo.ResumeCollectionDO;
import com.jt.redis_distribute_lock_copy.pojo.ResumeCollectionDTO;
import com.jt.redis_distribute_lock_copy.service.RedisService;
import com.jt.redis_distribute_lock_copy.third_party.AsyncResumeParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 简历收集主任务
 * @author jiatai.hu
 * @version 1.0
 * @date 2021/4/7 16:35
 */
@Slf4j
@Component
@EnableScheduling
public class ResumeCollectionTask implements ApplicationListener<ContextRefreshedEvent> {

    @Resource
    private RedisService redisService;
    @Resource
    private AsyncResumeParser asyncResumeParser;

    /**
     * 每个服务器随机获取
     */
    public static final String MACHINE_ID = String.valueOf(ThreadLocalRandom.current().nextLong(10000000));

    /**
     * 项目启动后 尝试删除之前的锁（如果存在） 防止死锁
     * @param contextRefreshedEvent
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        redisService.releaseLock(RedisKeyConst.RESUME_PULL_TASK_LOCK);
    }

    /**
     * 每一分钟 调用一次此方法
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void resumeSchedule(){
        // 尝试上锁，返回true或false，锁的过期时间设置为10分钟（实际要根据项目调整，这也是自己实现Redis分布式锁的难点之一）
        boolean lock = redisService.tryLock(RedisKeyConst.RESUME_PULL_TASK_LOCK, MACHINE_ID, 10, TimeUnit.MINUTES);
        // 如果当前服务器成功获取锁，那么整个系统只允许当前程序去MySQL拉取待执行任务
        if (lock) {
            log.info("节点：{}获取锁成功，任务开始",MACHINE_ID);
            try {
                collectResume();
            }catch (Exception e){
                e.printStackTrace();
                log.info("节点{}，任务执行异常",e);
            }finally {
                // 执行完毕释放当前MACHINE_ID的锁
                redisService.unLock(RedisKeyConst.RESUME_PULL_TASK_LOCK,MACHINE_ID);
                log.info("节点：{}，任务执行完毕，释放锁",MACHINE_ID);
            }
        }else {
            log.info("节点：{}，获取锁失败",MACHINE_ID);
        }
    }

    /**
     * 任务主体
     * 1.从数据库拉取符合条件的HR邮箱
     * 2.从HR邮箱拉取附件简历
     * 3.调用远程服务异步解析简历
     * 4.插入待处理任务到数据库，作为记录留存
     * 5.把待处理任务的id丢到Redis Message Queue，让Consumer异步处理
     */
    private void collectResume() throws InterruptedException {
        // 模拟从数据库拉取数据
        log.info("节点{}：从数据库拉取任务简历",MACHINE_ID);
        List<ResumeCollectionDO> resumeCollectionDOList = new ArrayList<>();
        resumeCollectionDOList.add(new ResumeCollectionDO(1L, "张三的简历.pdf"));
        resumeCollectionDOList.add(new ResumeCollectionDO(2L, "李四的简历.html"));
        resumeCollectionDOList.add(new ResumeCollectionDO(3L, "王五的简历.doc"));
        TimeUnit.SECONDS.sleep(2);
        log.info("提交任务到消息队列：{}",resumeCollectionDOList.stream().map(ResumeCollectionDO::getName).collect(Collectors.joining(",")));

        resumeCollectionDOList.forEach(resumeCollectionDO -> {
            // 通过第三方解析简历 获取解析id
            Long asyncParseId = asyncResumeParser.asyncParse(resumeCollectionDO);
            // 存入数据库操作

            // 把任务放进Redis 消息队列 交由消费者处理
            ResumeCollectionDTO resumeCollectionDTO = new ResumeCollectionDTO();
            BeanUtils.copyProperties(resumeCollectionDO,resumeCollectionDTO);
            resumeCollectionDTO.setAsyncPredictId(asyncParseId);

            redisService.pushQueue(RedisKeyConst.RESUME_PARSE_TASK_QUEUE,resumeCollectionDTO);
        });
    }
}
