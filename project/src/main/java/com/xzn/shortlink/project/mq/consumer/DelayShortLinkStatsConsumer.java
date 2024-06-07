package com.xzn.shortlink.project.mq.consumer;

import static com.xzn.shortlink.project.common.constant.RedisConstantKey.DELAY_QUEUE_STATS_KEY;

import com.xzn.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.xzn.shortlink.project.service.ShortLinkService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
/**
 * @author Nruonan
 * @description 延迟记录短链接统计组件
 */
@Component
@Deprecated
@RequiredArgsConstructor
public class DelayShortLinkStatsConsumer implements InitializingBean {
    private final RedissonClient redissonClient;
    private final ShortLinkService shortLinkService;
    private void onMessage() {
        Executors.newSingleThreadExecutor(
            r ->{
                Thread thread = new Thread(r);
                thread.setName("delay_short-link-stats-consumer");
                thread.setDaemon(Boolean.TRUE);
                return thread;
            }
        ).execute(() ->{
            RBlockingDeque<ShortLinkStatsRecordDTO> blockingDeque = redissonClient.getBlockingDeque(DELAY_QUEUE_STATS_KEY);
            RDelayedQueue<ShortLinkStatsRecordDTO> delayedQueue = redissonClient.getDelayedQueue(blockingDeque);
            for (; ;){
                try{
                    ShortLinkStatsRecordDTO recordDTO = delayedQueue.poll();
                    if (recordDTO != null){
                        shortLinkService.shortLinkStats(null, null, recordDTO);
                        continue;
                    }
                    LockSupport.parkUntil(500);
                }catch (Throwable i){

                }
            }
        });
    }
    @Override
    public void afterPropertiesSet() throws Exception {
        onMessage();
    }


}
