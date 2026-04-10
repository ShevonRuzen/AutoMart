package com.shehan.automart.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private String id;
    private String name;
    private String phoneNumber;
    private String email;
    //    private String password;
    private String profileImage;
    private String fcm_token;

}
