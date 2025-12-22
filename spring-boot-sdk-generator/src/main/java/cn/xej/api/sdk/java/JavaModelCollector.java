package cn.xej.api.sdk.java;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;


/**
 * 模型收集器，负责扫描API端点，收集所有请求和响应类型
 */
public class JavaModelCollector {

    private final Set<MethodInfo> methodInfoRegistry;
    private final Set<ModelInfo> modelInfoRegistry;
    private final Set<Class<?>> modelRegistry;
    

    public JavaModelCollector() {
        this.methodInfoRegistry = new HashSet<>();
        this.modelInfoRegistry = new HashSet<>();
        this.modelRegistry = new HashSet<>();
    }

    

    /**
     * 收集方法的请求和响应模型
     * @param method API方法
     */
    public void registerModels(java.lang.reflect.Method method) {
        // 收集请求体类型
        collectRequestBodyType(method);
        
        // 收集返回类型
        collectReturnType(method);
    }

    /**
     * 收集请求体类型
     * @param method API方法
     */
    private void collectRequestBodyType(java.lang.reflect.Method method) {
        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters) {
            if (parameter.isAnnotationPresent(RequestBody.class)) {
                collectNestedModels(parameter.getType(), false);
                break;
            }
        }
    }

    /**
     * 收集返回类型
     * @param method API方法
     */
    private void collectReturnType(java.lang.reflect.Method method) {
        collectNestedModels(method.getGenericReturnType(), true);
    }

    /**
     * 递归收集嵌套模型
     * @param type 类型
     */
    public void collectNestedModels(Type type, boolean addRequestId) {
        if (type == null) {
            return;
        }

        // 处理Class类型
        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            
            // 如果是基本类型、包装类型或标准库类型，跳过
            if (isStandardType(clazz)) {
                return;
            }

            // 如果已经注册，跳过
            if (modelRegistry.contains(clazz)) {
                return;
            }

            // 注册模型
            modelRegistry.add(clazz);

            // 处理数组类型
            if (clazz.isArray()) {
                collectNestedModels(clazz.getComponentType(), false);
                return;
            }

            // 递归处理字段类型
            try {
                // 创建模型信息对象
                ModelInfo modelInfo = new ModelInfo();
                modelInfo.setClassName(clazz.getSimpleName());
                if (addRequestId) {
                    modelInfo.addField(new ModelInfo.FieldInfo("requestId", "String"));
                }
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    if (!field.isSynthetic() && !java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
                            !java.lang.reflect.Modifier.isTransient(field.getModifiers())) {

                        String fieldName = field.getName();
                        Type fieldType = field.getGenericType();
                        String fieldTypeName = getFieldTypeName(fieldType);
                        
                        // 添加字段信息
                        modelInfo.addField(new ModelInfo.FieldInfo(fieldName, fieldTypeName));
                        modelInfoRegistry.add(modelInfo);

                        collectNestedModels(field.getGenericType(), false);
                    }
                }
            } catch (Exception e) {
            }
            return;
        }

        // 处理泛型类型
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            
            // 处理所有泛型参数
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            for (Type actualTypeArgument : actualTypeArguments) {
                collectNestedModels(actualTypeArgument, false);
            }

            // 处理原始类型
            Type rawType = parameterizedType.getRawType();
            collectNestedModels(rawType, false);
        }
    }

    /**
     * 判断是否为标准类型（基本类型、包装类型或Java标准库类型）
     * @param clazz 类
     * @return 是否为标准类型
     */
    private boolean isStandardType(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz.getName().startsWith("java.lang.") ||
                clazz.getName().startsWith("java.util.") ||
                clazz.getName().startsWith("java.time.") ||
                clazz.getName().startsWith("java.io.") ||
                clazz.getName().startsWith("java.math.") ||
                clazz.getName().startsWith("java.net.");
    }

    /**
     * 获取收集到的所有模型类
     * @return 模型类集合
     */
    public Set<ModelInfo> getCollectedModels() {
        return new HashSet<>(modelInfoRegistry);
    }

    public void registerMethod(Method method) {
         MethodInfo methodModel = new MethodInfo();
        
        // 获取映射信息
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        String mappingValue = mapping.value().length > 0 ? mapping.value()[0] : "";
        String httpMethod = "POST"; // 默认POST

        // 组合完整的URL路径
        String fullMapping = mappingValue;
        
        // 设置方法基本信息
        String methodName = method.getName();
        String returnType = method.getReturnType().getSimpleName();
        
        // 生成参数列表
        Class<?>[] paramTypes = method.getParameterTypes();
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        StringBuilder paramsBuilder = new StringBuilder();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) paramsBuilder.append(", ");
            paramsBuilder.append(paramTypes[i].getSimpleName()).append(" ").append(parameters[i].getName());
        }
        String parametersStr = paramsBuilder.toString();
        
        // 生成请求体代码
        StringBuilder requestBodyBuilder = new StringBuilder();
        if (paramTypes.length > 0) {
            requestBodyBuilder.append("        requestBody = RequestBody.create(");
            requestBodyBuilder.append("objectMapper.writeValueAsString(");
            requestBodyBuilder.append(parameters[0].getName());
            requestBodyBuilder.append("), MediaType.parse(\"application/json; charset=utf-8\"));\n\n");
        }
        
        // 设置方法模型属性
        methodModel.setReturnType(returnType);
        methodModel.setMethodName(methodName);
        methodModel.setFullMapping(fullMapping);
        methodModel.setHttpMethod(httpMethod);
        methodModel.setParameters(parametersStr);
        methodModel.setRequestBodyCode(requestBodyBuilder.toString());
        methodInfoRegistry.add(methodModel);
    }

    public Set<MethodInfo> getCollectedMethods() {
        return new HashSet<>(methodInfoRegistry);
    }

    /**
     * 清空模型注册表
     */
    public void clear() {
        methodInfoRegistry.clear();
        modelInfoRegistry.clear();
        modelRegistry.clear();
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
}