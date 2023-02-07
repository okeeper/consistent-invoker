package com.okeeper.consistentinvoker.core;

import lombok.Data;

/**
 * @author zhangyue
 */
@Data
public class ConsistentInvokeContext {
    private Long asyncInvokeRecordId;
    private String key;
}
