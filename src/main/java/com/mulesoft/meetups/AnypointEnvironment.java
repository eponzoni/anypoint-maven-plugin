package com.mulesoft.meetups;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class AnypointEnvironment {

    private String id;
    private String name;
    private String organizationId;
    private Boolean isProduction;
    private String type;
    private String clientId;
}
