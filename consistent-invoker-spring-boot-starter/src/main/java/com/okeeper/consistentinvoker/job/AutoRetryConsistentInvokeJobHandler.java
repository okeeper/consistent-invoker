package com.okeeper.consistentinvoker.job;

import com.okeeper.consistentinvoker.core.ConsistentInvokeHandler;
import com.okeeper.consistentinvoker.core.ConsistentInvokeRecordStatusEnum;
import com.okeeper.consistentinvoker.config.ConsistentInvokerConfig;
import com.okeeper.consistentinvoker.core.model.ConsistentInvokeRecord;
import com.okeeper.consistentinvoker.core.service.ConsistentInvokeRecordService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.log.XxlJobLogger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * 从数据库中取出来进行异步方法重试
 *
 * @author zhangyue
 */
@Slf4j
public class AutoRetryConsistentInvokeJobHandler extends IJobHandler {

    @Autowired
    private ConsistentInvokeRecordService consistentInvokeRecordService;

    @Autowired
    private ConsistentInvokerConfig consistentInvokerConfig;

    @Autowired
    private ConsistentInvokeHandler consistentInvokeHandler;

    @Override
    public ReturnT<String> execute(String params) {
      try {
          this.info("start handle AutoRetryAsyncInvokeHandler..., params={}", params);

          //手动重试
          if(StringUtils.isNotEmpty(params)) {
              AtomicBoolean forceInvoke = new AtomicBoolean(false);
              Stream.of(params.split(",")).forEach(id-> {
                  if(id.equals("force")) {
                      forceInvoke.set(true);
                      return;
                  }

                  Long invokeId = Long.valueOf(id);
                  ConsistentInvokeRecord consistentInvokeRecord = consistentInvokeRecordService.queryById(invokeId);
                  if(Objects.isNull(consistentInvokeRecord)) {
                      info("consistentInvokeRecord not exists for id {}", id);
                  }
                  consistentInvokeRecord.setForce(forceInvoke.get());
                  consistentInvokeHandler.invokeByRpc(consistentInvokeRecord);
              });
              return ReturnT.SUCCESS;
          }

          doExecuteByPage();

          this.info("handle AutoRetryAsyncInvokeHandler success");
          return ReturnT.SUCCESS;
      }catch (Exception e) {
          log.error("AutoRetryConsistentInvokeJobHandler error.", e);
          throw e;
      }
    }

    void doExecuteByPage() {
        //定时任务重试
        int pageNo = 1;
        int pageSize = consistentInvokerConfig.getRetryBatchSize();

        List<Integer> statusList = Arrays.asList(ConsistentInvokeRecordStatusEnum.WAIT_INVOKE.getType(),
                ConsistentInvokeRecordStatusEnum.FAIL.getType());
        do {
            List<ConsistentInvokeRecord> consistentInvokeRecords = consistentInvokeRecordService.queryWaitInvokeListByNextRetryTimePageList(statusList, pageNo, pageSize);
            if(!CollectionUtils.isEmpty(consistentInvokeRecords)) {
                consistentInvokeRecords.forEach(consistentInvokeRecord -> {
                    try {
                        consistentInvokeHandler.invokeByRpc(consistentInvokeRecord);
                    } catch (Exception e) {
                        log.error("invokeByRpc error.", e);
                    }
                });
                pageNo ++;
            }else {
                break;
            }
        } while (true);
    }


    public void info(String var1, Object... var2){
        log.info(var1,var2);
        XxlJobLogger.log(var1,var2);
    }

    public void info(String var1){
        log.info(var1);
        XxlJobLogger.log(var1);
    }
}

