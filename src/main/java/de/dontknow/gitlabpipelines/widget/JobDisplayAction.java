package de.dontknow.gitlabpipelines.widget;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import de.dontknow.gitlabpipelines.gitlab.dto.PipelineJob;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class JobDisplayAction extends AnAction {

    private final PipelineJob pipelineJob;
    public JobDisplayAction(PipelineJob pipelineJob) {
        super(pipelineJob.name(), "", pipelineJob.status().getIcon());
        this.pipelineJob = pipelineJob;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(pipelineJob.web_url()));
            } catch (IOException | URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

}
