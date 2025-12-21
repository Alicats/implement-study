package cn.xej.api.sdk.java;

import java.util.ArrayList;
import java.util.List;

/**
 * 模型信息类，用于存储模型类的相关信息，供模板引擎使用
 */
public class ModelInfo {
    private String className;
    private List<FieldInfo> fields = new ArrayList<>();
    private boolean hasFields;


    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<FieldInfo> getFields() {
        return fields;
    }

    public void setFields(List<FieldInfo> fields) {
        this.fields = fields;
        this.hasFields = !fields.isEmpty();
    }

    public void addField(FieldInfo field) {
        this.fields.add(field);
        this.hasFields = true;
    }

    public boolean isHasFields() {
        return hasFields;
    }

    /**
     * 字段信息类
     */
    public static class FieldInfo {
        private String name;
        private String typeName;
        private boolean isBoolean;

        public FieldInfo(String name, String typeName) {
            this.name = name;
            this.typeName = typeName;
            this.isBoolean = "boolean".equals(typeName);
        }

        // Getters
        public String getName() {
            return name;
        }

        public String getTypeName() {
            return typeName;
        }

        public boolean isBoolean() {
            return isBoolean;
        }

        public String getCapitalizedName() {
            if (name == null || name.isEmpty()) {
                return name;
            }
            return Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
    }
}