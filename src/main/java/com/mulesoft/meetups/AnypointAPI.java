package com.mulesoft.meetups;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Builder
@Getter
@Setter
public class AnypointAPI {

    private String masterOrganizationId;
    private String organizationId;
    private Long id;
    private String instanceLabel;
    private String groupId;
    private String assetId;
    private String assetVersion;
    private String productVersion;
    private String description;
    private boolean deprecated;
    private Date lastActiveDate;
    private String endpointUri;
    private String environmentId;
    private boolean isPublic;
    private String stage;
    private String technology;
    private int lastActiveDelta;
    private boolean pinned;
    private int activeContractsCount;
    private String autodiscoveryInstanceName;
    private final AnypointAPIAsset asset;
}
