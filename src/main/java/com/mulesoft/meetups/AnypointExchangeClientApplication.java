package com.mulesoft.meetups;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
public class AnypointExchangeClientApplication {

    private String description;
    private String name;
    private final List<String> grantTypes = new ArrayList<>();
    private final List<String> redirectUri = new ArrayList<>();
    private String url;
    private String masterOrganizationId;
    private String clientId;
    private String clientSecret;
    private Long id;
}
