package com.gxa.test.pojo;

import com.gxa.wyq.anno.Autowired;
import com.gxa.wyq.anno.Component;
import com.gxa.wyq.anno.Scope;
import com.gxa.wyq.anno.Value;
import com.gxa.wyq.myspring.BeanNamesAware;

import java.util.Set;

@Component
@Scope("prototype")
public class Person implements BeanNamesAware {
    @Autowired
    private User user;

    @Value("32.5")
    private Double money;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Double getMoney() {
        return money;
    }

    public void setMoney(Double money) {
        this.money = money;
    }

    @Override
    public String toString() {
        return "Person{" +
                "user=" + user +
                ", money=" + money +
                ", beanNames=" + beanNames +
                '}';
    }

    private Set<String> beanNames;
    @Override
    public void setBeanNames(Set<String> beanNames) {
        this.beanNames = beanNames;
    }
}
