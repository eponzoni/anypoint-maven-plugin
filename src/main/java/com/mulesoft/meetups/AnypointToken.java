package com.mulesoft.meetups;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class AnypointToken {

    private String accessToken;
    private String tokenType;
    private String redirectUrl;
}
