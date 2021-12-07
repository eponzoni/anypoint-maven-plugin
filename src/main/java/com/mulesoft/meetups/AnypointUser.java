package com.mulesoft.meetups;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class AnypointUser {

    private String id;
    private String organizationId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String username;
}
