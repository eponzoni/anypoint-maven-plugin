package com.mulesoft.meetups;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class AnypointExchangeAsset {

    private String groupId;
    private String assetId;
    private String version;
    private String productAPIVersion;
    private String name;
    private String type;
    private String status;
}
