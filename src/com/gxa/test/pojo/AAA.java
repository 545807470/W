package com.gxa.test.pojo;

import com.gxa.wyq.anno.Autowired;
import com.gxa.wyq.anno.Component;
import com.gxa.wyq.anno.Value;

@Component
public class AAA {

    @Value("13")
    private Integer age;

    @Autowired
    private User user;

    @Override
    public String toString() {
        return "AAA{" +
                "age=" + age +
                ", user=" + user +
                '}';
    }
}
