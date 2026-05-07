package com.clockshore.aooai.dto;

import com.clockshore.aooai.entity.AppUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    //前端传入
    private String userName;
    private String password;

    public static UserDto from(AppUser u){
        if (u == null) {
            return null;
        }
        return UserDto.builder()
                .userName(u.getUserName())
                .password(u.getPassword())
                .build();
    }
}
