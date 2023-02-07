package com.okeeper.consistentinvoker.utils;

import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * spEL spring表达式
 * @author zhangyue
 */
public class SpelUtils {

    /**
     * 参数发现器
     */
    private static final ParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();
    /**
     * Express语法解析器
     */
    private static final ExpressionParser PARSER = new SpelExpressionParser();

    public static String getMethodParameterValue(Method method, Object []arguments, String key) {
        if(StringUtils.isEmpty(key)) {
            return null;
        }
        EvaluationContext context = new MethodBasedEvaluationContext(null, method, arguments, NAME_DISCOVERER);
        final Object value = PARSER.parseExpression(key).getValue(context);
        return value == null ? null : value.toString();
    }
}
