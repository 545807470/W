package com.gxa.wyq.myspring;

import com.gxa.wyq.anno.Component;
import com.gxa.wyq.anno.ComponentScan;
import com.gxa.wyq.anno.Scope;
import com.gxa.wyq.postprocessor.BeanPostProcessor;
import com.gxa.wyq.postprocessor.ValueAndAutowiredPostProcessor;
import com.gxa.wyq.utils.MyClassScannerUtil;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AnnotationConfigApplicationContext implements ApplicationContext {

    //核心容器(单例)
    private static final Map<String, Object> beans = new ConcurrentHashMap<>();
    //存放bean定义的容器
    private static final Map<String, BeanDefinition> beanDefinitions = new ConcurrentHashMap<>();
    private static List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

    private static final String SINGLETON = "singleton";
    private static final String PROTOTYPE = "prototype";

    static {
        //走后门
        //FIXME 优化，如果多个后处理器，需要编写方法，优先进入容器并进行解析
        ValueAndAutowiredPostProcessor valueAndAutowiredPostProcessor = new ValueAndAutowiredPostProcessor();
        beanPostProcessors.add(valueAndAutowiredPostProcessor);
        BeanDefinition beanDefinition = new BeanDefinition();
        beanDefinition.setType(ValueAndAutowiredPostProcessor.class);
        beanDefinition.setScope("singleton");
        beanDefinitions.put("valueAndAutowiredPostProcessor",beanDefinition);
        beans.put("valueAndAutowiredPostProcessor",valueAndAutowiredPostProcessor);

        valueAndAutowiredPostProcessor.setBeans(beans);
    }

    public AnnotationConfigApplicationContext(Class clazz) {
        try {
            //把引导类加载成bean放入bean容器中
            Constructor<?> declaredConstructor = clazz.getDeclaredConstructor();
            declaredConstructor.setAccessible(true);
            Object instance = clazz.newInstance();
            //User   user:user对象
            beans.put(getSimpleName(clazz.getSimpleName()), instance);
            //包扫描
            classScan(clazz);
            //创建BeanDefinition对象
            createBeanDefinition();
            //IOC创建单例对象
            IOC();
            //单例属性注入
//            DI();
            //接口监测
            checkBeanInterface();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 创建BeanDefinitions中的单例对象
     */
    private void IOC() {
        try {
            for (Map.Entry<String, BeanDefinition> beanDefinitionEntry : beanDefinitions.entrySet()) {
                BeanDefinition beanDefinition = beanDefinitionEntry.getValue();
                if (SINGLETON.equals(beanDefinition.getScope()) && !beanDefinition.getType().equals(ValueAndAutowiredPostProcessor.class)){
                    Object bean = beanDefinition.getType().newInstance();

                    //存入单例池
                    beans.put(beanDefinitionEntry.getKey(),bean);
                }
            }
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 包扫描方法
     * @param clazz
     */
    private void classScan(Class clazz){
        if (clazz.isAnnotationPresent(ComponentScan.class)){
            //拿到注解
            ComponentScan annotation = (ComponentScan) clazz.getAnnotation(ComponentScan.class);
            //获取扫描的路径
            String[] packagePath = annotation.value();
            //开始扫描
            for (String path : packagePath) {
                try {
                    MyClassScannerUtil.classScanner(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * 检测单例生命周期接口
     */
    private void checkBeanInterface(){
        try {
            for (Map.Entry<String, Object> stringObjectEntry : beans.entrySet()) {
                Object bean = stringObjectEntry.getValue();
                if (bean.getClass().equals(ValueAndAutowiredPostProcessor.class)){
                    //走后门的不用再处理
                    continue;
                }
                //1.检测是否实现Aware接口
                if (bean instanceof BeanNamesAware){
                    ((BeanNamesAware) bean).setBeanNames(beans.keySet());
                }
                if (bean instanceof BeansAware){
                    ((BeansAware)bean).setBeans(beans);
                }
                //2.执行初始化前
                for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
                    bean = beanPostProcessor.postProcessBeforeInitialization(bean,stringObjectEntry.getKey());
                }
                //3.检测bean是否实现InitializingBean接口
                if (bean instanceof InitializingBean) {
                    try {
                        ((InitializingBean) bean).afterPropertiesSet();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                //4.执行后置处理
                for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
                    bean = beanPostProcessor.postProcessAfterInitialization(bean,stringObjectEntry.getKey());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据扫描结果创建bean定义对象
     */
    private void createBeanDefinition(){
        //获取扫描结果
        List<Class<?>> classList = MyClassScannerUtil.getClassList();
        if (classList.size() == 0){
            //没有任何扫描结果，防止空指针
            return;
        }
        //根据扫描结果创建bean定义对象，并放入bean定义容器内
        for (Class<?> clazz : classList) {
            if (clazz.isAnnotationPresent(Component.class)){
                //加了@Component注解的类才会被管理
                BeanDefinition beanDefinition = new BeanDefinition();
                //获取map的key
                Component component = clazz.getAnnotation(Component.class);
                String name = component.value();
                if ("".equals(name)){
                    //赠送首字母小写的名称
                    name = getSimpleName(clazz.getSimpleName());
                }
                //类型
                beanDefinition.setType(clazz);
                //判断是否有Scope注解，没有则默认单例，如果有并且内部属性为prototype则代表非单例
                if (clazz.isAnnotationPresent(Scope.class)){
                    Scope scope = clazz.getAnnotation(Scope.class);
                    String value = scope.value();
                    beanDefinition.setScope(value);
                    beanDefinitions.put(name,beanDefinition);
                    continue;
                }
                //默认单例
                beanDefinition.setScope(SINGLETON);
                //FIXME 是否懒加载
                //放入bean定义集合
                beanDefinitions.put(name,beanDefinition);

                //判断当前clazz是否实现生命周期接口
                if (BeanPostProcessor.class.isAssignableFrom(clazz)){
                    try {
                        beanPostProcessors.add((BeanPostProcessor) clazz.newInstance());
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }


    /**
     * 非单例对象注入属性
     */
    private Object prototypeDi(String name) {
        try {
            for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
                //一定是多例
                BeanDefinition beanDefinition = beanDefinitions.get(name);
                //创建对象
                Object prototypeBean = beanDefinition.getType().newInstance();
                if (prototypeBean instanceof BeanNamesAware){
                    ((BeanNamesAware) prototypeBean).setBeanNames(beans.keySet());
                }
                if (prototypeBean instanceof BeansAware){
                    ((BeansAware)prototypeBean).setBeans(beans);
                }
                //执行注解解析
                prototypeBean = beanPostProcessor.postProcessAfterInitialization(prototypeBean,name);
                //返回结果
                return prototypeBean;
            }
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }


    /*
        把首字母大写的简单名称，改为首字母小写
     */
    private String getSimpleName(String className) {
        className = className.substring(0, 1).toLowerCase() + className.substring(1);

        return className;
    }

    @Override
    public Object getBean(String name) {
        //1.获取bean定义对象
        BeanDefinition beanDefinition = beanDefinitions.get(name);
        //2.判断是否单例
        if (SINGLETON.equals(beanDefinition.getScope())){
            //是单例直接从单例池返回
            return beans.get(name);
        }
        //3.不是单例则创建对象并进行依赖注入返回
        return prototypeDi(name);
    }
    @Override
    public <T> T getBean(String name, Class<T> clazz) {
        //1.获取bean定义对象
        BeanDefinition beanDefinition = beanDefinitions.get(name);
        //2.判断是否单例
        if (SINGLETON.equals(beanDefinition.getScope())){
            //是单例直接从单例池返回
            return (T) beans.get(name);
        }
        //3.不是单例则创建对象并进行依赖注入返回
        return (T) prototypeDi(name);
    }
}
