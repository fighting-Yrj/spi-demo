---
title: Spi机制
date: 2022-03-30
---

## SPI机制实现原理

`JDK`中`ServiceLoader<S>`方法具体实现

```java
//查找配置文件的目录
private static final String PREFIX = "META-INF/services/";

//表示要被加载的服务的类或接口
private final Class<S> service;

//这个ClassLoader用来定位，加载，实例化服务提供者
private final ClassLoader loader;

// 缓存已经被实例化的服务提供者，按照实例化的顺序存储
private LinkedHashMap<String,S> providers = new LinkedHashMap<>();

// 迭代器
private LazyIterator lookupIterator;

//重新加载，就相当于重新创建ServiceLoader了，用于新的服务提供者安装到正在运行的Java虚拟机中的情况。
public void reload() {
    //清空缓存中所有已实例化的服务提供者
    providers.clear();
    //新建一个迭代器，该迭代器会从头查找和实例化服务提供者
    lookupIterator = new LazyIterator(service, loader);
}

//私有构造器
//使用指定的类加载器和服务创建服务加载器
//如果没有指定类加载器，使用系统类加载器，就是应用类加载器。
private ServiceLoader(Class<S> svc, ClassLoader cl) {
    service = Objects.requireNonNull(svc, "Service interface cannot be null");
    loader = (cl == null) ? ClassLoader.getSystemClassLoader() : cl;
    acc = (System.getSecurityManager() != null) ? AccessController.getContext() : null;
    reload();
}

//服务提供者查找的迭代器
private class LazyIterator
    implements Iterator<S>
{

    Class<S> service;//服务提供者接口
    ClassLoader loader;//类加载器
    Enumeration<URL> configs = null;//保存实现类的url
    Iterator<String> pending = null;//保存实现类的全名
    String nextName = null;//迭代器中下一个实现类的全名

    private LazyIterator(Class<S> service, ClassLoader loader) {
        this.service = service;
        this.loader = loader;
    }

    private boolean hasNextService() {
        if (nextName != null) {
            return true;
        }
        if (configs == null) {
            try {
                String fullName = PREFIX + service.getName();
                if (loader == null)
                    configs = ClassLoader.getSystemResources(fullName);
                else
                    configs = loader.getResources(fullName);
            }
        }
        while ((pending == null) || !pending.hasNext()) {
            if (!configs.hasMoreElements()) {
                return false;
            }
            pending = parse(service, configs.nextElement());
        }
        nextName = pending.next();
        return true;
    }

    private S nextService() {
        if (!hasNextService())
            throw new NoSuchElementException();
        String cn = nextName;
        nextName = null;
        Class<?> c = null;
        try {
            c = Class.forName(cn, false, loader);
        }
        if (!service.isAssignableFrom(c)) {
            fail(service, "Provider " + cn  + " not a subtype");
        }
        try {
            S p = service.cast(c.newInstance());
            providers.put(cn, p);
            return p;
        }
    }

    public boolean hasNext() {
        if (acc == null) {
            return hasNextService();
        } else {
            PrivilegedAction<Boolean> action = new PrivilegedAction<Boolean>() {
                public Boolean run() { return hasNextService(); }
            };
            return AccessController.doPrivileged(action, acc);
        }
    }

    public S next() {
        if (acc == null) {
            return nextService();
        } else {
            PrivilegedAction<S> action = new PrivilegedAction<S>() {
                public S run() { return nextService(); }
            };
            return AccessController.doPrivileged(action, acc);
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

}

//获取迭代器
//返回遍历服务提供者的迭代器
//以懒加载的方式加载可用的服务提供者
//懒加载的实现是：解析配置文件和实例化服务提供者的工作由迭代器本身完成
public Iterator<S> iterator() {
    return new Iterator<S>() {
        //按照实例化顺序返回已经缓存的服务提供者实例
        Iterator<Map.Entry<String,S>> knownProviders
            = providers.entrySet().iterator();

        public boolean hasNext() {
            if (knownProviders.hasNext())
                return true;
            return lookupIterator.hasNext();
        }

        public S next() {
            if (knownProviders.hasNext())
                return knownProviders.next().getValue();
            return lookupIterator.next();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    };
}

//为指定的服务使用指定的类加载器来创建一个ServiceLoader
public static <S> ServiceLoader<S> load(Class<S> service,
                                        ClassLoader loader)
{
    return new ServiceLoader<>(service, loader);
}

//使用线程上下文的类加载器来创建ServiceLoader
public static <S> ServiceLoader<S> load(Class<S> service) {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    return ServiceLoader.load(service, cl);
}

//使用扩展类加载器为指定的服务创建ServiceLoader
//只能找到并加载已经安装到当前Java虚拟机中的服务提供者，应用程序类路径中的服务提供者将被忽略
public static <S> ServiceLoader<S> loadInstalled(Class<S> service) {
    ClassLoader cl = ClassLoader.getSystemClassLoader();
    ClassLoader prev = null;
    while (cl != null) {
        prev = cl;
        cl = cl.getParent();
    }
    return ServiceLoader.load(service, prev);
}

public String toString() {
    return "java.util.ServiceLoader[" + service.getName() + "]";
}
```

1. `LazyIterator`中的`hasNext`方法，静态变量PREFIX就是`META-INFO/services`目录，所以加载这个目录下的配置文件
2. 最终调用`nextService`方法内的`Class.forName()`加载类对象，并用`newInstance()`实例化对象，写入`LinkedHashMap`对象`providers`中
3. 通过`iterator`方法遍历`provider`对象，拿到所有实例对象

缺陷：

1. 不能按需加载，通过iterator全部遍历一遍
2. 获取实现类的方式不灵活
3. 多线程下的`ServiceLoader`不安全

## SPI机制的简单示例

- 先定义好接口

```java
public interface DemoService {
    void demoTest(String message);
}
```

- 编写实现类

```java
// 两个测试实现类
public class Demo1ServiceImpl implements DemoService {
    @Override
    public void demoTest(String message) {
        System.out.println("Demo1Service: " + message);
    }
}

public class Demo2ServiceImpl implements DemoService {
    @Override
    public void demoTest(String message) {
        System.out.println("Demo2Service: " + message);
    }
}
```

- resources下创建META-INFO/services文件夹，创建接口全限定类名为名称的文件：`cn.com.demo.spi.service.DemoService`，并在文件内容添加实现类

```java
cn.com.demo.spi.service.impl.Demo1ServiceImpl
cn.com.demo.spi.service.impl.Demo2ServiceImpl
```

- 编写测试类

```java
public class TestClient {
    public static void main(String[] args) {
        // load时，回去META-INFO/services下找到接口的全限定名的文件，根据里面的内容加载实现类
        ServiceLoader<DemoService> loader = ServiceLoader.load(DemoService.class);
        Iterator<DemoService> iterator = loader.iterator();
        while (iterator.hasNext()) {
            DemoService next = iterator.next();
            next.demoTest("spi create success");
        }
    }
}
```

输出结果可以看到有两个输出：

`Demo1Service: spi create success`

`Demo2Service: spi create success`

## SPI机制加载外部jar

- 将简单示例打包成jar，放入新项目的`plugins`文件夹下
- 加载指定路径下的jar并生成`ClassLoader`

```java
public ClassLoader loadJarBySpi(String path) throws MalformedURLException {
    URL[] jarUrl = new URL[] { new URL("jar:file:/" + path + "!/") };
    return new URLClassLoader(jarUrl);
}
```

- 利用`ClassLoader`结合`SPI`机制加载指定实现类实例对象并输出

```java
public static void test() {
LocalClassLoader classLoader = new LocalClassLoader();
    try {
        // jar存放路径
        String path = "../../../../../../plugins";
        // 寻找路径下所有jar包
        List<String> jars = classLoader.loadJar(path);
        for (String jar : jars) {
            ClassLoader loader = classLoader.loadJarBySpi(path + File.separator +jar);
            ServiceLoader<DemoService> serviceLoader = ServiceLoader.load(DemoService.class, loader);
            Iterator<DemoService> iterator = serviceLoader.iterator();
            while (iterator.hasNext()) {
                DemoService next = iterator.next();
                next.demoTest("load success");
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

优点：可以动态加载指定路径下的jar

## Spring中SPI机制

在spring的自动装配过程中，最终会加载`META-INFO/spring-factories`文件，由`SpringFactoriesLoader`加载。从`CLASSPATH`下寻找所有jar的`META-INFO/spring-factories`配置文件，然后载入properites对象，找到指定名称的配置并返回。

- 配置文件`spring-factories`，多值用`，`隔开

```java
cn.com.demo.spi.service.DemoSerivce=cn.com.demo.spi.service.impl.Demo1ServiceImpl, cn.com.demo.spi.service.impl.Demo2ServiceImpl
```

- `SpringFactoriesLoader`加载配置文件信息

```java
public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/spring.factories";
// spring.factories文件的格式为：key=value1,value2,value3
// 从所有的jar包中找到META-INF/spring.factories文件
// 然后从文件中解析出key=factoryClass类名称的所有value值
public static List<String> loadFactoryNames(Class<?> factoryClass, ClassLoader classLoader) {
    String factoryClassName = factoryClass.getName();
    // 取得资源文件的URL
    Enumeration<URL> urls = (classLoader != null ? classLoader.getResources(FACTORIES_RESOURCE_LOCATION) : ClassLoader.getSystemResources(FACTORIES_RESOURCE_LOCATION));
    List<String> result = new ArrayList<String>();
    // 遍历所有的URL
    while (urls.hasMoreElements()) {
        URL url = urls.nextElement();
        // 根据资源文件URL解析properties文件，得到对应的一组@Configuration类
        Properties properties = PropertiesLoaderUtils.loadProperties(new UrlResource(url));
        String factoryClassNames = properties.getProperty(factoryClassName);
        // 组装数据，并返回
        result.addAll(Arrays.asList(StringUtils.commaDelimitedListToStringArray(factoryClassNames)));
    }
    return result;
}
```

