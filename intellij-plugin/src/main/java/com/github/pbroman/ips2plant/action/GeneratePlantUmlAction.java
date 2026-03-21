package com.github.pbroman.ips2plant.action;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import com.github.pbroman.ips2plant.core.Ips2PlantGenerator;
import com.github.pbroman.ips2plant.core.Ips2PlantOptions;
import com.github.pbroman.ips2plant.ui.Ips2PlantToolWindowFactory;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GeneratePlantUmlAction extends AnAction {

    private static final Map<Project, LightVirtualFile> pumlFiles = new ConcurrentHashMap<>();

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (project == null) return;

        var selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        if (selectedFiles == null || selectedFiles.length == 0) return;

        List<Path> dirs = collectDirectoryPaths(selectedFiles);
        if (dirs.isEmpty()) return;

        var options = Ips2PlantToolWindowFactory.getCurrentOptions(project);

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating PlantUML from IPS Models", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Collecting IPS files...");
                var generator = new Ips2PlantGenerator();
                var pumlContent = generator.generate(dirs, options);

                ApplicationManager.getApplication().invokeLater(() ->
                        openInEditor(project, pumlContent));
            }
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        var project = e.getProject();
        var files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        boolean hasDirectory = project != null && files != null
                && Arrays.stream(files).anyMatch(VirtualFile::isDirectory);
        e.getPresentation().setEnabled(hasDirectory);
        e.getPresentation().setVisible(project != null);
    }

    private List<Path> collectDirectoryPaths(VirtualFile[] files) {
        return Arrays.stream(files)
                .filter(VirtualFile::isDirectory)
                .map(vf -> Path.of(vf.getPath()))
                .toList();
    }

    public static void openInEditor(Project project, String pumlContent) {
        var existingFile = pumlFiles.get(project);
        if (existingFile != null) {
            // Reuse existing tab: update the document content
            var document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(existingFile);
            if (document != null) {
                WriteCommandAction.runWriteCommandAction(project, () ->
                        document.setText(pumlContent));
                // Ensure the tab is focused
                FileEditorManager.getInstance(project).openFile(existingFile, true);
                return;
            }
        }
        // Create new file and tab
        var virtualFile = new LightVirtualFile("ips-model.puml", pumlContent);
        pumlFiles.put(project, virtualFile);
        FileEditorManager.getInstance(project).openFile(virtualFile, true);
    }
}