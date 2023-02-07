
package com.okeeper.consistentinvoker.utils;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.ConvertUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ClassUtils
 * @author zhangyue1
 */
@Slf4j
public class ClassUtils {

	public static final Map<String, String> commonTypeMappings = new HashMap<>();
	static {
		commonTypeMappings.put("int","java.lang.Integer");
		commonTypeMappings.put("double","java.lang.Double");
		commonTypeMappings.put("float","java.lang.Float");
		commonTypeMappings.put("long","java.lang.Long");
		commonTypeMappings.put("short","java.lang.Short");
		commonTypeMappings.put("byte","java.lang.Byte");
		commonTypeMappings.put("boolean","java.lang.Boolean");
		commonTypeMappings.put("char","java.lang.Character");
	}

	/**
	 * 获取接口的返回值类型
	 * @param interfaceName
	 * @param methodName
	 * @param paramStringTypes
	 * @return
	 */
	public static Type getMethodReturnType(String interfaceName, String methodName, String []paramStringTypes) {
		try {
			// 创建类
			Class<?> class1 = Class.forName(interfaceName);
			// 获取所有的公共的方法
			Method[] methods = class1.getMethods();
			for (Method method : methods) {
				if (method.getName().equals(methodName) && method.getParameterTypes().length == paramStringTypes.length) {
					return method.getGenericReturnType();
				}
			}
		} catch (Exception e) {
			log.error("getMethodReturnType error.", e);
		}
		return null;
	}

	/**
	 * 获取接口的返回值类型
	 * @param methodName
	 * @param paramStringTypes
	 * @return
	 */
	public static Method getMethod(Class clazz, String methodName, String []paramStringTypes) {
		try {
			Class[] parameterTypeClasses = Stream.of(paramStringTypes).map(s -> {
				try {
					return findClassByClassName(s);
				} catch (ClassNotFoundException e) {
					log.error("class forName error.", e);
				}
				return null;
			}).collect(Collectors.toList()).toArray(new Class[]{});
			// 获取所有的公共的方法
			return clazz.getMethod(methodName, parameterTypeClasses);
		} catch (NoSuchMethodException e) {
			log.error("getMethod error.", e);
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public static String getUnionClassName(String clazzName) {
		return commonTypeMappings.getOrDefault(clazzName, clazzName);
	}

	public static Class findClassByClassName(String clazzName) throws ClassNotFoundException {
		switch (clazzName) {
			case "int": return int.class;
			case "double": return double.class;
			case "float": return float.class;
			case "long": return long.class;
			case "short": return short.class;
			case "byte": return byte.class;
			case "boolean": return boolean.class;
			case "char": return char.class;
			default: return Class.forName(clazzName);
		}
	}

	/**
	 * 将序列化之后的json string 转化成真实类型
	 * @param args
	 * @param argTypes
	 * @return 真实入参数组
	 */
	public static Object[] convertRealArgs(Object[] args, String[] argTypes) {
		Object[] rewriteArgs = new Object[args.length];
		try {
			for(int i=0; i<args.length; i++) {
				Class<?> toClass = Class.forName(getUnionClassName(argTypes[i]));
				if(args[i] instanceof JSON) {
					JSON json = (JSON) args[i];
					rewriteArgs[i] = json.toJavaObject(toClass);
				} else {
					rewriteArgs[i] = convertType(args[i], toClass);
				}
			}
			return rewriteArgs;
		}catch (ClassNotFoundException e) {
			log.error(e.getMessage() + ", args={}, argTypes={}", JSON.toJSONString(args), JSON.toJSONString(argTypes), e);
			throw new RuntimeException("convertRealArgs error.");
		}
	}



	/**
	 * 将序列化之后的json string 转化成真实类型
	 *
	 * @param args
	 * @param method
	 * @return 真实入参数组
	 */
	public static Object[] convertRealArgs(Object[] args, Method method) {
		if(args.length != method.getParameterTypes().length){
			throw new IllegalArgumentException("args length not eq parameterTypes length.");
		}
		Object[] rewriteArgs = new Object[args.length];
		Class []parameterTypeClass = method.getParameterTypes();
		Type []parameterTypes = method.getGenericParameterTypes();
		for (int i = 0; i < args.length; i++) {
			if(args[i] instanceof JSON) {
				rewriteArgs[i] = ((JSON)args[i]).toJavaObject(parameterTypes[i]);
			}else {
				rewriteArgs[i] = convertType(args[i], parameterTypeClass[i]);
			}
		}
		return rewriteArgs;
	}


	public static <T> T convertType(Object value, Class<T> targetClass) {
		if(Enum.class.isAssignableFrom(targetClass)) {
			List<T> enums = Arrays.asList(targetClass.getEnumConstants());
			for(T e : enums) {
				if (e.toString().equals(value)) {
					return e;
				}
			}
			return null;
		} if(value instanceof JSON) {
			return ((JSON) value).toJavaObject(targetClass);
		}else if(value instanceof String && ((String) value).startsWith("{")){
			return JSON.parseObject((String)value, targetClass);
		} else {
			return (T) ConvertUtils.convert(value, targetClass);
		}
	}

}
