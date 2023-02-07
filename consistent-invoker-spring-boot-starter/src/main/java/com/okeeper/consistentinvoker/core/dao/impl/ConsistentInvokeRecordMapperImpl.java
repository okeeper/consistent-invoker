package com.okeeper.consistentinvoker.core.dao.impl;

import com.okeeper.consistentinvoker.core.dao.ConsistentInvokeRecordMapper;
import com.okeeper.consistentinvoker.core.model.ConsistentInvokeRecord;
import com.google.common.collect.Maps;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * @author yue
 */
public class ConsistentInvokeRecordMapperImpl implements ConsistentInvokeRecordMapper {

    private SqlSessionTemplate sqlSessionTemplate;

    public ConsistentInvokeRecordMapperImpl(DataSource dataSource, Resource resource) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        sqlSessionFactoryBean.setMapperLocations(resource);
        sqlSessionFactoryBean.setTypeAliases(ConsistentInvokeRecord.class);
        SqlSessionFactory sqlSessionFactory = sqlSessionFactoryBean.getObject();
        sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory);
    }


    @Override
    public int insertSelective(ConsistentInvokeRecord record) {
        return sqlSessionTemplate.insert("com.okeeoer.consistentinvoker.core.dao.ConsistentInvokeRecordMapper.insertSelective", record);
    }

    @Override
    public ConsistentInvokeRecord selectByPrimaryKey(Long id) {
        return sqlSessionTemplate.selectOne("com.okeeoer.consistentinvoker.core.dao.ConsistentInvokeRecordMapper.selectByPrimaryKey", id);
    }

    @Override
    public int updateByPrimaryKeySelective(ConsistentInvokeRecord record) {
        return sqlSessionTemplate.update("com.okeeoer.consistentinvoker.core.dao.ConsistentInvokeRecordMapper.updateByPrimaryKeySelective", record);
    }

//    @Override
//    public long queryWaitInvokeListByNextRetryTimePageListCount(List<Integer> statusList) {
//        return sqlSessionTemplate.selectOne("com.okeeoer.consistentinvoker.core.dao.ConsistentInvokeRecordMapper.queryWaitInvokeListByNextRetryTimePageListCount", statusList);
//    }

    @Override
    public List<ConsistentInvokeRecord> queryWaitInvokeListByNextRetryTimePageList(List<Integer> statusList, Integer startRowNum, Integer pageSize) {
        Map<String,Object> paramMap = Maps.newHashMap();
        paramMap.put("statusList", statusList);
        paramMap.put("startRowNum", startRowNum);
        paramMap.put("pageSize", pageSize);
        return sqlSessionTemplate.selectList("com.okeeoer.consistentinvoker.core.dao.ConsistentInvokeRecordMapper.queryWaitInvokeListByNextRetryTimePageList", paramMap);
    }

    @Override
    public int delete(Long id) {
        return sqlSessionTemplate.delete("com.okeeoer.consistentinvoker.core.dao.ConsistentInvokeRecordMapper.delete", id);
    }
}
