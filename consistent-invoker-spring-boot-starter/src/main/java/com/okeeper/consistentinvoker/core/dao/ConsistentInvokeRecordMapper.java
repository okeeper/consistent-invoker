package com.okeeper.consistentinvoker.core.dao;


import com.okeeper.consistentinvoker.core.model.ConsistentInvokeRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ConsistentInvokeRecordMapper {

     int insertSelective(ConsistentInvokeRecord record);

     ConsistentInvokeRecord selectByPrimaryKey(Long id);

     int updateByPrimaryKeySelective(ConsistentInvokeRecord record);

     List<ConsistentInvokeRecord> queryWaitInvokeListByNextRetryTimePageList(@Param("statusList") List<Integer> statusList, @Param("startRowNum")Integer startRowNum, @Param("pageSize") Integer pageSize);

    int delete(Long id);
}
