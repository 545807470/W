package com.gxa.test.dao;

import com.gxa.wyq.anno.Component;

@Component("userDao")
public class UserDaoImpl implements UserDao{
    @Override
    public void add() {
        System.out.println("添加用户 1...");
    }
}
