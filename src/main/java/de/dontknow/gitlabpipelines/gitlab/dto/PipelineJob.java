package de.dontknow.gitlabpipelines.gitlab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PipelineJob(int id, String stage, JobStatus status, String name) {
}
