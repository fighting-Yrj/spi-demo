package cn.com.demo.client.loader;

import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * @Author: Yrenjie
 * @Date: 2022/3/29 14:26
 * @Description:
 * @Version 1.0
 */
public class LocalClassLoader {

    private final static String FACTORIES_RESOURCE_LOCATION = "META-INF/spring.factories";

    public List<String> loadJar(String path) {
        List<String> result = new ArrayList<>();
        File file = new File(path);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                if (f.getName().endsWith(".jar")) {
                    result.add(f.getName());
                }
            }
        }
        return result;
    }

    public List<String> loadFactoryNames(String jarName, Class<?> factoryClass) {
        String factoryClassName = factoryClass.getName();
        ClassLoader classLoader = this.getClass().getClassLoader();
        try {
            Enumeration<URL> urls =
                    (classLoader != null ? classLoader.getResources(FACTORIES_RESOURCE_LOCATION) :
                            ClassLoader.getSystemResources(FACTORIES_RESOURCE_LOCATION));
            List<String> result = new ArrayList<String>();
            while (urls.hasMoreElements()) {
                // 一个url代表一个spring.factories文件
                URL url = urls.nextElement();
                // 加载所有的属性, 一般是 xxx接口=impl1,impl2 这种形式的
                Properties properties = PropertiesLoaderUtils.loadProperties(new UrlResource(url));
                // 根据接口名获取的类似"impl1,impl2"的字符串
                String factoryClassNames = properties.getProperty(jarName + "." + factoryClassName);
                // 以逗号分隔,转化成列表
                result.addAll(Arrays.asList(StringUtils.commaDelimitedListToStringArray(factoryClassNames)));
            }
            // 返回实现类名的列表
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ClassLoader loadJarBySpi(String path) throws MalformedURLException {
        URL[] jarUrl = new URL[] { new URL("jar:file:/" + path + "!/") };
        return new URLClassLoader(jarUrl);
    }
}
