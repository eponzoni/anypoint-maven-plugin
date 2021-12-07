package com.mulesoft.meetups;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class AnypointLogin {

    private String username;
    private String password;
}
