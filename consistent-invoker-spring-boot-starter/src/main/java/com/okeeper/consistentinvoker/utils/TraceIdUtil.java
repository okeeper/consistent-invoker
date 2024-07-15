package com.okeeper.consistentinvoker.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.slf4j.MDC;

/**
 * 获取traceId
 * @author: zhangyue
 */
public class TraceIdUtil {

    private static final String TRACE_ID = "tid";
    private static final String TRACE_ID_EMPTY = "N/A";

    /**
     * 把traceId放到MDC
     *
     * @param traceId
     */
    public static void putTraceIdToMDC(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    /**
     * 从MDC获取traceId
     *
     * @return
     */
    public static String getTraceIdFromMDC() {
        String traceId = MDC.get(TRACE_ID);
       return TRACE_ID_EMPTY.equals(traceId) ? null : traceId;
    }

    /**
     * 获取traceId
     * @return
     */
    public static String getTraceId() {
        return StringUtils.isEmpty(TraceContext.traceId()) ? getTraceIdFromMDC() : TraceContext.traceId();
    }
}
