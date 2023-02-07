package com.okeeper.consistentinvoker.demo.service;

import com.okeeper.consistentinvoker.core.ConsistentInvoke;
import com.okeeper.consistentinvoker.demo.component.GoodsFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author zhangyue
 */
@Service
@Slf4j
public class NestedService {

    @Autowired
    GoodsFacade goodsFacade;

    @ConsistentInvoke(key = "#sku")
    public void nestedOfflineSku(Long sku) {
        log.info("execute nestedConsistent");

        //note default async, there is not transaction in async thread
        goodsFacade.offlineSkuWithoutTx(sku);
    }

    @ConsistentInvoke(key = "#sku")
    public void nestedOfflineSkuAsync(Long sku) {
        log.info("execute nestedConsistent");

        //note default async, there is not transaction in async thread
        goodsFacade.asyncOfflineSkuWithoutTx(sku);
    }
}
