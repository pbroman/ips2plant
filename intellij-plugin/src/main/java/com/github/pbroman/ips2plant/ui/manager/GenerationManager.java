package com.github.pbroman.ips2plant.ui.manager;

import com.github.pbroman.ips2plant.core.Ips2PlantGenerator;
import com.github.pbroman.ips2plant.core.Ips2PlantOptions;
import com.github.pbroman.ips2plant.core.IpsClassSearcher;
import com.github.pbroman.ips2plant.core.IpsFileCollector;
import com.github.pbroman.ips2plant.core.IpsSupertypeMerger;
import com.github.pbroman.ips2plant.core.MavenModuleResolver;
import com.github.pbroman.ips2plant.settings.Ips2PlantSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.Timer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class GenerationManager {

    private static final Logger LOG = Logger.getInstance(GenerationManager.class);
    private static final int DEBOUNCE_DELAY_MS = 500;
    private static final String TASK_TITLE = "Generating PlantUML from IPS Models";
    private static final String TASK_STATUS_COLLECTING = "Collecting IPS files...";
    public static final String EMPTY_PUML = "@startuml\nnote \"Nothing to show\" as N1\n@enduml\n";

    private static final Map<Project, LightVirtualFile> pumlFiles = new ConcurrentHashMap<>();

    public static void openInEditor(Project project, String pumlContent) {
        var existingFile = pumlFiles.get(project);
        if (existingFile != null) {
            var document = ReadAction.computeBlocking(() -> FileDocumentManager.getInstance().getDocument(existingFile));
            if (document != null) {
                WriteCommandAction.runWriteCommandAction(project, () ->
                        document.setText(pumlContent));
                new OpenFileDescriptor(project, existingFile).navigate(true);
                return;
            }
        }
        var virtualFile = new LightVirtualFile("ips-model.puml", pumlContent);
        pumlFiles.put(project, virtualFile);
        new OpenFileDescriptor(project, virtualFile).navigate(true);
    }

    private final Project project;
    private final SearchManager searchManager;
    private final TreeSearchManager treeSearchManager;
    private final ModelDirTreeManager treeManager;
    private final DependencyManager depManager;
    private final GenerationStateManager generationState;
    private final Supplier<Ips2PlantOptions> optionsSupplier;
    private final Timer regenerateTimer;

    public GenerationManager(Project project, SearchManager searchManager,
                             TreeSearchManager treeSearchManager, ModelDirTreeManager treeManager, DependencyManager depManager,
                             GenerationStateManager generationState, Supplier<Ips2PlantOptions> optionsSupplier) {
        this.project = project;
        this.searchManager = searchManager;
        this.treeSearchManager = treeSearchManager;
        this.treeManager = treeManager;
        this.depManager = depManager;
        this.generationState = generationState;
        this.optionsSupplier = optionsSupplier;
        regenerateTimer = new Timer(DEBOUNCE_DELAY_MS, e -> runGeneration());
        regenerateTimer.setRepeats(false);
    }

    public void scheduleRegeneration() {
        if (generationState.shouldRegenerate()) {
            regenerateTimer.restart();
        }
    }

    public void runGeneration() {
        var selectedSearchFiles = searchManager.getSelectedSearchResults();
        var options = optionsSupplier.get();

        if (generationState.isTreeActive() && !treeSearchManager.getResults().isEmpty()) {
            var selectedTreeFiles = treeSearchManager.getSelectedResults();
            if (selectedTreeFiles.isEmpty()) {
                LOG.info("runGeneration: tree active but no results selected, clearing diagram");
                GenerationManager.openInEditor(project, EMPTY_PUML);
                return;
            }
            LOG.info("runGeneration: generating from " + selectedTreeFiles.size() + " tree results");
            options.setHighlightClass(treeSearchManager.getRoot());
            // without merging, the supertype chain is drawn as separate classes, so its files must be in the collection
            boolean merge = treeSearchManager.isMergeSupertypes();
            generateFromFiles(selectedTreeFiles, options, !merge, false, merge);
        } else if (!searchManager.getSearchResults().isEmpty()) {
            // Search is active — only generate from selected results, never fall back to directories
            if (selectedSearchFiles.isEmpty()) {
                LOG.info("runGeneration: search active but no results selected, clearing diagram");
                GenerationManager.openInEditor(project, EMPTY_PUML);
                return;
            }
            LOG.info("runGeneration: generating from " + selectedSearchFiles.size() + " search results");
            generateFromFiles(selectedSearchFiles, options,
                    searchManager.isAddSupertypes(), searchManager.isAddReferencing(), false);
        } else {
            // Generate from selected model directories
            var localDirs = treeManager.getSelectedLocalDirs();
            var depDirs = treeManager.getSelectedDependencyDirs();
            if (localDirs.isEmpty() && depDirs.isEmpty()) {
                if (!Ips2PlantSettings.getInstance().generateFallbackToAll) {
                    LOG.info("runGeneration: no directories selected and generateFallbackToAll=false, skipping");
                    return;
                }
                localDirs = treeManager.getAllLocalDirs();
                depDirs = treeManager.getAllDependencyDirs();
                if (localDirs.isEmpty() && depDirs.isEmpty()) {
                    LOG.info("runGeneration: no directories available, skipping");
                    return;
                }
            }
            var finalLocalDirs = localDirs;
            var finalDepDirs = depDirs;
            var depModules = depManager.getDependencyMavenModules();
            LOG.info("runGeneration: generating from " + localDirs.size() + " local dirs + " + depDirs.size() + " dependency dirs");
            ProgressManager.getInstance().run(new Task.Backgroundable(project, TASK_TITLE, false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setText(TASK_STATUS_COLLECTING);
                    var generator = new Ips2PlantGenerator();
                    var pumlContent = generator.generate(finalLocalDirs, finalDepDirs, options, indicator::setText, depModules);
                    ApplicationManager.getApplication().invokeLater(() ->
                            GenerationManager.openInEditor(project, pumlContent));
                }
            });
        }
    }

    private void generateFromFiles(Map<String, java.io.File> selectedFiles, Ips2PlantOptions options,
                                   boolean expandSupertypes, boolean expandReferencing, boolean mergeSupertypes) {
        var allDirs = new ArrayList<>(treeManager.getAllLocalDirs());
        allDirs.addAll(treeManager.getAllDependencyDirs());
        var depModules = depManager.getDependencyMavenModules();
        ProgressManager.getInstance().run(new Task.Backgroundable(project, TASK_TITLE, false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText(TASK_STATUS_COLLECTING);
                var ipsFiles = new LinkedHashMap<>(selectedFiles);
                if (expandSupertypes) {
                    indicator.setText("Adding supertypes...");
                    var searcher = new IpsClassSearcher();
                    ipsFiles = new LinkedHashMap<>(searcher.addSupertypes(ipsFiles, allDirs));
                }
                if (expandReferencing) {
                    indicator.setText("Adding referencing classes...");
                    var searcher = new IpsClassSearcher();
                    ipsFiles = new LinkedHashMap<>(searcher.addReferencingClasses(ipsFiles, allDirs));
                }
                var generator = new Ips2PlantGenerator();
                Map<String, String> mavenModules = Map.of();
                if (options.isShowMavenModule()) {
                    var resolver = new MavenModuleResolver();
                    for (var depEntry : depModules.entrySet()) {
                        resolver.registerModule(depEntry.getKey(), depEntry.getValue());
                    }
                    mavenModules = resolver.resolveAll(ipsFiles);
                }
                Path mergeDir = null;
                try {
                    if (mergeSupertypes) {
                        indicator.setText("Merging supertypes...");
                        mergeDir = Files.createTempDirectory("ips2plant-merge-");
                        ipsFiles = new LinkedHashMap<>(new IpsSupertypeMerger()
                                .merge(ipsFiles, new IpsFileCollector().collect(allDirs), mergeDir));
                    }
                    var pumlContent = generator.generate(ipsFiles, options, indicator::setText, mavenModules);
                    ApplicationManager.getApplication().invokeLater(() ->
                            GenerationManager.openInEditor(project, pumlContent));
                } catch (IOException e) {
                    LOG.error("Failed to merge supertypes", e);
                } finally {
                    deleteQuietly(mergeDir);
                }
            }
        });
    }

    private static void deleteQuietly(Path dir) {
        if (dir == null) return;
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException e) {
            // best effort cleanup
        }
    }
}
