package de.dontknow.gitlabpipelines.gitlab.dto;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public enum GitlabStatus {
    waiting_for_resource("/Icons/status-waiting.svg"),
    success("/Icons/status_success_solid.svg"),
    pending("/Icons/status-paused.svg"),
    failed("/Icons/status-failed.svg"),
    running("/Icons/status-running.svg"),
    created("/Icons/status-waiting.svg"),
    canceled("/Icons/status_canceled.svg"),
    manual("/Icons/status_manual.svg");

    private String iconPath;

    GitlabStatus(String iconPath) {
        this.iconPath = iconPath;
    }

    public String getIconPath() {
        return iconPath.replaceFirst("/", "");
    }

    public Icon getIcon() {
        return IconLoader.getIcon(iconPath, getClass());
    }
}
