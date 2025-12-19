package cn.xej.api.response;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class CreateUserResponse {
    public String userId;


    public CreateUserResponse(String userId) {
        this.userId = userId;
    }

}
