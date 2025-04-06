package de.dontknow.gitlabpipelines.gitlab.dto;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public enum JobStatus {
    waiting_for_resource("/Icons/status-waiting.svg"),
    success("/Icons/status_success_solid.svg"),
    pending("/Icons/status-paused.svg"),
    failed("/Icons/status-failed.svg"),
    running("/Icons/status-running.svg"),
    created("/Icons/status-waiting.svg"),
    manual("/Icons/status_manual.svg");

    private String iconPath;

    JobStatus(String iconPath) {
        this.iconPath = iconPath;
    }

    public String getIconPath() {
        return iconPath.replaceFirst("/", "");
    }

    public Icon getIcon() {
        return IconLoader.getIcon(iconPath, getClass());
    }
}
