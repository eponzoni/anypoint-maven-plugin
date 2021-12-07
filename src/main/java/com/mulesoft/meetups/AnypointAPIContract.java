package com.mulesoft.meetups;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class AnypointAPIContract {

    private String apiId;
    private String environmentId;
    private String instanceType;
    private Long requestedTierId;
    private Boolean acceptedTerms;
    private String organizationId;
    private String groupId;
    private String assetId;
    private String version;
    private String versionGroup;
}
