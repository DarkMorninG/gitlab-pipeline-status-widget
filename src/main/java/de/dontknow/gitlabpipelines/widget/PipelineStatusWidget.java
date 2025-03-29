package de.dontknow.gitlabpipelines.widget;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.ui.JBColor;
import com.intellij.util.IconUtil;
import de.dontknow.gitlabpipelines.gitlab.GitlabProjectConnection;
import de.dontknow.gitlabpipelines.gitlab.dto.PipelineDto;
import de.dontknow.gitlabpipelines.gitlab.dto.PipelineJob;
import de.dontknow.gitlabpipelines.gitlab.dto.ProjectDto;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;

public class PipelineStatusWidget implements CustomStatusBarWidget {
    private final GitlabProjectConnection gitlabProjectConnection;
    private final Project project;


    private final HashMap<Integer, JobPanelDto> jobs = new HashMap<>();
    private final HashSet<PipelineDto> pipelines = new HashSet<>();
    private ProjectDto gitlabProject;

    public PipelineStatusWidget(GitlabProjectConnection gitlabProjectConnection, @NotNull Project project) {
        this.gitlabProjectConnection = gitlabProjectConnection;
        this.project = project;
    }

    public String getGitOriginUrl(Project project) {
        // Get the GitRepositoryManager instance
        var repositoryManager = GitRepositoryManager.getInstance(project);
        List<GitRepository> repositories = repositoryManager.getRepositories();
        if (repositories.isEmpty()) {
            return null;
        }
        // For this example, we use the first repository
        var repository = repositories.get(0);
        // Find the remote named "origin" if available; otherwise, take the first remote
        var originRemote = repository.getRemotes().stream()
                .filter(remote -> "origin".equals(remote.getName()))
                .findFirst()
                .orElse(repository.getRemotes().stream().findFirst().orElse(null));
        return originRemote != null ? originRemote.getFirstUrl() : null;
    }

    @NotNull
    @Override
    public String ID() {
        return "myCustomWidget";
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        // Called when the widget is added to the status bar.
    }

    @Override
    public void dispose() {
        // Clean up resources if necessary.
    }

    @Override
    public JComponent getComponent() {
        var root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.X_AXIS));
        try {
            System.out.println(getGitOriginUrl(project));
            setProjectIfConfigIsCorrect(root).start();
            GitlabPipelineStatusUpdateRunner(root).start();
            UpdateJobStates().start();

        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return root;
    }

    private Thread setProjectIfConfigIsCorrect(JPanel root) throws URISyntaxException, IOException, InterruptedException {
        return new Thread(() -> {
            try {
                while (true) {
                    if (gitlabProjectConnection.isValid()) {
                        var gitlabProject = gitlabProjectConnection.getProject();
                        PipelineDto master = gitlabProjectConnection.getLatestPipeline(gitlabProject, "master");
                        if (master != null) {
                            if (!pipelines.contains(master)) {
                                pipelines.clear();
                                pipelines.add(master);
                            }
                        }
                    } else {
                        gitlabProject = null;
                        pipelines.clear();
                        jobs.clear();
                    }

                    Thread.sleep(500);
                }
            } catch (URISyntaxException | IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Thread GitlabPipelineStatusUpdateRunner(JPanel root) {
        return new Thread(() -> {
            while (true) {
                try {
                    var first = pipelines.stream().findFirst();
                    if (first.isPresent()) {
                        List<PipelineJob> jobsFromPipeline = gitlabProjectConnection.getJobsFromPipeline(first.get());
                        if (jobsFromPipeline == null) continue;
                        root.removeAll();
                        jobs.clear();
                        jobsFromPipeline.forEach(pipelineJob -> {
                            var jLabel = new JLabel();
                            root.add(jLabel);
                            root.add(Box.createHorizontalStrut(5));
                            updateLabel(jLabel, pipelineJob);
                            jobs.put(pipelineJob.id(), new JobPanelDto(jLabel, pipelineJob, first.get().project_id()));
                        });
                    } else {
                        root.removeAll();
                        root.add(new JLabel("config error"));
                    }

                    Thread.sleep(500);
                } catch (URISyntaxException | IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public Thread UpdateJobStates() {
        return new Thread(() -> {
            while (true) {
                try {
                    List<JobPanelDto> values = new ArrayList<>(jobs.values());
                    values.forEach(pipelineJob -> {
                        try {
                            PipelineJob job = gitlabProjectConnection.getJob(pipelineJob.projectId, pipelineJob.pipelineJob.id());
                            if (job == null) return;
                            updateLabel(pipelineJob.label, job);
                        } catch (URISyntaxException | IOException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
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
        label.revalidate();
        label.repaint();
    }

    private record JobPanelDto(JLabel label, PipelineJob pipelineJob, long projectId) {
    }
}
