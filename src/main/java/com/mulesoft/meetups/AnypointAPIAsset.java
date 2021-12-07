package com.mulesoft.meetups;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class AnypointAPIAsset {

    private String masterOrganizationId;
    private String organizationId;
    private long id;
    private String name;
    private String exchangeAssetName;
    private String groupId;
    private String assetId;
}
