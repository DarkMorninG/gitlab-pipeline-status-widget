package de.dontknow.gitlabpipelines.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

@State(
        name = "de.dontknow.gitlab.settings",
        storages = @Storage("GitlabConnectionSetting.xml")
)
public class GitlabConnectionStorage implements PersistentStateComponent<GitlabConnectionStorage.GitlabConfigState> {

    public static class GitlabConfigState {
        @NonNls
        public String gitlabUrl = "https://Gitlab.com";
        public String privateToken = "";
    }

    private GitlabConfigState myGitlabConfigState = new GitlabConfigState();

    public static GitlabConnectionStorage getInstance() {
        return ApplicationManager.getApplication()
                .getService(GitlabConnectionStorage.class);
    }

    @Override
    public GitlabConfigState getState() {
        return myGitlabConfigState;
    }

    @Override
    public void loadState(@NotNull GitlabConnectionStorage.GitlabConfigState gitlabConfigState) {
        myGitlabConfigState = gitlabConfigState;
    }

}
