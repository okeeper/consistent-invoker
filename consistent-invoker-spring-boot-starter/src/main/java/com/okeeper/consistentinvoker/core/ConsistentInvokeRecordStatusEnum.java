package com.okeeper.consistentinvoker.core;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * rpc请求记录的状态
 * @author zhangyue
 */
public enum ConsistentInvokeRecordStatusEnum {
    WAIT_INVOKE(0, "待请求"),
    FAIL(1, "请求失败"),
    SUCCESS(2, "请求成功"),
    ;


    private int type;
    private String desc;
    ConsistentInvokeRecordStatusEnum(int type, String desc){
        this.type = type;
        this.desc = desc;
    }

    public static ConsistentInvokeRecordStatusEnum of(Integer code) {
            return Optional.ofNullable(code).
                    flatMap(v -> Stream.of(ConsistentInvokeRecordStatusEnum.values()).
                            filter(e -> e.getType() == v).findFirst()).orElse(null);
     }

    public int getType() {
        return type;
    }
}
