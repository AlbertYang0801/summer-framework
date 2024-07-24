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

import java.io.IOException;
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
public class AnnotationConfigApplicationContext implements ConfigurableApplicationContext {

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

    /**
     * BeanPostProcessor 需要在创建Bean之后执行
     */
    private List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        ApplicationContextUtils.setApplicationContext(this);

        this.propertyResolver = propertyResolver;

        // 1.扫描获取所有Bean的Class类型:
        final Set<String> beanClassNames = scanForClassNames(configClass);

        // 2.创建Bean的定义:BeanDefinition
        this.beans = createBeanDefinitions(beanClassNames);

        //--------在这一步BeanDefinition已经扫描完毕--------------

        //--------接下来开始创建Bean实例--------------

        //检测BeanName的循环依赖（强依赖检测）
        this.createingBeanNames = new HashSet<>();

        //首先创建@Configuration类型的Bean，因为@Configuration类型的Bean是一个工厂类，method对应的也是需要注入的Bean。
        this.beans.values().stream()
                .filter(this::isConfigurationDefinition).sorted().map(definition -> {
                    //创建@Configuration类型的Bean
                    createBeanAsEarlySingleton(definition);
                    return definition.getName();
                }).collect(Collectors.toList());

        //创建BeanPostProcessor类型的Bean
        List<BeanPostProcessor> processors = this.beans.values().stream()
                .filter(this::isBeanPostProcessorDefinition)
                .sorted()
                .map(definition -> (BeanPostProcessor) createBeanAsEarlySingleton(definition))
                .collect(Collectors.toList());
        this.beanPostProcessors.addAll(processors);


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

        //-----------------在这一步创建Bean实例完成-----------------

        //-----------------3.开始对实例Bean进行属性注入和字段注入-----------------

        //通过字段和set方法注入依赖
        this.beans.values().forEach(this::injectBean);

        //调用init方法
        this.beans.values().forEach(this::initBean);


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
                        // 扫描@PostConstruct注解
                        ClassUtils.findAnnotationMethod(clazz, PostConstruct.class),
                        // 扫描@PreDestroy注解
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
     * 创建Bean，但是不进行字段和方法级别的注入
     * 如果创建的Bean不是Configuration。
     * 则在构造方法/工厂方法中注入的依赖Bean会自动创建，这种强依赖解决不了
     * <p>
     * 循环依赖检测 -> 处理入参 -> 创建Bean实例
     *
     * @param def
     * @return
     */
    @Override
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

            // BeanPostProcessor不能依赖其他Bean，不允许使用@Autowired创建:
            final boolean isBeanPostProcessor = isBeanPostProcessorDefinition(def);
            if (isBeanPostProcessor && autowired != null) {
                throw new BeanCreationException(
                        String.format("Cannot specify @Autowired when create BeanPostProcessor '%s': %s.", def.getName(), def.getBeanClass().getName()));
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

        //调用BeanPostProcessor处理Bean
        //每个Bean在创建的时候要执行所有的BeanPostProcessor方法
        for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
            //BeanPostProcessor的before方法
            //增强Bean或者AOP
            Object processed = beanPostProcessor.postProcessBeforeInitialization(def.getInstance(), def.getName());
            if (processed == null) {
                throw new BeanCreationException(String.format("PostBeanProcessor returns null when process bean '%s' by %s", def.getName(), beanPostProcessor));
            }
            //如果BeanPostProcessor替换了原始Bean，需要更换Bean的引用。
            //ProxyObj在这里发生了
            if (def.getInstance() != processed) {
                log.debug("Bean '{}' was replaced by post processor {}.", def.getName(), beanPostProcessors.getClass().getName());
                def.setInstance(processed);
            }
        }

        return def.getInstance();
    }

    /**
     * 注入属性
     *
     * @param def
     */
    private void injectBean(BeanDefinition def) {
        try {
            //获取Bean实例或者被代理的原始对象，注入属性
            Object proxiedInstance = getProxiedInstance(def);
            injectProperties(def, def.getBeanClass(), proxiedInstance);
        } catch (ReflectiveOperationException e) {
            throw new BeanCreationException(e);
        }
    }

    /**
     * 获取原始Bean
     *
     * @param def
     * @return
     */
    private Object getProxiedInstance(BeanDefinition def) {
        Object beanInstance = def.getInstance();
        // 如果Proxy改变了原始Bean，又希望注入到原始Bean，则由BeanPostProcessor指定原始Bean:
        List<BeanPostProcessor> reversedBeanPostProcessors = new ArrayList<>(this.beanPostProcessors);
        Collections.reverse(reversedBeanPostProcessors);
        for (BeanPostProcessor beanPostProcessor : reversedBeanPostProcessors) {
            Object restoredInstance = beanPostProcessor.postProcessOnSetProperty(beanInstance, def.getName());
            if (restoredInstance != beanInstance) {
                beanInstance = restoredInstance;
            }
        }
        return beanInstance;
    }

    /**
     * 执行init方法，@PostConstruct对应方法
     *
     * @param def
     */
    private void initBean(BeanDefinition def) {
        //调用init方法
        callMethod(def.getInstance(), def.getInitMethod(), def.getInitMethodName());
    }

    /**
     * 执行destroy方法，@PreDestroy对应方法
     *
     * @param def
     */
    private void destroyBean(BeanDefinition def) {
        //调用init方法
        callMethod(def.getInstance(), def.getDestroyMethod(), def.getDestroyMethodName());
    }

    /**
     * 调用init/destroy方法
     *
     * @param beanInstance
     * @param method       方法，直接根据方法进行反射
     * @param namedMethod  方法名，直接根据方法名进行反射
     */
    private void callMethod(Object beanInstance, Method method, String namedMethod) {
        if (method != null) {
            try {
                //反射执行Bean的init/destroy方法
                method.invoke(beanInstance);
            } catch (ReflectiveOperationException e) {
                throw new BeanCreationException(e);
            }
        } else if (namedMethod != null) {
            //查找class里面的namedMethod方法
            Method named = ClassUtils.getNamedMethod(beanInstance.getClass(), namedMethod);
            if (named != null) {
                named.setAccessible(true);
                try {
                    named.invoke(beanInstance);
                } catch (ReflectiveOperationException e) {
                    throw new BeanCreationException(e);
                }
            }
        }
    }

    /**
     * 注入当前类属性和父类属性
     *
     * @param definition
     * @param clazz      当前BeanClass
     * @param bean       Bean实例
     */
    void injectProperties(BeanDefinition definition, Class<?> clazz, Object bean) throws ReflectiveOperationException {

        //获取当前类声明的所有字段
        for (Field declaredField : clazz.getDeclaredFields()) {
            //字段属性注入
            tryInjectProperties(definition, clazz, bean, declaredField);
        }
        //获取当前类声明的所有方法
        for (Method declaredMethod : clazz.getDeclaredMethods()) {
            //方法属性注入
            tryInjectProperties(definition, clazz, bean, declaredMethod);
        }

        //父类的属性和方法
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            //递归为当前Bean注入父类的属性或方法
            injectProperties(definition, superclass, bean);
        }
    }

    /**
     * 为bean实例注入单个属性
     * 属性可以是@Value和@Autowired注解
     * 可以是字段，也可以是方法
     * 如果是@Value，则从PropertyResover中获取配置，注入到当前BeanObj中。
     * 如果是@Autowired，则查找对应的Bean实例，注入到当前BeanObj中
     *
     * @param def
     * @param clazz
     * @param bean
     * @param acc   字段、方法、构造方法等统一父类
     * @throws ReflectiveOperationException
     */
    void tryInjectProperties(BeanDefinition def, Class<?> clazz, Object bean, AccessibleObject acc) throws ReflectiveOperationException {
        Value value = acc.getAnnotation(Value.class);
        Autowired autowired = acc.getAnnotation(Autowired.class);
        //字段或方法未加注解，不需要注入
        if (value == null && autowired == null) {
            return;
        }

        Field field = null;
        Method method = null;

        //字段属性注入
        //判断acc是否是Field类型，如果是则将acc赋值给f
        if (acc instanceof Field f) {
            //检查字段类型，如果是static和final修饰则无法注入属性
            checkFiledOrMethod(f);
            //修改字段访问权限
            f.setAccessible(true);
            field = f;
        }

        //set方法注入
        //set方法注入只推荐一个属性
        //public class MyClass {
        //    private MyDependency dependency;
        //
        //    @Autowired
        //    public void setDependency(MyDependency dependency) {
        //        this.dependency = dependency;
        //    }
        //}
        if (acc instanceof Method m) {
            checkFiledOrMethod(m);
            if (m.getParameters().length != 1) {
                throw new BeanDefinitionException(
                        String.format("Cannot inject a non-setter method %s for bean '%s': %s", m.getName(), def.getName(), def.getBeanClass().getName()));
            }
            m.setAccessible(true);
            method = m;
        }

        //字段名或方法名
        String accessibleName = field != null ? field.getName() : method.getName();
        //参数类型，属性或者方法第一个入类型
        Class<?> accessibleType = field != null ? field.getType() : method.getParameterTypes()[0];

        //@value和@autowired注解不能重复
        if (value != null && autowired != null) {
            throw new BeanCreationException(String.format("Cannot specify both @Autowired and @Value when inject %s.%s for bean '%s': %s",
                    clazz.getSimpleName(), accessibleName, def.getName(), def.getBeanClass().getName()));
        }

        //@Value注入
        if (value != null) {
            //获取@Value注解真实配置
            Object propValue = this.propertyResolver.getRequiredProperty(value.value(), accessibleType);
            if (field != null) {
                log.debug("Field injection: {}.{} = {}", def.getBeanClass().getName(), accessibleName, propValue);
                //设置obj的某个字段值
                field.set(bean, propValue);
            }
            //@Value在method级别，只能作为入参
            //然后类似执行普通方法一样调用所在方法
            if (method != null) {
                log.debug("Method injection: {}.{} ({})", def.getBeanClass().getName(), accessibleName, propValue);
                //调用了obj的method，并传入入参propValue
                method.invoke(bean, propValue);
            }
        }

        //@Autowired注入
        if (autowired != null) {
            String name = autowired.name();
            boolean required = autowired.value();
            //获取属性对应的Bean实例
            Object depends = name.isEmpty() ? findBean(accessibleType) : findBean(name, accessibleType);
            //bean not found
            if (required && depends == null) {
                throw new UnsatisfiedDependencyException(String.format("Dependency bean not found when inject %s.%s for bean '%s': %s", clazz.getSimpleName(),
                        accessibleName, def.getName(), def.getBeanClass().getName()));
            }
            if (depends != null) {
                if (field != null) {
                    log.debug("Field injection: {}.{} = {}", def.getBeanClass().getName(), accessibleName, depends);
                    //属性对应的beanObj，设置为bean的field值
                    field.set(bean, depends);
                }
                if (method != null) {
                    log.debug("Mield injection: {}.{} ({})", def.getBeanClass().getName(), accessibleName, depends);
                    //将对象作为入参传入，并执行bean对应的method
                    method.invoke(bean, depends);
                }
            }
        }
    }

    /**
     * 检查字段或者方法
     *
     * @param m
     */
    void checkFiledOrMethod(Member m) {
        int modifiers = m.getModifiers();
        //不能注入静态方法
        if (Modifier.isStatic(modifiers)) {
            throw new BeanDefinitionException("Cannot inject static field: " + m);
        }
        //不能注入final属性
        if (Modifier.isFinal(modifiers)) {
            if (m instanceof Field field) {
                throw new BeanDefinitionException("Cannot inject final field: " + field);
            }
            if (m instanceof Method method) {
                log.warn("Inject final method should be careful because it is not called on target bean when bean is proxied and may cause NullPointerException.");
            }
        }
    }


    private boolean isConfigurationDefinition(BeanDefinition definition) {
        return ClassUtils.findAnnotation(definition.getBeanClass(), Configuration.class) != null;
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
    @Override
    public BeanDefinition findBeanDefinition(String beanName) {
        return this.beans.get(beanName);
    }

    @Override
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
    @Override
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
    @Override
    public BeanDefinition findBeanDefinition(String name, Class<?> requiredType) {
        //先根据name查找Bean
        BeanDefinition def = findBeanDefinition(name);
        if (def == null) {
            return null;
        }
        //校验Bean的ClassType
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

    boolean isBeanPostProcessorDefinition(BeanDefinition def) {
        return BeanPostProcessor.class.isAssignableFrom(def.getBeanClass());
    }

    /**
     * 根据lassType获取BeanObj
     *
     * @param requiredType
     * @param <T>
     * @return
     */
    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> T findBean(Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(requiredType);
        if (def == null) {
            return null;
        }
        return (T) def.getRequiredInstance();
    }

    /**
     * 根据name和classType获取BeanObj
     *
     * @param name
     * @param requiredType
     * @param <T>
     * @return
     */
    //可能为空
    @Nullable
    //忽略warn
    @SuppressWarnings("unchecked")
    protected <T> T findBean(String name, Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(name, requiredType);
        if (def == null) {
            return null;
        }
        return (T) def.getRequiredInstance();
    }

    @Override
    public boolean containsBean(String name) {
        return this.beans.containsKey(name);
    }

    /**
     * 通过Name查找Bean，不存在时抛出NoSuchBeanDefinitionException
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getBean(String name) {
        BeanDefinition def = this.beans.get(name);
        if (def == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with name '%s'.", name));
        }
        return (T) def.getRequiredInstance();
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        //先根据name查找Bean，然后校验Bean的ClassType
        BeanDefinition def = findBeanDefinition(name, requiredType);
        if (def == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with name '%s'.", name));
        }
        return (T) def.getRequiredInstance();
    }

    /**
     * 通过Type查找Bean，不存在时抛出NoSuchBeanDefinitionException
     */
    @Override
    public <T> T getBean(Class<T> requiredType) {
        //根据类型查找Bean
        //如果存在多个相同类型的Bean，查找@Primary的bean
        BeanDefinition def = findBeanDefinition(requiredType);
        if (def == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with clazz '%s'.", requiredType));
        }
        return (T) def.getRequiredInstance();
    }

    @Override
    public <T> List<T> getBeans(Class<T> requiredType) {
        //查询出相同类型的所有Bean
        List<BeanDefinition> beanDefinitions = findBeanDefinitions(requiredType);
        if (beanDefinitions == null || beanDefinitions.isEmpty()) {
            return List.of();
        }
        List<T> list = new ArrayList<>(beanDefinitions.size());
        for (var def : beanDefinitions) {
            //bean实例
            list.add((T) def.getRequiredInstance());
        }
        return list;
    }

    @Override
    public void close(){
        //在关闭时，调用PreDestroy标注的方法
        for (BeanDefinition value : this.beans.values()) {
            destroyBean(value);
        }
        this.beans.clear();
        this.createingBeanNames.clear();
        ApplicationContextUtils.setApplicationContext(null);
    }


}
