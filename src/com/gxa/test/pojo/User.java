package com.gxa.test.pojo;

import com.gxa.wyq.anno.Component;
import com.gxa.wyq.anno.Value;
import com.gxa.wyq.myspring.InitializingBean;

@Component
//@Scope(Scope.Type.PROTOTYPE)
public class User implements InitializingBean {

    @Value("1")
    private Integer id;

    @Value("张三")
    private String name;

    @Value("13")
    private Integer age;

    @Value("21.5")
    private Double price;

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", price=" + price +
                '}';
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("user完成了IOC和DI");
    }
}
