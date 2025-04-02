package de.dontknow.gitlabpipelines.config;

import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import de.dontknow.gitlabpipelines.gitlab.GitlabProjectConnection;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Supports creating and managing a {@link JPanel} for the Settings Dialog.
 */
public class AppSettingComponent {

    private final JPanel rootPanel;
    private final JBTextField gitlabUrlText = new JBTextField();
    private final JBPasswordField gitlabPrivateTokenText = new JBPasswordField();
    private final JBLabel connectionStatusLabel = new JBLabel();

    public AppSettingComponent() {
        rootPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Gitlab url:"), gitlabUrlText, 1, false)
                .addLabeledComponent(new JBLabel("Gitlab private-token:"), gitlabPrivateTokenText, 1, false)
                .addLabeledComponent(new JBLabel("Connection status:"), connectionStatusLabel, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        var gitlabProjectConnection = new GitlabProjectConnection();
        updateStatusSymbol(gitlabProjectConnection);
        gitlabUrlText.getDocument().addDocumentListener(addListener(() -> updateStatusSymbol(gitlabProjectConnection)));
        gitlabPrivateTokenText.getDocument().addDocumentListener(addListener(() -> updateStatusSymbol(gitlabProjectConnection)));
    }

    private void updateStatusSymbol(GitlabProjectConnection gitlabProjectConnection) {
        connectionStatusLabel.setIcon(IconLoader.getIcon("/Icons/status-running.svg", getClass()));
        new Thread(() -> {
            if (gitlabProjectConnection.isValid(gitlabUrlText.getText(), new String(gitlabPrivateTokenText.getPassword()))) {
                connectionStatusLabel.setIcon(IconLoader.getIcon("/Icons/status_success_solid.svg", getClass()));
            } else {
                connectionStatusLabel.setIcon(IconLoader.getIcon("/Icons/status-failed.svg", getClass()));
            }
        }).start();
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
        return new String(gitlabPrivateTokenText.getPassword());
    }

    public void setGitlabPrivateToken(String newToken) {
        gitlabPrivateTokenText.setText(newToken);
    }


    private static @NotNull DocumentListener addListener(Runnable onChange) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onChange.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onChange.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        };
    }
}
