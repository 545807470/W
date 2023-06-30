package com.gxa.wyq.postprocessor;


import com.gxa.wyq.anno.Autowired;
import com.gxa.wyq.anno.Qualifier;
import com.gxa.wyq.anno.Value;
import com.gxa.wyq.myspring.BeansAware;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class ValueAndAutowiredPostProcessor implements BeanPostProcessor, BeansAware {

    private Map<String, Object> beans;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws Exception {
        return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws Exception {
        parseValue(bean);
        parseAutowired(bean);
        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }
    @Override
    public void setBeans(Map<String, Object> beans) {
        this.beans = beans;
    }
    private void parseValue(Object obj) {
        Field[] declaredFields = obj.getClass().getDeclaredFields();
        for (Field declaredField : declaredFields) {
            if (declaredField.isAnnotationPresent(Value.class)) {
                try {
                     //1.解析字段上的Value注解
                    Value v = declaredField.getAnnotation(Value.class);
                    String value = v.value();
                    //2.获取字段类型
                    Class<?> type = declaredField.getType();
                    String simpleName = type.getSimpleName();
                    //3.开启暴力反射注入
                    declaredField.setAccessible(true);
                    Object result = null;
                    //FIXME 策略模式优化if-else
                    if ("String".equals(simpleName)) {
                        result = String.valueOf(value);
                    } else if ("Integer".equals(simpleName)) {
                        result = Integer.valueOf(value);
                    } else if ("Double".equals(simpleName)) {
                        result = Double.valueOf(value);
                    } else if ("Boolean".equals(simpleName)) {
                        result = Boolean.valueOf(value);
                    } else if ("Float".equals(simpleName)) {
                        result = Float.valueOf(value);
                    }
                    declaredField.set(obj,result);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void parseAutowired(Object obj) {
        try {
            //1.解析内部属性，看看哪些方法或者字段上加了Autowired注解
            Field[] declaredFields = obj.getClass().getDeclaredFields();
            Method[] declaredMethods = obj.getClass().getDeclaredMethods();
            //2.如果是在字段上
            for (Field declaredField : declaredFields) {
                if (declaredField.isAnnotationPresent(Autowired.class)) {
                    //2.1.先按类型匹配
                    Class<?> type = declaredField.getType();
                    //开启暴力反射
                    declaredField.setAccessible(true);
                    if (type.isInterface()){
                        //如果当前注入的是个接口类型
                        //去容器中找实现该接口的实现类
                        //如果是接口先按类型去找
                        Object bean = getImplByInterface(declaredField);
                        declaredField.set(obj,bean);
                        continue;
                    }
                    //如果当前字段不是接口类型
                    //按类型匹配
                    Object beanByType = getBeanByType(declaredField);

                    //暴力反射注入
                    declaredField.set(obj,beanByType);
                }
            }
            //3.如果是在方法上的Autowired,比如setUser
            for (Method declaredMethod : declaredMethods) {
                if (declaredMethod.isAnnotationPresent(Autowired.class)) {
                    //在容器中找对应的类型，类型就是setUser中的User把首字母小写
                    String substring = declaredMethod.getName().substring(3);
                    substring = substring.substring(0, 1).toLowerCase() + substring.substring(1);

                    if (declaredMethod.getParameters().length != 1) {
                        //如果方法参数不是一个，set方法只有一个参数
                        throw new RuntimeException(obj.getClass().getSimpleName() + "类的" + declaredMethod.getName() + "无法注入");
                    }

                    Object typeObj = beans.get(substring);
                    if (typeObj != null) {
                        //注入
                        try {
                            declaredMethod.invoke(obj, typeObj);
                            continue;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    throw new RuntimeException(obj.getClass().getSimpleName() + "类的" + declaredMethod.getName() + "无法注入");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

//    /**
//     * 按类型/名称，在容器中找到对象后为字段注入赋值
//     * @param fieldName
//     * @param obj
//     * @param declaredField
//     * @param beanByType
//     */
//    private void invokeBySetMethod(String fieldName,Object obj,Field declaredField,Object beanByType){
//        try {
//            String methodName = "set" + fieldName.substring(0,1).toUpperCase() + fieldName.substring(1);
//            Method setMethod = obj.getClass().getMethod(methodName, declaredField.getType());
//            if (setMethod == null){
//                throw new RuntimeException(obj.getClass().getSimpleName() + "没有" + methodName + "方法");
//            }
//            setMethod.invoke(obj,beanByType);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }

    private Object getImplByInterface(Field field){
        Object result = null;
        List<String> beanNames = new ArrayList<>();
        for (Object bean : beans.values()) {
            Class<?>[] interfaces = bean.getClass().getInterfaces();
            for (Class<?> anInterface : interfaces) {
                if (field.getType().equals(anInterface)){
                    result = bean;
                    beanNames.add(bean.getClass().getSimpleName());
                }
            }
        }
        if (!field.isAnnotationPresent(Qualifier.class)){
            //当前字段上没有Qualifier并且找到的类型有多个
            if (beanNames.size() > 1){
                throw new RuntimeException("注入" + field.getName() + "失败,当前接口有多个实现类:" + beanNames);
            }
            //按类型注入，接口的实现类
            return result;
        }
        //代表当前字段有Qualifier注解
        Qualifier qualifier = field.getAnnotation(Qualifier.class);
        String value = qualifier.value();
        result = beans.get(value);
        if (result == null){
            throw new RuntimeException("当前容器中没有bean:" + value);
        }
        //按名称注入
        return result;
    }

    private Object getBeanByType(Field field){
        Collection values = beans.values();
        Object result = null;
        List<String> beanNames = new ArrayList<>();
        for (Object bean : values) {
            if (bean.getClass().equals(field.getType())) {
                //按类型匹配
                result = bean;
                beanNames.add(bean.getClass().getSimpleName());
            }
        }
        if (!field.isAnnotationPresent(Qualifier.class)){
            //当前字段上没有Qualifier并且找到的类型有多个
            if (beanNames.size() > 1){
                throw new RuntimeException("当前有多个类型:" + beanNames);
            }
            //按类型注入，接口的实现类
            return result;
        }
        Qualifier qualifier = field.getAnnotation(Qualifier.class);
        String value = qualifier.value();
        result = beans.get(value);
        if (result == null){
            throw new RuntimeException("当前容器中没有bean:" + value);
        }
        if (!result.getClass().equals(field.getType())){
            throw new RuntimeException("当前字段" + field.getName() +"注入的类型不一致!");
        }
        return result;
    }

    private Object getBeanByFieldName(String name){
        return beans.get(name);
    }


}
