package cn.com.demo.spi.service.impl;

import cn.com.demo.spi.service.DemoService;

/**
 * @Author: Yrenjie
 * @Date: 2022/3/29 9:33
 * @Description:
 * @Version 1.0
 */
public class Demo1ServiceImpl implements DemoService {
    @Override
    public void demoTest(String message) {
        System.out.println("Demo1Service: " + message);
    }
}
