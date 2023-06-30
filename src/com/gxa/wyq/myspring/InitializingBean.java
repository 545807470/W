package com.gxa.wyq.myspring;

public interface InitializingBean {

    //在DI注入完之后
    void afterPropertiesSet() throws Exception;
}
