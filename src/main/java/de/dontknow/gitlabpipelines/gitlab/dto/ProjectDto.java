package de.dontknow.gitlabpipelines.gitlab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProjectDto(String name, long id) {
}
