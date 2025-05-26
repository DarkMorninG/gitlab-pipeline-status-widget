package de.dontknow.gitlabpipelines.gitlab;

import com.intellij.ide.DataManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.IconUtil;
import de.dontknow.gitlabpipelines.gitlab.dto.GitlabStatus;
import de.dontknow.gitlabpipelines.gitlab.dto.PipelineDto;
import de.dontknow.gitlabpipelines.gitlab.dto.PipelineJob;
import de.dontknow.gitlabpipelines.gitlab.dto.ProjectDto;
import de.dontknow.gitlabpipelines.widget.JobDisplayAction;
import de.dontknow.gitlabpipelines.widget.JobGroupDropDown;
import git4idea.GitReference;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import git4idea.repo.GitRepositoryManager;
import reactor.core.publisher.Flux;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class PipelineStatusDisplay {

    private final HashMap<String, StagePanelDto> displayedStages = new HashMap<>();
    private final HashMap<Integer, JobPanelActionDto> displayedActionJobs = new HashMap<>();
    private final HashSet<PipelineDto> pipelines = new HashSet<>();
    private ProjectDto projectDto;
    private GitRepository currentGitRepository;
    private boolean pipelineCompleted;

    private boolean isRunning = true;
    private final GitlabProjectConnection gitlabProjectConnection;

    public PipelineStatusDisplay(GitlabProjectConnection gitlabProjectConnection) {
        this.gitlabProjectConnection = gitlabProjectConnection;
    }

    public void startWatcher(Project project, JPanel rootPanel) {
        var connection = project.getMessageBus().connect();
        AtomicReference<String> lastBranch = new AtomicReference<>("");
        connection.subscribe(GitRepository.GIT_REPO_CHANGE, (GitRepositoryChangeListener) repository -> {
            if (repository.getCurrentBranch() != null && !repository.getCurrentBranch().getName().equals(lastBranch.get())) {
                displayLoading(rootPanel);
                lastBranch.set(repository.getCurrentBranch().getName());
            }
        });


        Flux.interval(Duration.of(1, ChronoUnit.SECONDS))
                .handle((aLong, synchronousSink) -> {
                    if (projectDto == null && currentGitRepository == null) {
                        updateProjectDtoAndGitRepo(project);
                    } else {
                        if (!pipelineCompleted) {
                            updateJobStates();
                        }
                        updateActivePipeline(rootPanel);
                    }
                }).takeUntil(o -> !isRunning)
                .subscribe();
    }

    public void dispose() {
        isRunning = false;
    }

    private void updateProjectDtoAndGitRepo(Project project) {
        var gitRepo = getGitRepo(project);
        if (gitRepo.isPresent()) {
            var gitRepository = gitRepo.get();
            try {
                projectDto = gitlabProjectConnection.getProject(gitRepository);
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
            currentGitRepository = gitRepository;
        }
    }


    private void updateActivePipeline(JPanel root) {
        try {
            if (gitlabProjectConnection.isValid()) {
                PipelineDto master = gitlabProjectConnection.getLatestPipeline(projectDto, Optional.of(currentGitRepository).map(GitRepository::getCurrentBranch).map(GitReference::getName).orElse("master"));
                if (master != null) {
                    pipelineCompleted = master.status() == GitlabStatus.failed || master.status() == GitlabStatus.success;
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

    private void updatePipelineStatus(JPanel root, PipelineDto pipelineDto) {
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
            updateJobStates();

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
        var updatedPipelineJob = new HashMap<Integer, PipelineJob>();

        displayLabelJobs.parallelStream().forEach(stagePanelDto -> {
            var stageStatuses = stagePanelDto.stageJobs.parallelStream()
                    .map(job -> gitlabProjectConnection.getJob(job.pipeline().project_id(), job.id()))
                    .filter(Objects::nonNull)
                    .peek(pipelineJob -> updatedPipelineJob.put(pipelineJob.id(), pipelineJob))
                    .map(PipelineJob::status)
                    .toList();
            if (stageStatuses.stream().anyMatch(status -> status == GitlabStatus.failed)) {
                stagePanelDto.label.setIcon(GitlabStatus.failed.getIcon());
            } else if (stageStatuses.stream().anyMatch(status -> status == GitlabStatus.running)) {
                stagePanelDto.label.setIcon(GitlabStatus.running.getIcon());
            } else if (stageStatuses.stream().anyMatch(status -> status == GitlabStatus.pending)) {
                stagePanelDto.label.setIcon(GitlabStatus.pending.getIcon());
            } else if (stageStatuses.stream().anyMatch(status -> status == GitlabStatus.manual)) {
                stagePanelDto.label.setIcon(GitlabStatus.manual.getIcon());
            } else if (stageStatuses.stream().allMatch(status -> status == GitlabStatus.created)) {
                stagePanelDto.label.setIcon(GitlabStatus.created.getIcon());
            } else if (stageStatuses.stream().allMatch(status -> status == GitlabStatus.success)) {
                stagePanelDto.label.setIcon(GitlabStatus.success.getIcon());
            }
        });

        List<JobPanelActionDto> displayActionJobs = new ArrayList<>(displayedActionJobs.values());
        displayActionJobs.parallelStream().forEach(pipelineJob -> {
            var job = updatedPipelineJob.get(pipelineJob.pipelineJob.id());
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
