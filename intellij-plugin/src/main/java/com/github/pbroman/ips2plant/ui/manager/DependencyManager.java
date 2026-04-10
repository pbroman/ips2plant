package com.github.pbroman.ips2plant.ui.manager;

import com.github.pbroman.ips2plant.core.MavenDependencyCollector;
import com.github.pbroman.ips2plant.core.MavenDependencyCollector.DependencyModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class DependencyManager {

    private static final Logger LOG = Logger.getInstance(DependencyManager.class);
    private static final String TASK_TITLE_RESOLVE = "Resolving Maven Dependencies";

    private final Project project;
    private final ModelDirTreeManager treeManager;
    private final List<Path> dependencyTempRoots = new ArrayList<>();
    private final Map<Path, String> dependencyMavenModules = new LinkedHashMap<>();

    public DependencyManager(Project project, ModelDirTreeManager treeManager) {
        this.project = project;
        this.treeManager = treeManager;
    }

    public Map<Path, String> getDependencyMavenModules() {
        return Map.copyOf(dependencyMavenModules);
    }

    public void resolveDependencies(JComponent statusTarget) {
        var basePath = project.getBasePath();
        if (basePath == null) {
            LOG.warn("resolveDependencies: project basePath is null");
            return;
        }
        var allModelDirs = treeManager.getSelectedLocalDirs();
        if (allModelDirs.isEmpty()) {
            allModelDirs = treeManager.getAllLocalDirs();
            if (!allModelDirs.isEmpty()) {
                showTemporaryStatusHint(statusTarget,
                        "Resolving dependencies is more efficient if you check model directories to resolve from", 6000);
            }
        }
        if (allModelDirs.isEmpty()) {
            LOG.warn("resolveDependencies: no model dirs available, aborting");
            return;
        }
        LOG.info("resolveDependencies: found " + allModelDirs.size() + " model dirs");

        var pomDirs = new LinkedHashSet<Path>();
        for (var modelDir : allModelDirs) {
            LOG.info("resolveDependencies: searching pom.xml for model dir: " + modelDir);
            var pomDir = findPomDir(modelDir);
            if (pomDir != null) {
                LOG.info("resolveDependencies: found pom dir: " + pomDir);
                pomDirs.add(pomDir);
            } else {
                LOG.warn("resolveDependencies: no pom.xml found for model dir: " + modelDir);
            }
        }
        if (pomDirs.isEmpty()) {
            LOG.warn("resolveDependencies: no pom directories found, aborting");
            return;
        }

        var projectArtifactIds = collectProjectArtifactIds(Path.of(basePath));
        LOG.info("resolveDependencies: project artifact IDs to exclude: " + projectArtifactIds);
        LOG.info("resolveDependencies: resolving dependencies for " + pomDirs.size() + " pom dirs");

        ProgressManager.getInstance().run(new Task.Backgroundable(project, TASK_TITLE_RESOLVE, false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Resolving Maven classpath...");
                var collector = new MavenDependencyCollector();
                var depModels = new ArrayList<DependencyModel>();
                for (var pomDir : pomDirs) {
                    int prevCount = collector.getResolvedArtifactCount();
                    try {
                        LOG.info("resolveDependencies: collecting from " + pomDir);
                        var collected = collector.collectFromDependencies(pomDir, projectArtifactIds);
                        LOG.info("resolveDependencies: collected " + collected.size() + " dependency models from " + pomDir);
                        depModels.addAll(collected);
                    } catch (IOException | InterruptedException e) {
                        LOG.error("resolveDependencies: failed to resolve dependencies from " + pomDir, e);
                    }
                    if (collector.getResolvedArtifactCount() == prevCount) {
                        LOG.info("resolveDependencies: no new artifacts from " + pomDir + ", stopping early");
                        break;
                    }
                }
                LOG.info("resolveDependencies: total dependency models: " + depModels.size());
                ApplicationManager.getApplication().invokeLater(() -> applyResolvedDependencies(depModels));
            }
        });
    }

    public void clearDependencies() {
        cleanupTempDirs();
        dependencyMavenModules.clear();
        treeManager.removeDependenciesNode();
    }

    private void applyResolvedDependencies(List<DependencyModel> depModels) {
        if (depModels.isEmpty()) return;
        cleanupTempDirs();
        dependencyMavenModules.clear();
        for (var dep : depModels) {
            dependencyTempRoots.add(dep.tempRoot());
            if (dep.mavenModule() != null) {
                dependencyMavenModules.put(dep.modelDir(), dep.mavenModule());
            }
        }
        treeManager.addDependencyNodes(depModels);
    }

    private void cleanupTempDirs() {
        for (var tempRoot : dependencyTempRoots) {
            try {
                if (tempRoot != null && tempRoot.toString().contains("ips2plant-dep-")) {
                    deleteRecursively(tempRoot);
                }
            } catch (IOException e) {
                // best effort cleanup
            }
        }
        dependencyTempRoots.clear();
    }

    private void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                for (var child : stream.toList()) {
                    deleteRecursively(child);
                }
            }
        }
        Files.deleteIfExists(path);
    }

    private HashSet<String> collectProjectArtifactIds(Path projectRoot) {
        var artifactIds = new HashSet<String>();
        var artifactPattern = Pattern.compile("<artifactId>([^<]+)</artifactId>");
        try (var stream = Files.walk(projectRoot)) {
            stream.filter(p -> p.getFileName().toString().equals("pom.xml"))
                    .filter(Files::isRegularFile)
                    .forEach(pomFile -> {
                        try {
                            var content = Files.readString(pomFile);
                            var depsIndex = content.indexOf("<dependencies");
                            var header = depsIndex >= 0 ? content.substring(0, depsIndex) : content;
                            header = header.replaceAll("(?s)<parent>.*?</parent>", "");
                            var matcher = artifactPattern.matcher(header);
                            if (matcher.find()) {
                                artifactIds.add(matcher.group(1).trim());
                            }
                        } catch (IOException e) {
                            LOG.warn("Failed to read pom.xml: " + pomFile, e);
                        }
                    });
        } catch (IOException e) {
            LOG.warn("Failed to walk project directory: " + projectRoot, e);
        }
        return artifactIds;
    }

    private Path findPomDir(Path modelDir) {
        var current = modelDir.getParent();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private void showTemporaryStatusHint(JComponent target, String message, int durationMs) {
        var balloon = JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(message, MessageType.INFO, null)
                .setFadeoutTime(durationMs)
                .createBalloon();
        balloon.show(RelativePoint.getCenterOf(target), Balloon.Position.above);
    }
}
