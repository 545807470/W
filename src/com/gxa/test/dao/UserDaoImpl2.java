package com.gxa.test.dao;

import com.gxa.wyq.anno.Component;

@Component("userDao2")
public class UserDaoImpl2 implements UserDao{
    @Override
    public void add() {
        System.out.println("添加用户 2...");
    }
}
