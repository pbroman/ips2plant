package com.github.pbroman.ips2plant.ui;

import com.github.pbroman.ips2plant.core.Ips2PlantOptions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Ips2PlantToolWindowFactory implements ToolWindowFactory {

    private static final Map<Project, Ips2PlantToolWindowPanel> panels = new ConcurrentHashMap<>();

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        var panel = new Ips2PlantToolWindowPanel(project);
        panels.put(project, panel);

        var content = ContentFactory.getInstance().createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    public static Ips2PlantOptions getCurrentOptions(Project project) {
        var panel = panels.get(project);
        return panel != null ? panel.getOptions() : new Ips2PlantOptions();
    }
}