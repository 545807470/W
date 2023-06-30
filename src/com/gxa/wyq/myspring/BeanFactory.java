package com.gxa.wyq.myspring;

public interface BeanFactory {

    public Object getBean(String name);

    public <T> T getBean(String name,Class<T> clazz);
}
