package com.albert.summer.context;

import com.albert.summer.annotation.*;
import com.albert.summer.exception.*;
import com.albert.summer.io.ResourceResolver;
import com.albert.summer.property.PropertyResolver;
import com.albert.summer.utils.ClassUtils;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于注解配置注入Bean
 *
 * @author yangjunwei
 * @date 2024/7/16
 */
@Slf4j
public class AnnotationConfigApplicationContext {

    /**
     * 属性解析器
     */
    protected final PropertyResolver propertyResolver;

    /**
     * 存放Bean定义
     */
    protected final Map<String, BeanDefinition> beans;

    /**
     * 当前正在创建的所有Bean的名称
     */
    protected Set<String> createingBeanNames;

    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;

        // 扫描获取所有Bean的Class类型:
        final Set<String> beanClassNames = scanForClassNames(configClass);

        // 创建Bean的定义:
        this.beans = createBeanDefinitions(beanClassNames);

        //--------在这一步BeanDefinition已经扫描完毕--------------

        //--------接下来开始创建Bean--------------


        //检测BeanName的循环依赖（强依赖检测）
        this.createingBeanNames = new HashSet<>();

        //首先创建@Configuration类型的Bean，因为@Configuration类型的Bean是一个工厂类，method对应的也是需要注入的Bean。
        this.beans.values().stream()
                .filter(this::isConfigurationDefinition).sorted().map(definition -> {
                    //创建@Configuration类型的Bean
                    createBeanAsEarlySingleton(definition);
                    return definition.getName();
                }).collect(Collectors.toList());

        //创建其他普通Bean
        List<BeanDefinition> defs = this.beans.values().stream()
                // 过滤BeanDefinition 为 null 的实例对象
                .filter(def -> def.getInstance() == null).sorted().collect(Collectors.toList());

        defs.forEach(def -> {
            // 如果Bean未被创建(可能在其他Bean的构造方法注入前被创建):
            if (def.getInstance() == null) {
                // 创建Bean
                createBeanAsEarlySingleton(def);
            }
        });

    }

    private boolean isConfigurationDefinition(BeanDefinition definition) {
        return ClassUtils.findAnnotation(definition.getBeanClass(), Configuration.class) != null;
    }


    /**
     * Do component scan and return class names
     * 扫描@ComponentScan注解指定包下所有的类
     * 扫描@Import注解类
     * 如果当前类没有加注解，那么扫描类所在pkg
     *
     * @param configClass
     * @return
     */
    protected Set<String> scanForClassNames(Class<?> configClass) {
        //扫描ComponentScan注解
        ComponentScan componentScan = ClassUtils.findAnnotation(configClass, ComponentScan.class);
        //获取注解指定
        String[] scanPackages = componentScan == null || componentScan.value().length == 0 ?
                new String[]{configClass.getPackage().getName()} : componentScan.value();
        log.info("component scan in pkg : " + Arrays.toString(scanPackages));

        Set<String> classNameSet = new HashSet<>();
        //扫描pkg下所有的类
        for (String scanPackage : scanPackages) {
            log.info("scan package : " + scanPackage);
            var rr = new ResourceResolver(scanPackage);
            List<String> classList = rr.scan(res -> {
                String name = res.name();
                if (name.endsWith(".class")) {
                    return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
                }
                return null;
            });
            classNameSet.addAll(classList);
        }

        //查找@Import
        Import importConfig = configClass.getAnnotation(Import.class);
        if (importConfig != null) {
            for (Class<?> importClass : importConfig.value()) {
                String name = importClass.getName();
                if (classNameSet.contains(name)) {
                    log.warn("ignore import : " + name + " for it is already been scanned");
                } else {
                    log.info("class found by import : " + name);
                    classNameSet.add(name);
                }
            }
        }
        return classNameSet;
    }

    /**
     * 根据扫描的ClassName创建BeanDefinition
     */
    Map<String, BeanDefinition> createBeanDefinitions(Set<String> classNameSet) {
        Map<String, BeanDefinition> defs = new HashMap<>();
        for (String className : classNameSet) {
            // 反射获取Class
            Class<?> clazz = null;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new BeanCreationException(e);
            }
            //Annotation、enum、interface、record不需要加载
            if (clazz.isAnnotation() || clazz.isEnum() || clazz.isInterface() || clazz.isRecord()) {
                continue;
            }
            // 是否标注@Component?
            Component component = ClassUtils.findAnnotation(clazz, Component.class);
            if (component != null) {
                log.info("found component: " + clazz.getName());
                int mod = clazz.getModifiers();
                if (Modifier.isAbstract(mod)) {
                    throw new BeanDefinitionException("@Component class " + clazz.getName() + " must not be abstract.");
                }
                if (Modifier.isPrivate(mod)) {
                    throw new BeanDefinitionException("@Component class " + clazz.getName() + " must not be private.");
                }

                String beanName = ClassUtils.getBeanName(clazz);
                var def = new BeanDefinition(beanName, clazz, getSuitableConstructor(clazz), getOrder(clazz), clazz.isAnnotationPresent(Primary.class),
                        // named init / destroy method:
                        null, null,
                        // init method:
                        ClassUtils.findAnnotationMethod(clazz, PostConstruct.class),
                        // destroy method:
                        ClassUtils.findAnnotationMethod(clazz, PreDestroy.class));
                addBeanDefinitions(defs, def);
                log.debug("define bean: {}", def);

                //扫描Configuration注解
                Configuration configuration = ClassUtils.findAnnotation(clazz, Configuration.class);
                if (configuration != null) {
                    //扫描Configuration注解里面的@Bean
                    scanFactoryMethods(beanName, clazz, defs);
                }
            }
        }
        return defs;
    }

    /**
     * Get order by:
     *
     * <code>
     * &#64;Order(100)
     * &#64;Component
     * public class Hello {}
     * </code>
     */
    int getOrder(Class<?> clazz) {
        Order order = clazz.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    /**
     * Get order by:
     *
     * <code>
     * &#64;Order(100)
     * &#64;Bean
     * Hello createHello() {
     * return new Hello();
     * }
     * </code>
     */
    int getOrder(Method method) {
        Order order = method.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    /**
     * Check and add bean definitions.
     */
    void addBeanDefinitions(Map<String, BeanDefinition> defs, BeanDefinition def) {
        if (defs.put(def.getName(), def) != null) {
            throw new BeanDefinitionException("Duplicate bean name: " + def.getName());
        }
    }

    /**
     * Get public constructor or non-public constructor as fallback.
     */
    Constructor<?> getSuitableConstructor(Class<?> clazz) {
        Constructor<?>[] cons = clazz.getConstructors();
        if (cons.length == 0) {
            //获取类声明的所有构造函数
            cons = clazz.getDeclaredConstructors();
            if (cons.length != 1) {
                throw new BeanDefinitionException("More than one constructor found in class " + clazz.getName() + ".");
            }
        }
        if (cons.length != 1) {
            throw new BeanDefinitionException("More than one public constructor found in class " + clazz.getName() + ".");
        }
        return cons[0];
    }


    /**
     * 根据BeanName查询BeanDefinition
     *
     * @param beanName
     * @return
     */
    @Nullable
    public BeanDefinition findBeanDefinition(String beanName) {
        return this.beans.get(beanName);
    }

    public List<BeanDefinition> findBeanDefinitions(Class<?> type) {
        return this.beans.values().stream()
                //类型过滤
                //isAssignableFrom()方法用于判断当前Class对象所表示的类或接口是否可以被指定的另一个Class对象所表示的类或接口所赋值。
                .filter(def -> type.isAssignableFrom(def.getBeanClass()))
                .sorted().collect(Collectors.toList());
    }


    /**
     * 根据type查找某个BeanDefinition，如果不存在返回null，如果存在多个返回@Primary标注的一个
     *
     * @param type
     * @return
     */
    @Nullable
    public BeanDefinition findBeanDefinition(Class<?> type) {
        //查询某个类型下所有Bean，包含子类、实现类
        List<BeanDefinition> beanDefinitions = findBeanDefinitions(type);
        if (beanDefinitions.isEmpty()) {
            return null;
        }
        //找到唯一一个Bean
        if (beanDefinitions.size() == 1) {
            return beanDefinitions.getFirst();
        }
        //more than 1 beans,require @Primary
        List<BeanDefinition> primaryDefs = beanDefinitions.stream().filter(BeanDefinition::isPrimary).toList();
        //@Primary唯一标注的Bean
        if (primaryDefs.size() == 1) {
            return primaryDefs.getFirst();
        }
        if (primaryDefs.isEmpty()) {
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, but no @Primary specified.", type.getName()));
        } else {
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, and multiple @Primary specified.", type.getName()));
        }
    }

    /**
     * 根据Name和Type查找BeanDefinition，如果Name不存在，返回null，如果Name存在，但Type不匹配，抛出异常。
     */
    @Nullable
    public BeanDefinition findBeanDefinition(String name, Class<?> requiredType) {
        BeanDefinition def = findBeanDefinition(name);
        if (def == null) {
            return null;
        }
        if (!requiredType.isAssignableFrom(def.getBeanClass())) {
            throw new BeanNotOfRequiredTypeException(String.format("Autowire required type '%s' but bean '%s' has actual type '%s'.", requiredType.getName(),
                    name, def.getBeanClass().getName()));
        }
        return def;
    }

    /**
     * Scan factory method that annotated with @Bean:
     *
     * <code>
     * &#64;Configuration
     * public class Hello {
     *
     * @Bean ZoneId createZone() {
     * return ZoneId.of("Z");
     * }
     * }
     * </code>
     */
    void scanFactoryMethods(String factoryBeanName, Class<?> clazz, Map<String, BeanDefinition> defs) {
        for (Method method : clazz.getDeclaredMethods()) {
            //扫描方法上的@Bean注解
            Bean bean = method.getAnnotation(Bean.class);
            if (bean != null) {
                int mod = method.getModifiers();
                //abstract
                if (Modifier.isAbstract(mod)) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be abstract.");
                }
                //final
                if (Modifier.isFinal(mod)) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be final.");
                }
                //private
                if (Modifier.isPrivate(mod)) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be private.");
                }
                Class<?> beanClass = method.getReturnType();
                if (beanClass.isPrimitive()) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not return primitive type.");
                }
                if (beanClass == void.class || beanClass == Void.class) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not return void.");
                }
                var def = new BeanDefinition(ClassUtils.getBeanName(method), beanClass, factoryBeanName, method, getOrder(method),
                        method.isAnnotationPresent(Primary.class),
                        // init method:
                        bean.initMethod().isEmpty() ? null : bean.initMethod(),
                        // destroy method:
                        bean.destroyMethod().isEmpty() ? null : bean.destroyMethod(),
                        // @PostConstruct / @PreDestroy method:
                        null, null);
                addBeanDefinitions(defs, def);
                log.debug("define bean: {}", def);
            }
        }
    }

    /**
     * 创建Bean，但是不进行字段和方法级别的注入
     * 如果创建的Bean不是Configuration。
     * 则在构造方法/工厂方法中注入的依赖Bean会自动创建，这种强依赖解决不了
     * <p>
     * 循环依赖检测 -> 处理入参 -> 创建Bean实例
     *
     * @param def
     * @return
     */
    public Object createBeanAsEarlySingleton(BeanDefinition def) {

        //首先进行循环依赖检测
        if (!this.createingBeanNames.add(def.getName())) {
            //重复创建Bean导致的循环依赖
            //强依赖抛出异常
            //比如创建某个Bean的时候，先把Bean添加到set中。然后执行构造方法，触发循环依赖，发现set已经包含了该Bean
            //A->B, B->A
            throw new UnsatisfiedDependencyException();
        }

        //创建方式：构造方法或者工厂方法
        //Executable是一个抽象类，提供了对类里面可执行成员的通用访问和操作。比如 Method和Constructor。
        Executable createFn = def.getFactoryName() == null ? def.getConstructor() : def.getFactoryMethod();

        //入参
        Parameter[] parameters = createFn.getParameters();
        //入参注解
        //比如这种构造方法，多注解
        //HelloD(@Value("spring.name") @NotNull String name){}
        Annotation[][] parameterAnnotations = createFn.getParameterAnnotations();
        //方法入参，保存解析后的实际对象
        Object[] args = new Object[parameters.length];

        //循环处理入参
        for (int i = 0; i < parameters.length; i++) {
            final Parameter parameter = parameters[i];
            final Annotation[] paramAnnos = parameterAnnotations[i];

            //从方法入参获取@Value和@Autowired
            final Value value = ClassUtils.getAnnotation(paramAnnos, Value.class);
            final Autowired autowired = ClassUtils.getAnnotation(paramAnnos, Autowired.class);

            // @Configuration类型的Bean是工厂，不允许使用@Autowired创建@Configuration类型的Bean:
            // 因为会导致潜在的循环依赖，比如 @Configuration->A， A->B, @Configuration里面的@Bean包含B，而B只能在@Configuration初始化之后初始化。
            final boolean isConfiguration = isConfigurationDefinition(def);
            //这种是针对构造方法的场景，工厂方法的场景，@Configuration已经创建完了
            if (isConfiguration && autowired != null) {
                throw new BeanCreationException(
                        String.format("Cannot specify @Autowired when create @Configuration bean '%s': %s.", def.getName(), def.getBeanClass().getName()));
            }

            // 参数需要@Value或@Autowired两者之一:要不属于静态资源，要不属于其它Bean
            if (value != null && autowired != null) {
                throw new BeanCreationException(
                        String.format("Cannot specify both @Autowired and @Value when create bean '%s': %s.", def.getName(), def.getBeanClass().getName()));
            }
            //如果入参不属于@Value或@Autowired两者之一，则Spring不知道该为参数如何赋值，直接抛异常。
            if (value == null && autowired == null) {
                throw new BeanCreationException(
                        String.format("Must specify @Autowired or @Value when create bean '%s': %s.", def.getName(), def.getBeanClass().getName()));
            }

            //参数类型
            final Class<?> type = parameter.getType();
            //1.入参为@Value
            if (value != null) {
                //参数是@Value，从参数解析器 propertyResolver中查询Value对应的属性
                args[i] = this.propertyResolver.getRequiredProperty(value.value(), type);
            } else {
                //2.入参为@Autowired
                String name = autowired.name();
                boolean required = autowired.value();
                //根据 type 和 name 查找 bean
                //1.如果设置了name，则查找name和type都匹配的bean。
                //2.未设置name，根据type查找
                //返回值有几种情况：未找到Bean，同一个类型多个Bean返回标识@Primary注解的Bean，同一类型不包含@Primary注解抛出异常。
                BeanDefinition definitionOnDef = name.isEmpty() ? findBeanDefinition(type) : findBeanDefinition(name, type);

                if (required && definitionOnDef == null) {
                    //required则抛出异常
                    throw new BeanCreationException(String.format("Missing autowired bean with type '%s' when create bean '%s': %s.", type.getName(),
                            def.getName(), def.getBeanClass().getName()));
                }

                if (definitionOnDef != null) {
                    //获取入参依赖的Bean
                    Object autowiredBeanInstance = definitionOnDef.getInstance();
                    //当前Bean实例对象为空，不是@Configuration工厂类
                    if (autowiredBeanInstance == null & !isConfiguration) {
                        //该Bean尚未初始化，递归调用初始化该Bean
                        autowiredBeanInstance = createBeanAsEarlySingleton(definitionOnDef);
                    }
                    //设置入参真实值
                    args[i] = autowiredBeanInstance;
                } else {
                    args[i] = null;
                }
            }
        }

        //创建Bean实例
        Object instance = null;
        //构造方法或者工厂方法（@Bean）
        if (def.getFactoryName() == null) {
            try {
                //构造方法
                //根据入参真实值和构造方法创建Bean实例
                instance = def.getConstructor().newInstance(args);
            } catch (Exception e) {
                throw new BeanCreationException(String.format("Exception when create bean '%s': %s", def.getName(), def.getBeanClass().getName()), e);
            }
        } else {
            //@Bean方式创建
            //先查找出@Configuration对应的类
            Object bean = getBean(def.getFactoryName());
            try {
                //通过反射，传入对象和入参，即可根据method构建出一个对象
                instance = def.getFactoryMethod().invoke(bean, args);
            } catch (Exception e) {
                throw new BeanCreationException(String.format("Exception when create bean '%s': %s", def.getName(), def.getBeanClass().getName()), e);
            }
        }
        //设置Bean实例
        def.setInstance(instance);
        return def.getInstance();
    }


    /**
     * 通过Name查找Bean，不存在时抛出NoSuchBeanDefinitionException
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        BeanDefinition def = this.beans.get(name);
        if (def == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with name '%s'.", name));
        }
        return (T) def.getRequiredInstance();
    }

    /**
     * 通过Type查找Bean，不存在时抛出NoSuchBeanDefinitionException
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<?> clazz) {
        BeanDefinition def = findBeanDefinition(clazz);
        if (def == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with clazz '%s'.", clazz));
        }
        return (T) def.getRequiredInstance();
    }

    public boolean containsBeanDefinition(String name) {
        return this.beans.containsKey(name);
    }


}
