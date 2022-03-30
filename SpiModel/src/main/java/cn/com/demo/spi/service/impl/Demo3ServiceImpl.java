package cn.com.demo.spi.service.impl;

import cn.com.demo.spi.service.DemoService;

/**
 * @Author: Yrenjie
 * @Date: 2022/3/29 10:29
 * @Description:
 * @Version 1.0
 */
public class Demo3ServiceImpl implements DemoService {
    @Override
    public void demoTest(String message) {
        System.out.println("Demo3Service: " + message);
    }
}
