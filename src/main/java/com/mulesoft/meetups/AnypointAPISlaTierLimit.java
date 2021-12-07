package com.mulesoft.meetups;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class AnypointAPISlaTierLimit {

    private Boolean visible;
    private Integer maximumRequests;
    private Integer timePeriodInMilliseconds;
}
