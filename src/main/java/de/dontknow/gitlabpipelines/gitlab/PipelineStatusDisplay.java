package de.dontknow.gitlabpipelines.gitlab;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBColor;
import com.intellij.util.IconUtil;
import de.dontknow.gitlabpipelines.gitlab.dto.PipelineDto;
import de.dontknow.gitlabpipelines.gitlab.dto.PipelineJob;
import git4idea.GitReference;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import git4idea.repo.GitRepositoryManager;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class PipelineStatusDisplay {

    private final HashMap<Integer, JobPanelDto> jobs = new HashMap<>();
    private final HashSet<PipelineDto> pipelines = new HashSet<>();
    private Thread watcher;

    private final GitlabProjectConnection gitlabProjectConnection;

    public PipelineStatusDisplay(GitlabProjectConnection gitlabProjectConnection) {
        this.gitlabProjectConnection = gitlabProjectConnection;
    }

    public void startWatcher(Project project, JPanel rootPanel) {
        var connection = project.getMessageBus().connect();
        AtomicReference<String> lastBranch = new AtomicReference<>("");
        connection.subscribe(GitRepository.GIT_REPO_CHANGE, (GitRepositoryChangeListener) repository -> {
            if (!repository.getCurrentBranch().getName().equals(lastBranch.get())) {
                displayLoading(rootPanel);
                lastBranch.set(repository.getCurrentBranch().getName());

            }
        });

        watcher = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                updateActivePipeline(project);
                updatePipelineStatus(rootPanel);
                updateJobStates();
            }
        });
        if (!watcher.isAlive()) watcher.start();
    }


    private void updateActivePipeline(Project project) {
        try {
            if (gitlabProjectConnection.isValid()) {
                var gitRepo = getGitRepo(project);
                if (gitRepo.isEmpty()) return;
                var gitlabProject = gitlabProjectConnection.getProject(gitRepo.get());
                PipelineDto master = gitlabProjectConnection.getLatestPipeline(gitlabProject, gitRepo.map(GitRepository::getCurrentBranch).map(GitReference::getName).orElse("master"));
                if (master != null) {
                    if (!pipelines.contains(master)) {
                        pipelines.clear();
                        pipelines.add(master);
                    }
                }
            } else {
                pipelines.clear();
                jobs.clear();
            }
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void updatePipelineStatus(JPanel root) {
        try {
            var first = pipelines.stream().findFirst();
            if (first.isPresent()) {
                List<PipelineJob> jobsFromPipeline = gitlabProjectConnection.getJobsFromPipeline(first.get());
                if (jobsFromPipeline == null) return;
                root.removeAll();
                jobs.clear();
                String lastStage = null;
                for (PipelineJob job : jobsFromPipeline) {
                    if (!Objects.equals(lastStage, job.stage())) {
                        if (lastStage != null) {
                            var icon = IconLoader.getIcon("Icons/arrow-left.svg", getClass());
                            root.add(new JLabel(IconUtil.scale(icon, .8f)));
                        }
                        lastStage = job.stage();
                    }
                    var jLabel = new JLabel();
                    root.add(jLabel);
                    updateLabel(jLabel, job);
                    jobs.put(job.id(), new JobPanelDto(jLabel, job, first.get().project_id()));
                }
            } else {
                displayLoading(root);
            }

        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void displayLoading(JPanel root) {
        root.removeAll();
        root.add(new JLabel("fetching..."));
    }

    private void updateJobStates() {
        List<JobPanelDto> values = new ArrayList<>(jobs.values());
        values.forEach(pipelineJob -> {
            try {
                PipelineJob job = gitlabProjectConnection.getJob(pipelineJob.projectId, pipelineJob.pipelineJob.id());
                if (job == null) return;
                updateLabel(pipelineJob.label, job);
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void updateLabel(JLabel label, PipelineJob job) {
        switch (job.status()) {
            case waiting_for_resource -> {
                var icon = IconLoader.getIcon("/Icons/status-waiting-for-resource.svg", getClass());
                icon = IconUtil.colorize(icon, JBColor.GRAY);
                label.setIcon(icon);
            }
            case success -> {
                var icon = IconLoader.getIcon("/Icons/status_success_solid.svg", getClass());
                icon = IconUtil.colorize(icon, Color.GREEN);
                label.setIcon(icon);
            }
            case pending -> {
                var icon = IconLoader.getIcon("/Icons/status-paused.svg", getClass());
                icon = IconUtil.colorize(icon, JBColor.YELLOW);
                label.setIcon(icon);
            }
            case failed -> {
                var icon = IconLoader.getIcon("/Icons/status-failed.svg", getClass());
                icon = IconUtil.colorize(icon, JBColor.RED);
                label.setIcon(icon);
            }
            case running -> {
                var icon = IconLoader.getIcon("/Icons/status-running.svg", getClass());
                icon = IconUtil.colorize(icon, JBColor.BLUE);
                label.setIcon(icon);
            }
            case created -> {
                var icon = IconLoader.getIcon("/Icons/status-waiting.svg", getClass());
                icon = IconUtil.colorize(icon, JBColor.YELLOW);
                label.setIcon(icon);
            }
        }
        label.setToolTipText(job.name());
        label.revalidate();
        label.repaint();
    }

    public Optional<GitRepository> getGitRepo(Project project) {
        var repositoryManager = GitRepositoryManager.getInstance(project);
        List<GitRepository> repositories = repositoryManager.getRepositories();
        if (repositories.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(repositories.get(0));
    }


    private record JobPanelDto(JLabel label, PipelineJob pipelineJob, long projectId) {

    }
}
