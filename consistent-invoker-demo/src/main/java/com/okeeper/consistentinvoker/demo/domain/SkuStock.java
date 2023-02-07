package com.okeeper.consistentinvoker.demo.domain;

import lombok.Builder;
import lombok.Data;
import tk.mybatis.mapper.annotation.KeySql;
import tk.mybatis.mapper.code.IdentityDialect;

import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * @author zhangyue
 */
@Data
@Builder
@Table(name = "t_sku_stock")
public class SkuStock implements Serializable {

    @Id
    @KeySql(dialect = IdentityDialect.MYSQL)
    private Long id;

    private String name;

    private Long skuId;

    private Long stock;
}
