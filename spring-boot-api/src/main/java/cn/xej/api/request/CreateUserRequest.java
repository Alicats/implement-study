package cn.xej.api.request;

import javax.validation.constraints.NotBlank;

public class CreateUserRequest {
    @NotBlank(message = "name not blank")
    public String name;
    @NotBlank(message = "email not blank")
    public String email;
}
