package de.dontknow.gitlabpipelines.widget;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import de.dontknow.gitlabpipelines.gitlab.GitlabProjectConnection;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class StatusWidgetFactory implements StatusBarWidgetFactory {

    @NotNull
    @Override
    public String getId() {
        return "gitlab-widget-status-factory";
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @NotNull
    @Override
    public String getDisplayName() {
        return "Gitlab Status Widget";
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        return true; // Set conditions when the widget should be available.
    }

    @NotNull
    @Override
    public StatusBarWidget createWidget(@NotNull Project project) {
        var gitlabProjectConnection = new GitlabProjectConnection();
        return new PipelineStatusWidget(gitlabProjectConnection, project);
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
        widget.dispose();
    }

    @Override
    public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
        return true;
    }



}
