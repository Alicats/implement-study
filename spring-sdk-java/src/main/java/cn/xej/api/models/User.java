package cn.xej.api.models;

import java.io.Serializable;


/**
 * User 业务对象模型
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String email;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}
