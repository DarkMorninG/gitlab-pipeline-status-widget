package de.dontknow.gitlabpipelines.gitlab;

import com.intellij.ide.DataManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.IconUtil;
import de.dontknow.gitlabpipelines.gitlab.dto.JobStatus;
import de.dontknow.gitlabpipelines.gitlab.dto.PipelineDto;
import de.dontknow.gitlabpipelines.gitlab.dto.PipelineJob;
import de.dontknow.gitlabpipelines.widget.JobDisplayAction;
import de.dontknow.gitlabpipelines.widget.JobGroupDropDown;
import git4idea.GitReference;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import git4idea.repo.GitRepositoryManager;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class PipelineStatusDisplay {

    private final HashMap<String, StagePanelDto> displayedStages = new HashMap<>();
    private final HashMap<Integer, JobPanelActionDto> displayedActionJobs = new HashMap<>();
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
                updateActivePipeline(project, rootPanel);
                updateJobStates();
            }
        });
        if (!watcher.isAlive()) watcher.start();
    }


    private void updateActivePipeline(Project project, JPanel root) {
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
                        updatePipelineStatus(root, master);
                    }
                }
            } else {
                pipelines.clear();
                displayedStages.clear();
            }
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void updatePipelineStatus(JPanel root, PipelineDto pipelineDto) {
        try {
            List<PipelineJob> jobsFromPipeline = gitlabProjectConnection.getJobsFromPipeline(pipelineDto);
            if (jobsFromPipeline == null) return;
            root.removeAll();
            displayedStages.clear();
            var jobsByStage = jobsFromPipeline.stream().collect(Collectors.groupingBy(PipelineJob::stage, LinkedHashMap::new, Collectors.toList()));
            AtomicBoolean isFirst = new AtomicBoolean(false);


            jobsByStage.forEach((stage, jobs) -> {
                if (!isFirst.get()) {
                    isFirst.set(true);
                } else {
                    var icon = IconLoader.getIcon("Icons/arrow-left.svg", getClass());
                    root.add(new JLabel(IconUtil.scale(icon, root, .8f)));
                }

                var stageLabelDisplay = new JLabel();
                var jobGroupDropDown = new JobGroupDropDown(jobs.stream().map(pipelineJob -> {
                    var jobDisplayAction = new JobDisplayAction(pipelineJob);
                    displayedActionJobs.put(pipelineJob.id(), new JobPanelActionDto(jobDisplayAction, pipelineJob));
                    return jobDisplayAction;
                }).toList());
                stageLabelDisplay.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        openPopup(e, jobGroupDropDown, stage);
                    }
                });
                displayedStages.put(stage, new StagePanelDto(stageLabelDisplay, jobs));
                stageLabelDisplay.setToolTipText(stage);
                root.add(stageLabelDisplay);

            });

        } catch (URISyntaxException |
                 IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void openPopup(MouseEvent e, JobGroupDropDown jobGroupDropDown, String stage) {
        var dataContext = DataManager.getInstance().getDataContext(e.getComponent());
        var popup = JBPopupFactory.getInstance().createActionGroupPopup(stage, jobGroupDropDown, dataContext, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true);
        popup.showInBestPositionFor(dataContext);
    }


    private static void displayLoading(JPanel root) {
        root.removeAll();
        root.add(new JLabel("fetching..."));
    }

    private void updateJobStates() {
        List<StagePanelDto> displayLabelJobs = new ArrayList<>(displayedStages.values());
        displayLabelJobs.parallelStream().forEach(stagePanelDto -> {
            var stageStatuses = stagePanelDto.stageJobs.parallelStream()
                    .map(job -> gitlabProjectConnection.getJob(job.pipeline().project_id(), job.id()))
                    .filter(Objects::nonNull)
                    .map(PipelineJob::status)
                    .toList();
            if (stageStatuses.stream().anyMatch(status -> status == JobStatus.failed)) {
                stagePanelDto.label.setIcon(JobStatus.failed.getIcon());
            } else if (stageStatuses.stream().anyMatch(status -> status == JobStatus.running)) {
                stagePanelDto.label.setIcon(JobStatus.running.getIcon());
            } else if (stageStatuses.stream().anyMatch(status -> status == JobStatus.pending)) {
                stagePanelDto.label.setIcon(JobStatus.pending.getIcon());
            } else if (stageStatuses.stream().anyMatch(status -> status == JobStatus.manual)) {
                stagePanelDto.label.setIcon(JobStatus.manual.getIcon());
            } else if (stageStatuses.stream().allMatch(status -> status == JobStatus.created)) {
                stagePanelDto.label.setIcon(JobStatus.created.getIcon());
            } else if (stageStatuses.stream().allMatch(status -> status == JobStatus.success)) {
                stagePanelDto.label.setIcon(JobStatus.success.getIcon());
            }
        });

        List<JobPanelActionDto> displayActionJobs = new ArrayList<>(displayedActionJobs.values());
        displayActionJobs.forEach(pipelineJob -> {
            PipelineJob job = gitlabProjectConnection.getJob(pipelineJob.pipelineJob.pipeline().project_id(), pipelineJob.pipelineJob.id());
            if (job == null) return;
            pipelineJob.jobDisplayAction.getTemplatePresentation().setIcon(job.status().getIcon());
            pipelineJob.jobDisplayAction.getTemplatePresentation().setText(job.name());
        });
    }

    private void updateLabel(JLabel label, PipelineJob job) {
        label.setIcon(job.status().getIcon());
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


    private record StagePanelDto(JLabel label, List<PipelineJob> stageJobs) {

    }

    private record JobPanelActionDto(JobDisplayAction jobDisplayAction, PipelineJob pipelineJob) {

    }
}
