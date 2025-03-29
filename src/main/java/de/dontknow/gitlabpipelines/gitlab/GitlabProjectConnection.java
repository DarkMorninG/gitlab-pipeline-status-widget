package de.dontknow.gitlabpipelines.gitlab;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.dontknow.gitlabpipelines.config.GitlabConnectionStorage;
import de.dontknow.gitlabpipelines.gitlab.dto.PipelineDto;
import de.dontknow.gitlabpipelines.gitlab.dto.PipelineJob;
import de.dontknow.gitlabpipelines.gitlab.dto.ProjectDto;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class GitlabProjectConnection {

    private final ObjectMapper mapper = new ObjectMapper();

    public ProjectDto getProject(GitRepository gitRepository) throws URISyntaxException, IOException, InterruptedException {
        GitlabConfig gitlabConfig = getGitlabConfig();
        if (!gitlabConfig.isValied) return null;
        var encodedProjectPath = getGitRepoOriginPath(gitRepository);
        if (encodedProjectPath.isEmpty()) return null;

        HttpRequest projectListRequest = HttpRequest.newBuilder()
                .uri(new URI(String.format("%s/api/v4/projects/%s", gitlabConfig.gitlabUrl(), encodedProjectPath.get())))
                .GET()
                .header("Private-Token", gitlabConfig.accesToken())
                .build();
        HttpResponse<String> projectList = HttpClient.newHttpClient().send(projectListRequest, HttpResponse.BodyHandlers.ofString());
        if (projectList.statusCode() == 200) {
            return mapper.readValue(projectList.body(), ProjectDto.class);
        }
        return null;
    }

    public PipelineDto getLatestPipeline(ProjectDto projectDto, String branch) throws URISyntaxException, IOException, InterruptedException {
        var gitlabConfig = getGitlabConfig();
        if (!gitlabConfig.isValied) return null;

        HttpRequest projectListRequest = HttpRequest.newBuilder()
                .uri(new URI(String.format("%s/api/v4/projects/%s/pipelines?ref=%s", gitlabConfig.gitlabUrl, projectDto.id(), branch)))
                .GET()
                .header("Private-Token", gitlabConfig.accesToken())
                .build();
        HttpResponse<String> projectList = HttpClient.newHttpClient().send(projectListRequest, HttpResponse.BodyHandlers.ofString());
        if (projectList.statusCode() == 200) {
            var pipelineDtos = mapper.readValue(projectList.body(), new TypeReference<List<PipelineDto>>() {
            });
            if (!pipelineDtos.isEmpty()) {
                return pipelineDtos.get(0);
            }
        }
        return null;
    }

    public List<PipelineJob> getJobsFromPipeline(PipelineDto pipelineDto) throws URISyntaxException, IOException, InterruptedException {
        var gitlabConfig = getGitlabConfig();
        if (!gitlabConfig.isValied) return null;

        HttpRequest projectListRequest = HttpRequest.newBuilder()
                .uri(new URI(gitlabConfig.gitlabUrl + "/api/v4/projects/" + pipelineDto.project_id() + "/pipelines/" + pipelineDto.id() + "/jobs"))
                .GET()
                .header("Private-Token", gitlabConfig.accesToken())
                .build();
        HttpResponse<String> projectList = HttpClient.newHttpClient().send(projectListRequest, HttpResponse.BodyHandlers.ofString());
        if (projectList.statusCode() == 200) {
            return mapper.readValue(projectList.body(), new TypeReference<>() {
            });
        }
        return null;
    }

    public PipelineJob getJob(long projectId, int jobsID) throws URISyntaxException, IOException, InterruptedException {
        GitlabConfig gitlabConfig = getGitlabConfig();
        if (!gitlabConfig.isValied) return null;

        HttpRequest projectListRequest = HttpRequest.newBuilder()
                .uri(new URI(gitlabConfig.gitlabUrl + "/api/v4/projects/" + projectId + "/jobs/" + jobsID))
                .GET()
                .header("Private-Token", gitlabConfig.accesToken())
                .build();
        HttpResponse<String> projectList = HttpClient.newHttpClient().send(projectListRequest, HttpResponse.BodyHandlers.ofString());
        if (projectList.statusCode() == 200) {
            return mapper.readValue(projectList.body(), PipelineJob.class);
        }
        return null;
    }

    boolean isValidURL(String toTest, String token) {
        try {
            HttpRequest projectListRequest = HttpRequest.newBuilder()
                    .uri(new URI(toTest + "/api/v4/projects/"))
                    .GET()
                    .header("Private-Token", token)
                    .build();
            var httpResponse = HttpClient.newHttpClient().send(projectListRequest, HttpResponse.BodyHandlers.ofString());
            return httpResponse.statusCode() == HttpURLConnection.HTTP_OK;
        } catch (URISyntaxException | IOException | InterruptedException e) {
            return false;
        }
    }

    private Optional<String> getGitRepoOriginPath(GitRepository gitRepository) {
        var originRemote = gitRepository.getRemotes().stream()
                .filter(remote -> "origin".equals(remote.getName()))
                .findFirst()
                .orElse(gitRepository.getRemotes().stream().findFirst().orElse(null));
        if (originRemote == null) return Optional.empty();
        if (originRemote.getFirstUrl() == null) return Optional.empty();
        var projectPath = extractProjectPath(originRemote.getFirstUrl());
        if (projectPath == null) return Optional.empty();
        return Optional.of(URLEncoder.encode(projectPath, StandardCharsets.UTF_8));
    }


    private GitlabProjectConnection.@NotNull GitlabConfig getGitlabConfig() {
        GitlabConnectionStorage configInstance = GitlabConnectionStorage.getInstance();
        if (configInstance.getState() == null) return new GitlabConfig(null, null, false);
        String gitlabUrl = configInstance.getState().gitlabUrl;
        String accesToken = configInstance.getState().privateToken;
        if (accesToken == null || accesToken.isEmpty()) return new GitlabConfig(null, null, false);
        if (gitlabUrl == null) return new GitlabConfig(null, null, false);
        if (!isValidURL(gitlabUrl, accesToken)) return new GitlabConfig(null, null, false);
        return new GitlabConfig(gitlabUrl, accesToken, true);
    }

    public boolean isValid() {
        return getGitlabConfig().isValied();
    }

    private String extractProjectPath(String gitPullUrl) {
        if (gitPullUrl.startsWith("git@")) {
            int colonIndex = gitPullUrl.indexOf(':');
            String path = gitPullUrl.substring(colonIndex + 1);
            if (path.endsWith(".git")) {
                path = path.substring(0, path.length() - 4);
            }
            return path;
        }
        if (gitPullUrl.startsWith("https://") || gitPullUrl.startsWith("http://")) {
            URI uri = URI.create(gitPullUrl);
            String path = uri.getPath();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (path.endsWith(".git")) {
                path = path.substring(0, path.length() - 4);
            }
            return path;
        }
        return null;
    }


    private record GitlabConfig(String gitlabUrl, String accesToken, boolean isValied) {
    }
}
