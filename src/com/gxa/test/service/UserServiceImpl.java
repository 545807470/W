package com.gxa.test.service;

import com.gxa.wyq.anno.Autowired;
import com.gxa.wyq.anno.Component;
import com.gxa.wyq.anno.Qualifier;
import com.gxa.test.dao.UserDao;
import com.gxa.test.dao.UserDaoImpl2;

@Component("userService")
public class UserServiceImpl implements UserService{

    @Autowired
    @Qualifier("userDao")
    private UserDao userDao;
    @Autowired
    @Qualifier("userDao2")
    private UserDaoImpl2 userDaoImpl;
    @Override
    public void add() {
        userDao.add();
    }
}
