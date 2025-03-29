package de.dontknow.gitlabpipelines.config;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

/**
 * Provides controller functionality for application settings.
 */
final class AppSettingsConfigurable implements Configurable {

    private AppSettingComponent mySettingsComponent;

    // A default constructor with no arguments is required because
    // this implementation is registered as an applicationConfigurable

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "SDK: Application Settings Example";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return mySettingsComponent.getPreferredFocusedComponent();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        mySettingsComponent = new AppSettingComponent();
        return mySettingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        GitlabConnectionStorage.GitlabConfigState gitlabConfigState =
                Objects.requireNonNull(GitlabConnectionStorage.getInstance().getState());
        return !mySettingsComponent.getGitlabUrl().equals(gitlabConfigState.gitlabUrl) ||
                mySettingsComponent.getGitlabPrivateToken() != gitlabConfigState.privateToken;
    }

    @Override
    public void apply() {
        GitlabConnectionStorage.GitlabConfigState gitlabConfigState =
                Objects.requireNonNull(GitlabConnectionStorage.getInstance().getState());
        gitlabConfigState.gitlabUrl = mySettingsComponent.getGitlabUrl();
        gitlabConfigState.privateToken = mySettingsComponent.getGitlabPrivateToken();
    }

    @Override
    public void reset() {
        GitlabConnectionStorage.GitlabConfigState gitlabConfigState =
                Objects.requireNonNull(GitlabConnectionStorage.getInstance().getState());
        mySettingsComponent.setGitlabUrl(gitlabConfigState.gitlabUrl);
        mySettingsComponent.setGitlabPrivateToken(gitlabConfigState.privateToken);
    }

    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }

}
