package ${packageName}.models;

import java.io.Serializable;
import java.util.*;

/**
 * ${model.className} 模型类
 */
public class ${model.className} implements Serializable {
    private static final long serialVersionUID = 1L;

    <#if model.hasFields>
    <#list model.fields as field>
    private ${field.typeName} ${field.name};
    
    </#list>
    
    <#list model.fields as field>
    <#if field.boolean>
    public boolean is${field.capitalizedName}() {
    <#else>
    public ${field.typeName} get${field.capitalizedName}() {
    </#if>
        return ${field.name};
    }
    
    public void set${field.capitalizedName}(${field.typeName} ${field.name}) {
        this.${field.name} = ${field.name};
    }
    
    </#list>
    <#else>
    // 无字段定义
    </#if>
}
