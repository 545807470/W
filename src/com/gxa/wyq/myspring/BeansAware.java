package com.gxa.wyq.myspring;

import java.util.Map;

public interface BeansAware extends Aware{

    public void setBeans(Map<String,Object> beans);
}
