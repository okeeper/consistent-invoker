package com.okeeper.consistentinvoker.demo.component;

import com.okeeper.consistentinvoker.core.ConsistentInvoke;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author zhangyue
 */
@Component
@Slf4j
public class GoodsFacade {

    @ConsistentInvoke(key = "#sku")
    public Object offlineSkuNonTxAndSyncMode(Long sku){
        log.info("offlineSku:{}", sku);
        if(sku == null || sku < 0){
            throw new RuntimeException("call failed");
        }
        return "offlineSku success";
    }

    @ConsistentInvoke(key = "#sku")
    public void asyncOfflineSkuNoReturnType(Long sku){
        log.info("offlineSku:{}", sku);
        if(sku == null || sku < 0){
            throw new RuntimeException("call failed");
        }
    }

    @ConsistentInvoke(key = "#sku")
    public Object asyncOfflineSku(Long sku){
        log.info("offlineSku:{}", sku);
        if(sku == null || sku < 0){
            throw new RuntimeException("call failed");
        }
        return "offlineSku success";
    }

    @ConsistentInvoke(key = "#sku")
    public Object syncOfflineSku(Long sku){
        log.info("offlineSku:{}", sku);
        if(sku == null || sku < 0){
            throw new RuntimeException("call failed");
        }
        return "offlineSku success";
    }

    @ConsistentInvoke(key = "#sku")
    public Object offlineSkuWithoutTx(Long sku){
        log.info("offlineSku:{}", sku);
        if(sku == null || sku < 0){
            throw new RuntimeException("call failed");
        }
        return "offlineSku success";
    }

    @ConsistentInvoke(key = "#sku")
    public Object asyncOfflineSkuWithoutTx(Long sku){
        log.info("offlineSku:{}", sku);
        if(sku == null || sku < 0){
            throw new RuntimeException("call failed");
        }
        return "offlineSku success";
    }

    @ConsistentInvoke(key = "#sku")
    public Object offlineSkuWithDelay(Long sku){
        log.info("offlineSku:{}", sku);
        if(sku == null || sku < 0){
            throw new RuntimeException("call failed");
        }
        return "offlineSkuWithDelay success";
    }

    @ConsistentInvoke(key = "#sku")
    public Object offlineSkuWithDelayWithWrongPlaceholder(Long sku){
        log.info("offlineSku:{}", sku);
        if(sku == null || sku < 0){
            throw new RuntimeException("call failed");
        }
        return "offlineSkuWithDelay success";
    }

    @ConsistentInvoke(key = "#sku")
    public Object offlineSkuWithDelayWithPlaceholder(Long sku){
        log.info("offlineSku:{}", sku);
        if(sku == null || sku < 0){
            throw new RuntimeException("call failed");
        }
        return "offlineSkuWithDelay success";
    }

    @ConsistentInvoke(key = "#sku", async = false)
    public boolean offlineSkuSyncWithoutTxReturnRawType(Long sku){
        log.info("offlineSku:{}", sku);
        if(sku == null || sku < 0){
            throw new RuntimeException("call failed");
        }
        return true;
    }
}
