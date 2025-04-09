package de.dontknow.gitlabpipelines.widget;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBar;
import de.dontknow.gitlabpipelines.gitlab.GitlabProjectConnection;
import de.dontknow.gitlabpipelines.gitlab.PipelineStatusDisplay;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class PipelineStatusWidget implements CustomStatusBarWidget {
    private final GitlabProjectConnection gitlabProjectConnection;
    private final Project project;
    private PipelineStatusDisplay pipelineStatusDisplay;

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
        if (pipelineStatusDisplay != null) {
            pipelineStatusDisplay.dispose();
        }
    }

    @Override
    public JComponent getComponent() {
        var root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.X_AXIS));
        this.pipelineStatusDisplay = new PipelineStatusDisplay(gitlabProjectConnection);
        pipelineStatusDisplay.startWatcher(project, root);
        return root;
    }
}
