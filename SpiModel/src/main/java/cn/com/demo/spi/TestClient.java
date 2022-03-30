package cn.com.demo.spi;

import cn.com.demo.spi.service.DemoService;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @Author: Yrenjie
 * @Date: 2022/3/30 10:03
 * @Description:
 * @Version 1.0
 */
public class TestClient {
    public static void main(String[] args) {
        ServiceLoader<DemoService> loader = ServiceLoader.load(DemoService.class);
        Iterator<DemoService> iterator = loader.iterator();
        while (iterator.hasNext()) {
            DemoService next = iterator.next();
            next.demoTest("spi create success");
        }
    }
}
