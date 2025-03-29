package de.dontknow.gitlabpipelines.widget;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;

import javax.swing.*;
import java.awt.*;

public class ToolbarWidget extends AnAction implements CustomComponentAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        // Optional: Define an action if the widget is interactive.
    }

    @Override
    public JComponent createCustomComponent(Presentation presentation, String place) {
        // Create and return the widget component (e.g., a JPanel with a label)
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("My Widget"));
        // You can add buttons, dropdowns, or any other component as needed.
        return panel;
    }
}
