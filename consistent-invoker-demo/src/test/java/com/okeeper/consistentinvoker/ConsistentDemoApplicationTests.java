package com.okeeper.consistentinvoker;

import com.okeeper.consistentinvoker.core.ConsistentInvokeHandler;
import com.okeeper.consistentinvoker.core.ConsistentInvokeRecordStatusEnum;
import com.okeeper.consistentinvoker.core.model.ConsistentInvokeRecord;
import com.okeeper.consistentinvoker.core.service.ConsistentInvokeRecordService;
import com.okeeper.consistentinvoker.demo.component.GoodsFacade;
import com.okeeper.consistentinvoker.demo.dao.SkuStockMapper;
import com.okeeper.consistentinvoker.demo.domain.SkuStock;
import com.okeeper.consistentinvoker.demo.service.SkuService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

//@Transactional
//@Rollback
@Slf4j
//@ActiveProfiles(profiles = "ut")
@SpringBootTest(classes = ConsistentDemoApplication.class, webEnvironment= SpringBootTest.WebEnvironment.NONE)
class ConsistentDemoApplicationTests {

    @Resource
    SkuStockMapper skuStockMapper;

    @Autowired
    SkuService skuService;

    @Autowired
    GoodsFacade goodsFacade;

    @Autowired
    private ConsistentInvokeRecordService consistentInvokeRecordService;

    @Autowired
    private ConsistentInvokeHandler consistentInvokeHandler;


    @Test
    public void testCreateSkuStock() {
        SkuStock skuStock = SkuStock.builder()
                .name("testaaa")
                .skuId(1L)
                .stock(100L)
                .build();
        skuStockMapper.insert(skuStock);
    }

    @Test
    public void testUpdateStockAndOfflineSku_withNoTransaction(){
        SkuStock skuStock = SkuStock.builder()
                .name("abc")
                .skuId(-1L)
                .stock(100L)
                .build();
        skuService.updateStockAndOfflineSku_withNoTransaction(skuStock);
    }

    @Test
    public void testBizSuccess(){
        SkuStock skuStock = SkuStock.builder()
                .name("ioioioioio")
                .skuId(-1L)
                .stock(100L)
                .build();
        skuService.updateStockAndOfflineSku(skuStock);
    }

    @Test
    public void testBizSuccessWithMDC() {
        SkuStock skuStock = SkuStock.builder()
                .name("abc")
                .skuId(1L)
                .stock(100L)
                .build();
        skuService.updateStockAndOfflineSku(skuStock);
    }

    @Test
    public void testBizSuccessWithDelay() throws InterruptedException {
        SkuStock skuStock = SkuStock.builder()
                .name("abc")
                .skuId(1L)
                .stock(100L)
                .build();
        skuService.updateStockAndOfflineSkuWithDelay(skuStock);
        //TimeUnit.SECONDS.sleep(60);
    }

    @Test
    public void testBizSuccessWithDelayWithWrongPlaceholder() throws InterruptedException {
        SkuStock skuStock = SkuStock.builder()
                .name("abc")
                .skuId(1L)
                .stock(100L)
                .build();
        skuService.updateStockAndOfflineSkuWithDelayWithWrongPlaceholder(skuStock);
    }

    @Test
    public void testBizSuccessWithDelayWithPlaceholder() throws InterruptedException {
        SkuStock skuStock = SkuStock.builder()
                .name("abc")
                .skuId(1L)
                .stock(100L)
                .build();
        skuService.updateStockAndOfflineSkuWithDelayWithPlaceholder(skuStock);
    }

    @Test
    public void testBizFail(){
        SkuStock skuStock = SkuStock.builder()
                .name("1234567890") //mock fail
                .skuId(1L)
                .stock(100L)
                .build();
        skuService.updateStockAndOfflineSku(skuStock);
    }

    @Test
    public void testBizSuccessEventFail() throws InterruptedException {
        SkuStock skuStock = SkuStock.builder()
                .name("abc")
                .skuId(-1L) //mock fail
                .stock(5L)
                .build();
        skuService.updateStockAndOfflineSku(skuStock);
    }

    @Test
    public void testBizSuccessEventFailSyncWithTxMode() throws InterruptedException {
        SkuStock skuStock = SkuStock.builder()
                .name("abc")
                .skuId(-1L) //mock fail
                .stock(5L)
                .build();
        skuService.updateStockAndSyncOfflineSku(skuStock);
    }

    @Test
    public void testNonTransactionAndSyncModeReturnResult() {
        Object result = goodsFacade.offlineSkuNonTxAndSyncMode(1L);
        System.out.println("result:"+result);
    }

    @Test
    public void testNestedErrorWarning() {
        SkuStock skuStock = SkuStock.builder()
                .name("abc")
                .skuId(1L)
                .stock(100L)
                .build();
        skuService.updateStockAndOfflineSkuNested(skuStock);
    }

    @Test
    public void testNestedErrorWarningAsync() {
        SkuStock skuStock = SkuStock.builder()
                .name("abc")
                .skuId(1L)
                .stock(100L)
                .build();
        skuService.updateStockAndOfflineSkuNestedAsync(skuStock);
    }

    /**
     * 测试有返回值的同步请求
     */
    @Test
    public void testUsingSyncWithoutTxResult() {
        SkuStock skuStock = SkuStock.builder()
                .name("abc")
                .skuId(1L)
                .stock(100L)
                .build();
        skuService.syncWithoutTxUsingResult(skuStock);
    }

    /**
     * for 1.5.0-RELEASE
     * org.springframework.aop.AopInvocationException: Null return value from advice does not match primitive return type for: public boolean com.example.component.GoodsFacade.offlineSkuSyncWithoutTxReturnRawType(java.lang.Long)
     *
     * from 1.5.1
     * direct throw biz exception
     */
    @Test
    public void testUsingSyncWithoutTxResultThrowException() {
        SkuStock skuStock = SkuStock.builder()
                .name("abc")
                .skuId(-1L) //for mock throw exception
                .stock(100L)
                .build();
        skuService.syncWithoutTxUsingResult(skuStock);
    }

    @Test
    public void testRetry() {
        ConsistentInvokeRecord consistentInvokeRecord = consistentInvokeRecordService.queryById(41L);
        consistentInvokeHandler.invokeByRpc(consistentInvokeRecord);
    }


    @Test
    public void queryWaitInvokeListByNextRetryTimePageList() {
        List<ConsistentInvokeRecord> listByNextRetryTimePageList = consistentInvokeRecordService.queryWaitInvokeListByNextRetryTimePageList(Arrays.asList(ConsistentInvokeRecordStatusEnum.FAIL.getType(), ConsistentInvokeRecordStatusEnum.WAIT_INVOKE.getType()), 1, 10);
        log.info(">>>>>" + JSON.toJSONString(listByNextRetryTimePageList));
    }

}
