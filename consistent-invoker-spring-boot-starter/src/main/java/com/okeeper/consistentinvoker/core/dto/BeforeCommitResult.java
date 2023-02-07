package com.okeeper.consistentinvoker.core.dto;

import com.okeeper.consistentinvoker.core.model.ConsistentInvokeRecord;
import lombok.Data;

/**
 * @author zhangyue
 */
@Data
public class BeforeCommitResult {
    private boolean added;
    private ConsistentInvokeRecord consistentInvokeRecord;
}
