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
import git4idea.GitReference;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.eclipse.jgit.lib.Repository;
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

    public PipelineStatusWidget(GitlabProjectConnection gitlabProjectConnection, @NotNull Project project) {
        this.gitlabProjectConnection = gitlabProjectConnection;
        this.project = project;
    }

    @NotNull
    @Override
    public String ID() {
        return "gitlab-status-widget";
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
            System.out.println(getGitRepo(project));
            updatePipeline().start();
            GitlabPipelineStatusUpdateRunner(root).start();
            UpdateJobStates().start();

        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return root;
    }

    private Thread updatePipeline() throws URISyntaxException, IOException, InterruptedException {
        return new Thread(() -> {
            try {
                while (true) {
                    if (gitlabProjectConnection.isValid()) {
                        var gitRepo = getGitRepo(project);
                        if (gitRepo.isEmpty()) continue;
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
                        root.removeAll();
                        root.add(new JLabel("loading..."));
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
