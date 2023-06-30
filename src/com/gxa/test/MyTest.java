package com.gxa.test;

import com.gxa.test.pojo.AAA;
import com.gxa.test.pojo.Person;
import com.gxa.wyq.myspring.AnnotationConfigApplicationContext;
import com.gxa.wyq.myspring.ApplicationContext;
import com.gxa.test.config.MySpring;
import com.gxa.test.service.UserService;

public class MyTest {

    public static void main(String[] args) throws NoSuchMethodException {
        ApplicationContext context = new AnnotationConfigApplicationContext(MySpring.class);
        UserService userService = context.getBean("userService", UserService.class);
        Person person1 = context.getBean("person", Person.class);
        Person person2 = context.getBean("person", Person.class);
        AAA aAA = context.getBean("aAA", AAA.class);
        System.out.println(aAA);
        userService.add();

        System.out.println(person1);
        System.out.println(person2);

    }
}
