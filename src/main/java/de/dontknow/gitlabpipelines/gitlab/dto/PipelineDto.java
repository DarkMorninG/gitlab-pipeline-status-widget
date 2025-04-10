package de.dontknow.gitlabpipelines.gitlab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PipelineDto(int id, int project_id, GitlabStatus status) {
}
