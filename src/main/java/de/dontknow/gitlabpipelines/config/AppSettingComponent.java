package de.dontknow.gitlabpipelines.config;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Supports creating and managing a {@link JPanel} for the Settings Dialog.
 */
public class AppSettingComponent {

    private final JPanel rootPanel;
    private final JBTextField gitlabUrlText = new JBTextField();
    private final JBPasswordField gitlabPrivateTokenText = new JBPasswordField();

    public AppSettingComponent() {
        rootPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Gitlab url:"), gitlabUrlText, 1, false)
                .addLabeledComponent(new JBLabel("Gitlab private-token:"), gitlabPrivateTokenText, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    public JPanel getPanel() {
        return rootPanel;
    }

    public JComponent getPreferredFocusedComponent() {
        return gitlabUrlText;
    }

    @NotNull
    public String getGitlabUrl() {
        return gitlabUrlText.getText();
    }

    public void setGitlabUrl(@NotNull String gitlabUrl) {
        gitlabUrlText.setText(gitlabUrl);
    }

    public String getGitlabPrivateToken() {
        return gitlabPrivateTokenText.getText();
    }

    public void setGitlabPrivateToken(String newToken) {
        gitlabPrivateTokenText.setText(newToken);
    }

}
