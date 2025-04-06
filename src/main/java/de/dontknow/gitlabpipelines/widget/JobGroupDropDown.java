package de.dontknow.gitlabpipelines.widget;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class JobGroupDropDown extends ActionGroup {

    private final List<JobDisplayAction> actions;
    public JobGroupDropDown(List<JobDisplayAction> actions) {
        this.actions = actions;
    }

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        return actions.toArray(new AnAction[0]);
    }
}
