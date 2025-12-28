package cn.xej.api.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class AbstractModel {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    /**
     * 将对象转换为JSON字符串
     *
     * @return JSON字符串
     */
    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }

    /**
     * 将JSON字符串转换为指定类型的对象
     *
     * @param json  JSON字符串
     * @param clazz 目标类型
     * @param <T>   泛型参数
     * @return 转换后的对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert JSON to object", e);
        }
    }

    /**
     * 将对象转换为Map
     *
     * @return Map对象
     */
    public Map<String, Object> toMap() {
        try {
            // 先转换为JSON，再转换为Map，这样可以利用Jackson的序列化逻辑
            String json = toJson();
            return OBJECT_MAPPER.readValue(json, Map.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert object to Map", e);
        }
    }

    /**
     * 从Map构造对象
     *
     * @param map   Map对象
     * @param clazz 目标类型
     * @param <T>   泛型参数
     * @return 构造后的对象
     */
    public static <T extends AbstractModel> T fromMap(Map<String, Object> map, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.convertValue(map, clazz);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Failed to convert Map to object", e);
        }
    }

    /**
     * 检查对象是否为null
     *
     * @param obj 要检查的对象
     * @return 是否为null
     */
    protected boolean isNull(Object obj) {
        return obj == null;
    }

    /**
     * 检查字符串是否为空
     *
     * @param str 要检查的字符串
     * @return 是否为空
     */
    protected boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * 检查集合是否为空
     *
     * @param collection 要检查的集合
     * @return 是否为空
     */
    protected boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 检查数组是否为空
     *
     * @param array 要检查的数组
     * @return 是否为空
     */
    protected boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    /**
     * 检查Map是否为空
     *
     * @param map 要检查的Map
     * @return 是否为空
     */
    protected boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * 获取对象的所有字段值
     *
     * @return 字段名和值的映射
     */
    @JsonIgnore
    public Map<String, Object> getFields() {
        Map<String, Object> fields = new LinkedHashMap<>();
        Class<?> clazz = this.getClass();
        
        // 遍历所有父类直到Object
        while (clazz != null && !clazz.equals(Object.class)) {
            for (Field field : clazz.getDeclaredFields()) {
                // 跳过serialVersionUID字段
                if ("serialVersionUID".equals(field.getName())) {
                    continue;
                }
                
                field.setAccessible(true);
                try {
                    Object value = field.get(this);
                    if (value != null) {
                        fields.put(field.getName(), value);
                    }
                } catch (IllegalAccessException e) {
                    // 忽略无法访问的字段
                }
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    /**
     * 转换为字符串，便于日志输出
     *
     * @return 字符串表示
     */
    @Override
    public String toString() {
        return toJson();
    }

    /**
     * 计算哈希值
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(getFields());
    }

    /**
     * 比较对象是否相等
     *
     * @param obj 要比较的对象
     * @return 是否相等
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AbstractModel that = (AbstractModel) obj;
        return Objects.equals(this.getFields(), that.getFields());
    }

    /**
     * 获取指定字段的值
     *
     * @param fieldName 字段名
     * @return 字段值
     */
    public Object getFieldValue(String fieldName) {
        try {
            Field field = findField(getClass(), fieldName);
            if (field == null) {
                return null;
            }
            field.setAccessible(true);
            return field.get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get field value", e);
        }
    }

    /**
     * 设置指定字段的值
     *
     * @param fieldName 字段名
     * @param value     字段值
     */
    public void setFieldValue(String fieldName, Object value) {
        try {
            Field field = findField(getClass(), fieldName);
            if (field == null) {
                throw new RuntimeException("Field not found: " + fieldName);
            }
            field.setAccessible(true);
            field.set(this, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set field value", e);
        }
    }

    /**
     * 查找指定名称的字段，包括父类
     *
     * @param clazz     类对象
     * @param fieldName 字段名
     * @return 字段对象，如果未找到则返回null
     */
    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null && !clazz.equals(Object.class)) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // 继续查找父类
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 转换为查询字符串
     *
     * @return 查询字符串，例如：param1=value1&param2=value2
     */
    public String toQueryString() {
        return getFields().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue().toString())
                .collect(Collectors.joining("&"));
    }
}
