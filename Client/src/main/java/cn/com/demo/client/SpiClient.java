package cn.com.demo.client;

import cn.com.demo.client.loader.LocalClassLoader;
import cn.com.demo.spi.service.DemoService;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @Author: Yrenjie
 * @Date: 2022/3/29 9:41
 * @Description:
 * @Version 1.0
 */
public class SpiClient {
    public static void main(String[] args) {
        //ServiceLoader<DemoService> loader = ServiceLoader.load(DemoService.class);
        //Iterator<DemoService> iterator = loader.iterator();
        //while (iterator.hasNext()) {
        //    DemoService next = iterator.next();
        //    next.demoTest("success spi");
        //}
        test();
    }

    public static void test() {
        LocalClassLoader classLoader = new LocalClassLoader();
        try {
            String path = "D:/Codes/temp/Demo/Client/plugins";
            List<String> jars = classLoader.loadJar(path);
            for (String jar : jars) {
                ClassLoader loader = classLoader.loadJarBySpi(path + File.separator +jar);
                ServiceLoader<DemoService> serviceLoader = ServiceLoader.load(DemoService.class, loader);
                Iterator<DemoService> iterator = serviceLoader.iterator();
                while (iterator.hasNext()) {
                    DemoService next = iterator.next();
                    next.demoTest("load success");
                }
                //for (String name : names) {
                //    Class aClass = loader.loadClass(name);
                //    DemoService service = (DemoService) aClass.newInstance();
                //    service.demoTest("load success");
                //}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
