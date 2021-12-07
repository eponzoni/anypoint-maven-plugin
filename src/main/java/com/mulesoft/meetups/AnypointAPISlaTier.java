package com.mulesoft.meetups;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
public class AnypointAPISlaTier {

    private String status;
    private Boolean autoApprove;
    private List<AnypointAPISlaTierLimit> limits;
    private String name;
    private String description;
    private Long apiVersionId;
}
