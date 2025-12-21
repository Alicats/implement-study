package cn.xej.api;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Maven插件用于从Spring Boot Controller生成Java SDK
 */
@Mojo(name = "generate")
public class SdkGeneratorMojo extends AbstractMojo {

    /**
     * 当前Maven项目对象
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * SDK输出目录参数
     */
    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/generate-sdk")
    private File outputDirectory;

    /**
     * SDK包名参数
     */
    @Parameter(property = "packageName", defaultValue = "cn.xej.api")
    private String packageName;

    /**
     * 要扫描的Controller包名
     */
    @Parameter(property = "controllerPackage", defaultValue = "cn.xej.api.controller")
    private String controllerPackage;

    // 用于收集所有需要处理的方法，避免重复生成模型类
    private final Set<java.lang.reflect.Method> methods = new HashSet<>();
    
    // 用于收集所有需要生成的模型类，避免重复生成
    private final Set<Class<?>> models = new HashSet<>();
    
    /**
     * 插件的主要执行方法
     *
     * @throws MojoExecutionException 执行异常
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("开始生成SDK...");
        getLog().info("输出目录: " + outputDirectory.getAbsolutePath());
        getLog().info("包名: " + packageName);
        getLog().info("Controller包名: " + controllerPackage);

        try {
            // 确保输出目录存在
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }

            // 创建包目录结构
            String packagePath = packageName.replace(".", "/");
            File packageDir = new File(outputDirectory, packagePath);
            if (!packageDir.exists()) {
                packageDir.mkdirs();
            }

            // 扫描Controller类并生成SDK
            scanControllersAndGenerateSdk(packageDir);

            getLog().info("SDK生成完成!");

        } catch (Exception e) {
            getLog().error("生成SDK时出错", e);
            throw new MojoExecutionException("生成SDK时出错", e);
        }
    }

    private void scanControllersAndGenerateSdk(File packageDir) throws Exception {
        // 构建完整的类路径 - 关键改进：使用当前项目的类路径
        List<URL> urls = new ArrayList<>();

        // 添加当前项目的编译输出目录（包含业务代码）
        String projectOutputDirectory = project.getBuild().getOutputDirectory();
        if (projectOutputDirectory != null) {
            File outputDir = new File(projectOutputDirectory);
            if (outputDir.exists()) {
                urls.add(outputDir.toURI().toURL());
                getLog().info("添加项目输出目录到类路径: " + outputDir.getAbsolutePath());
            } else {
                getLog().warn("项目输出目录不存在: " + outputDir.getAbsolutePath());
            }
        }

        // 添加项目的依赖到类路径
        addProjectDependenciesToClasspath(urls);


        // 创建URLClassLoader - 使用当前线程的类加载器作为父加载器
        URLClassLoader classLoader = new URLClassLoader(
                urls.toArray(new URL[0]),
                Thread.currentThread().getContextClassLoader()
        );

        // 临时设置上下文类加载器，以便Reflections能正确扫描
        Thread.currentThread().setContextClassLoader(classLoader);

        try {
            // 使用Reflections库扫描Controller类
            Configuration configuration = new ConfigurationBuilder()
                    .forPackages(controllerPackage)
                    .addClassLoaders(classLoader)
                    .setScanners(Scanners.TypesAnnotated, Scanners.SubTypes);

            Reflections reflections = new Reflections(configuration);

            // 查找@RestController注解的类
            Set<Class<?>> controllerClasses = reflections.getTypesAnnotatedWith(RestController.class);
            // 过滤掉不在指定包内的Controller类
            Set<Class<?>> filteredControllerClasses = new HashSet<>();
            for (Class<?> controllerClass : controllerClasses) {
                if (controllerClass.getPackage().getName().equals(controllerPackage)) {
                    filteredControllerClasses.add(controllerClass);
                }
            }
            controllerClasses = filteredControllerClasses;
            
            // 输出扫描到的@RestController注解的类，便于调试
            for (Class<?> controllerClass : controllerClasses) {
                getLog().info("扫描到的@RestController注解的类: " + controllerClass.getName());
            }

            // 如果找到Controller，生成ApiClient和models
            if (!controllerClasses.isEmpty()) {
                // 创建models目录
                File modelsDir = new File(packageDir, "models");
                if (!modelsDir.exists()) {
                    modelsDir.mkdirs();
                }
                getLog().info("创建models目录: " + modelsDir.getAbsolutePath());

                // 生成统一的ApiClient
                generateApiClient(controllerClasses, packageDir, modelsDir);
            }
        } finally {
            // 恢复原始的上下文类加载器
            Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
        }
    }


    /**
     * 添加项目依赖到类路径
     */
    private void addProjectDependenciesToClasspath(List<URL> urls) throws MojoFailureException {
        try {
            List<String> classpathElements = project.getCompileClasspathElements();

            for (String element : classpathElements) {
                try {
                    File file = new File(element);
                    if (file.exists()) {
                        urls.add(file.toURI().toURL());
                        getLog().debug("添加依赖到类路径: " + element);
                    } else {
                        getLog().warn("依赖文件不存在: " + element);
                    }
                } catch (MalformedURLException e) {
                    getLog().warn("无法转换为URL: " + element, e);
                }
            }
        } catch (Exception e) {
            throw new MojoFailureException("添加项目依赖到类路径时出错", e);
        }
    }

    /**
     * 生成统一的ApiClient类
     */
    private void generateApiClient(Set<Class<?>> controllerClasses, File outputDir, File modelsDir) throws Exception {
        // 先收集所有需要处理的方法
        collectAllMethods(controllerClasses);
        
        // 统一生成所有模型类，避免重复生成
        generateAllModelClasses(modelsDir);
        
        // 然后生成ApiClient类
        String fileName = "ApiClient.java";
        File outputFile = new File(outputDir, fileName);

        StringBuilder apiClientCode = new StringBuilder();
        
        // 生成包声明
        apiClientCode.append("package ").append(packageName).append(";\n");
        
        // 生成导入语句
        apiClientCode.append("import okhttp3.*;\n");
        apiClientCode.append("import com.fasterxml.jackson.databind.ObjectMapper;\n");
        apiClientCode.append("import java.io.IOException;\n");
        apiClientCode.append("import java.util.Map;\n");
        apiClientCode.append("import java.util.HashMap;\n");
        apiClientCode.append("import ").append(packageName).append(".models.*;\n");
        
        // 添加空行
        apiClientCode.append("\n");
        
        // 生成类注释
        apiClientCode.append("/**\n");
        apiClientCode.append(" * 统一API客户端\n");
        apiClientCode.append(" */\n");
        
        // 生成类声明
        apiClientCode.append("public class ApiClient {\n");
        
        // 生成类成员
        apiClientCode.append("    private final OkHttpClient okHttpClient;\n");
        apiClientCode.append("    private final ObjectMapper objectMapper;\n");
        apiClientCode.append("    private final String baseUrl;\n");
        
        // 添加空行
        apiClientCode.append("\n");
        
        // 生成构造函数
        apiClientCode.append("    public ApiClient(String baseUrl) {\n");
        apiClientCode.append("        this.okHttpClient = new OkHttpClient();\n");
        apiClientCode.append("        this.objectMapper = new ObjectMapper();\n");
        apiClientCode.append("        this.baseUrl = baseUrl.endsWith(\"/\") ? baseUrl : baseUrl + \"/\";\n");
        apiClientCode.append("    }\n");
        
        // 添加空行
        apiClientCode.append("\n");

        // 生成所有Controller的方法
        for (Class<?> controllerClass : controllerClasses) {
            getLog().info("处理Controller类: " + controllerClass.getName());
            generateControllerApiMethods(controllerClass, apiClientCode);
        }

        apiClientCode.append("}");

        // 写入文件
        java.nio.file.Files.write(outputFile.toPath(), apiClientCode.toString().getBytes("UTF-8"));
        getLog().info("生成ApiClient文件: " + outputFile.getAbsolutePath());
    }
    
    /**
     * 收集所有需要处理的方法
     */
    private void collectAllMethods(Set<Class<?>> controllerClasses) {
        methods.clear(); // 清空之前的方法列表
        
        for (Class<?> controllerClass : controllerClasses) {
            for (java.lang.reflect.Method method : controllerClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(PostMapping.class)) {
                    methods.add(method);
                    getLog().info("收集方法: " + method.getName());
                }
            }
        }
    }
    
    /**
     * 统一生成所有模型类
     */
    private void generateAllModelClasses(File modelsDir) throws Exception {
        // 第一步：收集所有需要生成的模型类
        getLog().info("开始收集所有需要生成的模型类...");
        
        // 清空模型集合
        models.clear();
        
        // 收集所有方法的请求和响应模型的嵌套类
        for (java.lang.reflect.Method method : methods) {
            // 查找@RequestBody注解的参数
            java.lang.reflect.Parameter[] parameters = method.getParameters();
            for (java.lang.reflect.Parameter parameter : parameters) {
                if (parameter.isAnnotationPresent(RequestBody.class)) {
                    collectNestedModels(parameter.getType());
                }
            }
            
            // 收集返回类型的嵌套类
            collectNestedModels(method.getReturnType());
        }
        
        // 第二步：生成所有请求和响应模型类
        getLog().info("开始生成所有请求和响应模型类...");
        for (java.lang.reflect.Method method : methods) {
            getLog().info("为方法生成模型类: " + method.getName());
            generateModelClasses(method, modelsDir);
        }
        
        // 第三步：统一生成所有业务对象模型类
        getLog().info("开始统一生成所有业务对象模型类...");
        for (Class<?> model : models) {
            generateBusinessModelClass(model, modelsDir);
        }
    }

    /**
     * 生成Controller的所有方法到ApiClient中（仅生成API方法，不生成模型类）
     */
    private void generateControllerApiMethods(Class<?> controllerClass, StringBuilder apiClientCode) throws Exception {
        // 获取类级别的@RequestMapping注解
        String classMapping = "";
        if (controllerClass.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping classAnnotation = controllerClass.getAnnotation(RequestMapping.class);
            if (classAnnotation.value().length > 0) {
                classMapping = classAnnotation.value()[0];
            }
        }

        // 生成方法
        for (java.lang.reflect.Method method : controllerClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PostMapping.class)) {
                generateMethodApi(method, classMapping, apiClientCode);
            }
        }
    }

    /**
     * 生成API方法到ApiClient中（仅生成API方法，不生成模型类）
     */
    private void generateMethodApi(java.lang.reflect.Method method, String classMapping, StringBuilder apiClientCode) throws Exception {
        // 获取映射信息
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        String mappingValue = mapping.value().length > 0 ? mapping.value()[0] : "";
        String httpMethod = "POST"; // 默认POST

        // 组合完整的URL路径
        String fullMapping = classMapping + mappingValue;
        
        // 生成方法签名
        String methodName = method.getName();
        apiClientCode.append("    public ").append(method.getReturnType().getSimpleName()).append(" ");
        apiClientCode.append(methodName).append("(");

        // 添加参数
        Class<?>[] paramTypes = method.getParameterTypes();
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) apiClientCode.append(", ");
            apiClientCode.append(paramTypes[i].getSimpleName()).append(" ").append(parameters[i].getName());
        }

        apiClientCode.append(") throws IOException {\n");
        apiClientCode.append("        String url = baseUrl + \"");
        apiClientCode.append(fullMapping);
        apiClientCode.append("        RequestBody requestBody = null;\n");

        // 创建请求体
        if (paramTypes.length > 0) {
            apiClientCode.append("        requestBody = RequestBody.create(");
            apiClientCode.append("objectMapper.writeValueAsString(");
            apiClientCode.append(parameters[0].getName());
            apiClientCode.append("), MediaType.parse(\"application/json; charset=utf-8\"));\n\n");
        }

        // 创建请求
        apiClientCode.append("        Request.Builder okHttpRequestBuilder = new Request.Builder()\n");
        apiClientCode.append("                .url(url)\n");

        // 设置请求方法
        apiClientCode.append("                .method(\"").append(httpMethod).append("\", requestBody);\n");
        apiClientCode.append("        Request okHttpRequest = okHttpRequestBuilder.build();\n\n");

        // 执行请求
        apiClientCode.append("        try (Response response = okHttpClient.newCall(okHttpRequest).execute()) {\n");
        apiClientCode.append("            if (!response.isSuccessful()) {\n");
        apiClientCode.append("                throw new IOException(\"Unexpected code \" + response);\n");
        apiClientCode.append("            }\n\n");

        // 解析响应
        apiClientCode.append("            String responseBody = response.body().string();\n");
        apiClientCode.append("            return objectMapper.readValue(responseBody, ");
        apiClientCode.append(method.getReturnType().getSimpleName());
        apiClientCode.append(".class);\n");
        apiClientCode.append("        }\n");
        apiClientCode.append("    }\n\n");

    }

    /**
     * 生成模型类（请求和响应）
     */
    private void generateModelClasses(java.lang.reflect.Method method, File modelsDir) throws Exception {
        String methodName = method.getName();
        
        // 生成请求模型（示例：CreateUserRequest）
        String requestClassName = capitalizeFirstLetter(methodName) + "Request";
        generateRequestModel(method, requestClassName, modelsDir);
        
        // 生成响应模型（示例：CreateUserResponse）
        String responseClassName = capitalizeFirstLetter(methodName) + "Response";
        generateResponseModel(method, responseClassName, modelsDir);
    }

    /**
     * 生成请求模型类
     */
    private void generateRequestModel(java.lang.reflect.Method method, String className, File modelsDir) throws Exception {
        File outputFile = new File(modelsDir, className + ".java");
        
        // 如果文件已存在，跳过
        if (outputFile.exists()) {
            getLog().info("模型类已存在，跳过生成: " + outputFile.getAbsolutePath());
            return;
        }
        
        StringBuilder modelCode = new StringBuilder();
        modelCode.append("package ").append(packageName).append(".models;\n\n");
        modelCode.append("import java.io.Serializable;\n");
        modelCode.append("\n");
        modelCode.append("import java.util.*;\n\n");
        
        modelCode.append("/**\n");
        modelCode.append(" * ").append(className).append(" 请求模型\n");
        modelCode.append(" */\n");
        modelCode.append("public class ").append(className).append(" implements Serializable {\n");
        modelCode.append("    private static final long serialVersionUID = 1L;\n\n");
        
        // 查找@RequestBody注解的参数
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        Class<?> requestBodyType = null;
        for (java.lang.reflect.Parameter parameter : parameters) {
            if (parameter.isAnnotationPresent(RequestBody.class)) {
                requestBodyType = parameter.getType();
                break;
            }
        }
        
        // 如果找到@RequestBody参数，提取其字段
        if (requestBodyType != null && !requestBodyType.isPrimitive() && !requestBodyType.getName().startsWith("java.lang.")) {
            // 获取请求体类型的所有字段
            java.lang.reflect.Field[] fields = requestBodyType.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                // 跳过静态字段
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                
                String fieldName = field.getName();
                Type fieldType = field.getGenericType();
                String fieldTypeName = getFieldTypeName(fieldType);
                
                // 添加字段
                modelCode.append("    private ").append(fieldTypeName).append(" ").append(fieldName).append(";\n\n");
            }
            
            // 添加getter和setter
            for (java.lang.reflect.Field field : fields) {
                // 跳过静态字段
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                
                String fieldName = field.getName();
                Type fieldType = field.getGenericType();
                String fieldTypeName = getFieldTypeName(fieldType);
                String capitalizedFieldName = capitalizeFirstLetter(fieldName);
                
                // 添加getter
                if (fieldTypeName.equals("boolean")) {
                    modelCode.append("    public boolean is").append(capitalizedFieldName).append("() {\n");
                } else {
                    modelCode.append("    public ").append(fieldTypeName).append(" get").append(capitalizedFieldName).append("() {\n");
                }
                modelCode.append("        return ").append(fieldName).append(";\n");
                modelCode.append("    }\n\n");
                
                // 添加setter
                modelCode.append("    public void set").append(capitalizedFieldName).append("(").append(fieldTypeName).append(" ").append(fieldName).append(") {\n");
                modelCode.append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n");
                modelCode.append("    }\n\n");
            }
            
            // 递归生成所有字段类型的模型
            for (java.lang.reflect.Field field : fields) {
                Type fieldType = field.getGenericType();
                if (fieldType instanceof Class) {
                    generateNestedModels((Class<?>) fieldType, modelsDir);
                } else if (fieldType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) fieldType;
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    for (Type actualTypeArgument : actualTypeArguments) {
                        if (actualTypeArgument instanceof Class) {
                            generateNestedModels((Class<?>) actualTypeArgument, modelsDir);
                        } else if (actualTypeArgument instanceof ParameterizedType) {
                            Class<?> rawType = (Class<?>) ((ParameterizedType) actualTypeArgument).getRawType();
                            generateNestedModels(rawType, modelsDir);
                        }
                    }
                }
            }
        } else {
            // 如果没有@RequestBody参数或参数是基本类型，生成空的请求模型
            modelCode.append("    // 无请求体参数\n");
        }
        
        modelCode.append("}\n");
        
        // 写入文件
        java.nio.file.Files.write(outputFile.toPath(), modelCode.toString().getBytes("UTF-8"));
        getLog().info("生成请求模型类: " + outputFile.getAbsolutePath());
    }
    
    /**
     * 生成响应模型类
     */
    private void generateResponseModel(java.lang.reflect.Method method, String className, File modelsDir) throws Exception {
        File outputFile = new File(modelsDir, className + ".java");
        
        // 如果文件已存在，跳过
        if (outputFile.exists()) {
            getLog().info("模型类已存在，跳过生成: " + outputFile.getAbsolutePath());
            return;
        }
        
        StringBuilder modelCode = new StringBuilder();
        modelCode.append("package ").append(packageName).append(".models;\n\n");
        modelCode.append("import java.io.Serializable;\n");
        modelCode.append("\n");
        modelCode.append("import java.util.*;\n\n");
        
        modelCode.append("/**\n");
        modelCode.append(" * ").append(className).append(" 响应模型\n");
        modelCode.append(" */\n");
        modelCode.append("public class ").append(className).append(" implements Serializable {\n");
        modelCode.append("    private static final long serialVersionUID = 1L;\n\n");
        
        // 获取返回类型
        Type actualReturnType = method.getGenericReturnType();
        
        
        // 检查实际返回类型是否是自定义类型
        if (actualReturnType instanceof Class) {
            Class<?> actualReturnClass = (Class<?>) actualReturnType;
            // 如果是自定义类型（不是基本类型、包装类型、标准库类型或数组）
            if (!actualReturnClass.isPrimitive() && !actualReturnClass.getName().startsWith("java.lang.") && 
                !actualReturnClass.getName().startsWith("java.util.") && !actualReturnClass.isArray()) {
                // 获取原始响应类的字段
                java.lang.reflect.Field[] fields = actualReturnClass.getDeclaredFields();
                for (java.lang.reflect.Field field : fields) {
                    // 跳过静态字段和transient字段
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || 
                        java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                        continue;
                    }
                    
                    String fieldName = field.getName();
                    Type fieldType = field.getGenericType();
                    String fieldTypeName = getFieldTypeName(fieldType);
                    
                    // 添加字段
                    modelCode.append("    private ").append(fieldTypeName).append(" ").append(fieldName).append(";\n\n");
                }
                
                // 添加getter和setter
                for (java.lang.reflect.Field field : fields) {
                    // 跳过静态字段和transient字段
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || 
                        java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                        continue;
                    }
                    
                    String fieldName = field.getName();
                    Type fieldType = field.getGenericType();
                    String fieldTypeName = getFieldTypeName(fieldType);
                    String capitalizedFieldName = capitalizeFirstLetter(fieldName);
                    
                    // 添加getter
                    if (fieldTypeName.equals("boolean")) {
                        modelCode.append("    public boolean is").append(capitalizedFieldName).append("() {\n");
                    } else {
                        modelCode.append("    public ").append(fieldTypeName).append(" get").append(capitalizedFieldName).append("() {\n");
                    }
                    modelCode.append("        return ").append(fieldName).append(";\n");
                    modelCode.append("    }\n\n");
                    
                    // 添加setter
                    modelCode.append("    public void set").append(capitalizedFieldName).append("(").append(fieldTypeName).append(" ").append(fieldName).append(") {\n");
                    modelCode.append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n");
                    modelCode.append("    }\n\n");
                }
                
                // 递归生成所有字段类型的模型
                for (java.lang.reflect.Field field : fields) {
                    Type fieldType = field.getGenericType();
                    if (fieldType instanceof Class) {
                        generateNestedModels((Class<?>) fieldType, modelsDir);
                    } else if (fieldType instanceof ParameterizedType) {
                        ParameterizedType parameterizedType = (ParameterizedType) fieldType;
                        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                        for (Type actualTypeArgument : actualTypeArguments) {
                            if (actualTypeArgument instanceof Class) {
                                generateNestedModels((Class<?>) actualTypeArgument, modelsDir);
                            } else if (actualTypeArgument instanceof ParameterizedType) {
                                Class<?> rawType = (Class<?>) ((ParameterizedType) actualTypeArgument).getRawType();
                                generateNestedModels(rawType, modelsDir);
                            }
                        }
                    }
                }
            } 
        } 
        
        modelCode.append("}\n");
        
        // 写入文件
        java.nio.file.Files.write(outputFile.toPath(), modelCode.toString().getBytes("UTF-8"));
        getLog().info("生成响应模型类: " + outputFile.getAbsolutePath());
    }
    
    
    /**
     * 递归收集嵌套的业务对象模型类到全局集合
     */
    private void collectNestedModels(Type type) throws Exception {
        // 如果是Class类型
        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            
            // 如果是基本类型、包装类型或已经是Java标准库类型，跳过
            if (clazz.isPrimitive() || clazz.getName().startsWith("java.lang.") || 
                clazz.getName().startsWith("java.util.") || clazz.getName().startsWith("java.time.")) {
                return;
            }
            
            // 如果是数组，递归处理元素类型
            if (clazz.isArray()) {
                collectNestedModels(clazz.getComponentType());
                return;
            }
            
            // 如果已经在集合中，跳过
            if (models.contains(clazz)) {
                return;
            }
            
            // 添加到全局集合
            models.add(clazz);
            
            // 递归处理所有字段类型
            java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()) && 
                    !java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                    collectNestedModels(field.getGenericType());
                }
            }
            return;
        }
        
        // 如果是泛型类型，递归处理所有泛型参数
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            for (Type actualTypeArgument : actualTypeArguments) {
                collectNestedModels(actualTypeArgument);
            }
            
            // 处理原始类型
            Type rawType = parameterizedType.getRawType();
            collectNestedModels(rawType);
        }
    }
    
    /**
     * 递归生成嵌套的业务对象模型
     */
    private void generateNestedModels(Type type, File modelsDir) throws Exception {
        // 如果是Class类型
        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            
            // 如果是基本类型、包装类型或已经是Java标准库类型，跳过
            if (clazz.isPrimitive() || clazz.getName().startsWith("java.lang.") || 
                clazz.getName().startsWith("java.util.") || clazz.getName().startsWith("java.time.")) {
                return;
            }
            
            // 如果是数组，递归处理元素类型
            if (clazz.isArray()) {
                generateNestedModels(clazz.getComponentType(), modelsDir);
                return;
            }
            
            // 生成业务对象模型
            generateBusinessModelClass(clazz, modelsDir);
            return;
        }
        
        // 如果是泛型类型，递归处理所有泛型参数
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            for (Type actualTypeArgument : actualTypeArguments) {
                generateNestedModels(actualTypeArgument, modelsDir);
            }
            
            // 处理原始类型
            Type rawType = parameterizedType.getRawType();
            generateNestedModels(rawType, modelsDir);
        }
    }
    
    /**
     * 生成业务对象模型类
     */
    private void generateBusinessModelClass(Class<?> type, File modelsDir) throws Exception {
        String className = type.getSimpleName();
        File outputFile = new File(modelsDir, className + ".java");
        
        // 如果文件已存在，跳过
        if (outputFile.exists()) {
            getLog().info("业务对象模型类已存在，跳过生成: " + outputFile.getAbsolutePath());
            return;
        }
        
        StringBuilder modelCode = new StringBuilder();
        modelCode.append("package ").append(packageName).append(".models;\n\n");
        modelCode.append("import java.io.Serializable;\n");
        modelCode.append("\n");
        modelCode.append("import java.util.*;\n\n");
        
        modelCode.append("/**\n");
        modelCode.append(" * ").append(className).append(" 业务对象模型\n");
        modelCode.append(" */\n");
        modelCode.append("public class ").append(className).append(" implements Serializable {\n");
        modelCode.append("    private static final long serialVersionUID = 1L;\n\n");
        
        // 获取所有字段
        java.lang.reflect.Field[] fields = type.getDeclaredFields();
        for (java.lang.reflect.Field field : fields) {
            // 跳过静态字段和 transient 字段
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || 
                java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            
            String fieldName = field.getName();
            Type fieldType = field.getGenericType();
            String fieldTypeName = getFieldTypeName(fieldType);
            
            // 添加字段
            modelCode.append("    private ").append(fieldTypeName).append(" ").append(fieldName).append(";\n\n");
        }
        
        // 添加getter和setter
        for (java.lang.reflect.Field field : fields) {
            // 跳过静态字段和 transient 字段
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || 
                java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            
            String fieldName = field.getName();
            Type fieldType = field.getGenericType();
            String fieldTypeName = getFieldTypeName(fieldType);
            String capitalizedFieldName = capitalizeFirstLetter(fieldName);
            
            // 添加getter
            if (fieldTypeName.equals("boolean")) {
                modelCode.append("    public boolean is").append(capitalizedFieldName).append("() {\n");
            } else {
                modelCode.append("    public ").append(fieldTypeName).append(" get").append(capitalizedFieldName).append("() {\n");
            }
            modelCode.append("        return ").append(fieldName).append(";\n");
            modelCode.append("    }\n\n");
            
            // 添加setter
            modelCode.append("    public void set").append(capitalizedFieldName).append("(").append(fieldTypeName).append(" ").append(fieldName).append(") {\n");
            modelCode.append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n");
            modelCode.append("    }\n\n");
        }
        
        modelCode.append("}\n");
        
        // 写入文件
        java.nio.file.Files.write(outputFile.toPath(), modelCode.toString().getBytes("UTF-8"));
        getLog().info("生成业务对象模型类: " + outputFile.getAbsolutePath());
        
        // 递归生成所有字段类型的模型
        for (java.lang.reflect.Field field : fields) {
            Type fieldType = field.getGenericType();
            if (fieldType instanceof Class) {
                generateNestedModels((Class<?>) fieldType, modelsDir);
            } else if (fieldType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) fieldType;
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                for (Type actualTypeArgument : actualTypeArguments) {
                    if (actualTypeArgument instanceof Class) {
                        generateNestedModels((Class<?>) actualTypeArgument, modelsDir);
                    } else if (actualTypeArgument instanceof ParameterizedType) {
                        Class<?> rawType = (Class<?>) ((ParameterizedType) actualTypeArgument).getRawType();
                        generateNestedModels(rawType, modelsDir);
                    }
                }
            }
        }
    }
    
    /**
     * 获取字段类型的字符串表示
     */
    private String getFieldTypeName(Type type) {
        if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isArray()) {
                return getFieldTypeName(clazz.getComponentType()) + "[]";
            }
            return clazz.getSimpleName();
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            StringBuilder sb = new StringBuilder(rawType.getSimpleName());
            sb.append("<");
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            for (int i = 0; i < actualTypeArguments.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(getFieldTypeName(actualTypeArguments[i]));
            }
            sb.append(">");
            return sb.toString();
        }
        return type.toString();
    }
    
    /**
     * 将字符串首字母大写
     */
    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
    
    /**
     * 检查方法是否是RequestMapping方法
     */
    private boolean isRequestMappingMethod(java.lang.reflect.Method method) {
        return method.isAnnotationPresent(RequestMapping.class) ||
               method.isAnnotationPresent(GetMapping.class) ||
               method.isAnnotationPresent(PostMapping.class) ||
               method.isAnnotationPresent(PutMapping.class) ||
               method.isAnnotationPresent(DeleteMapping.class) ||
               method.isAnnotationPresent(PatchMapping.class);
    }
}