package com.okeeper.consistentinvoker.demo.service;

import com.okeeper.consistentinvoker.demo.component.GoodsFacade;
import com.okeeper.consistentinvoker.demo.dao.SkuStockMapper;
import com.okeeper.consistentinvoker.demo.domain.SkuStock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author zhangyue
 */
@Service
@Slf4j
public class SkuService {

    @Resource
    SkuStockMapper skuStockMapper;

    @Autowired
    GoodsFacade goodsFacade;

    @Autowired
    NestedService nestedService;

    /**
     * name empty throw ex
     * @param skuStock
     */
    public void updateStockAndOfflineSku_withNoTransaction(SkuStock skuStock){
        //skuStockMapper.insert(skuStock);
        goodsFacade.asyncOfflineSkuNoReturnType(skuStock.getSkuId());
    }


    /**
     * name empty throw ex
     * @param skuStock
     */
    @Transactional
    public void updateStockAndOfflineSku(SkuStock skuStock){
        goodsFacade.asyncOfflineSku(skuStock.getSkuId());
        skuStockMapper.insert(skuStock);

    }

    @Transactional
    public void updateStockAndSyncOfflineSku(SkuStock skuStock){
        skuStockMapper.insert(skuStock);
        goodsFacade.syncOfflineSku(skuStock.getSkuId());
    }

    @Transactional
    public void updateStockAndOfflineSkuWithDelay(SkuStock skuStock){
        skuStockMapper.insert(skuStock);
        goodsFacade.offlineSkuWithDelay(skuStock.getSkuId());
    }

    @Transactional
    public void updateStockAndOfflineSkuWithDelayWithWrongPlaceholder(SkuStock skuStock){
        skuStockMapper.insert(skuStock);
        goodsFacade.offlineSkuWithDelayWithWrongPlaceholder(skuStock.getSkuId());
    }

    @Transactional
    public void updateStockAndOfflineSkuWithDelayWithPlaceholder(SkuStock skuStock){
        skuStockMapper.insert(skuStock);
        goodsFacade.offlineSkuWithDelayWithPlaceholder(skuStock.getSkuId());
    }

    @Transactional
    public void updateStockAndOfflineSkuNested(SkuStock skuStock){
        skuStockMapper.insert(skuStock);
        nestedService.nestedOfflineSku(skuStock.getSkuId());
    }

    @Transactional
    public void updateStockAndOfflineSkuNestedAsync(SkuStock skuStock){
        skuStockMapper.insert(skuStock);
        nestedService.nestedOfflineSkuAsync(skuStock.getSkuId());
    }

    @Transactional
    public void syncWithoutTxUsingResult(SkuStock skuStock) {
        boolean result = goodsFacade.offlineSkuSyncWithoutTxReturnRawType(skuStock.getSkuId());
        log.info("syncWithoutTxUsingResult:{}", result);
    }
}
