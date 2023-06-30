package com.gxa.wyq.myspring;

import java.util.Set;

public interface BeanNamesAware extends Aware{

    void setBeanNames(Set<String> beanNames);
}
