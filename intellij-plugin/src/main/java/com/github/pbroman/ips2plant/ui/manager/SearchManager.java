package com.github.pbroman.ips2plant.ui.manager;

import com.github.pbroman.ips2plant.core.IpsClassSearcher;
import com.github.pbroman.ips2plant.settings.Ips2PlantSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CheckBoxList;
import org.jetbrains.annotations.NotNull;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.CardLayout;
import java.awt.Color;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SearchManager {

    private static final Logger LOG = Logger.getInstance(SearchManager.class);
    private static final String TASK_TITLE_SEARCH = "Searching IPS Classes";
    private static final String LABEL_SELECT_ALL_SEARCH = "Select All Results";
    private static final String LABEL_DESELECT_ALL_SEARCH = "Deselect All Results";
    private static final int SELECT_ALL_SEARCH_THRESHOLD = 3;
    private static final String CARD_RESULTS = "results";
    private static final String CARD_NO_RESULTS = "noResults";

    private final Project project;
    private final ModelDirTreeManager treeManager;
    private final GenerationStateManager generationState;

    // UI components owned by this manager, exposed for panel layout
    public final JCheckBox addSupertypesCheck = new JCheckBox("Add Supertypes");
    public final JCheckBox addReferencingCheck = new JCheckBox("Add Referencing Classes");
    private final CheckBoxList<String> searchResultsList = new CheckBoxList<>();
    private final JLabel noResultsLabel = new JLabel("No IPS classes found");
    private final CardLayout searchResultsCardLayout = new CardLayout();
    private final JPanel searchResultsCardPanel = new JPanel(searchResultsCardLayout);
    private final JButton selectAllSearchButton = new JButton(LABEL_SELECT_ALL_SEARCH);

    // State
    private final Map<String, File> searchResults = new LinkedHashMap<>();
    private boolean allSearchResultsSelected = true;

    // Callbacks
    private Runnable onResultsUpdated;
    private Consumer<Map<String, File>> onOptionsAutoEnabled;
    private Runnable onOptionsReset;

    public SearchManager(Project project, ModelDirTreeManager treeManager, GenerationStateManager generationState) {
        this.project = project;
        this.treeManager = treeManager;
        this.generationState = generationState;

        addSupertypesCheck.setToolTipText("Transitively add all supertypes (parents, grandparents, etc.) of found classes");
        addReferencingCheck.setToolTipText("Add all classes that reference found classes through associations");

        selectAllSearchButton.setToolTipText("Select or deselect all found classes");
        selectAllSearchButton.setVisible(false);
        selectAllSearchButton.addActionListener(e -> {
            allSearchResultsSelected = !allSearchResultsSelected;
            searchResultsList.clear();
            for (var className : searchResults.keySet()) {
                searchResultsList.addItem(className, className, allSearchResultsSelected);
            }
            selectAllSearchButton.setText(allSearchResultsSelected ? LABEL_DESELECT_ALL_SEARCH : LABEL_SELECT_ALL_SEARCH);
            if (allSearchResultsSelected && onResultsUpdated != null) {
                onResultsUpdated.run();
            }
        });

        searchResultsList.setToolTipText("Check/uncheck classes to include in the generated PlantUML diagram");
        searchResultsList.setCheckBoxListListener((index, value) -> {
            if (onResultsUpdated != null) onResultsUpdated.run();
        });

        var searchResultsScroll = new JScrollPane(searchResultsList);
        searchResultsScroll.setBorder(null);
        noResultsLabel.setForeground(new Color(200, 0, 0));
        noResultsLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
        searchResultsCardPanel.add(searchResultsScroll, CARD_RESULTS);
        searchResultsCardPanel.add(noResultsLabel, CARD_NO_RESULTS);
        searchResultsCardLayout.show(searchResultsCardPanel, CARD_RESULTS);
    }

    public void setOnResultsUpdated(Runnable callback) {
        this.onResultsUpdated = callback;
    }

    public void setOnOptionsAutoEnabled(Consumer<Map<String, File>> callback) {
        this.onOptionsAutoEnabled = callback;
    }

    public void setOnOptionsReset(Runnable callback) {
        this.onOptionsReset = callback;
    }

    public JPanel getResultsCardPanel() {
        return searchResultsCardPanel;
    }

    public JButton getSelectAllButton() {
        return selectAllSearchButton;
    }

    public Map<String, File> getSearchResults() {
        return searchResults;
    }

    public Map<String, File> getSelectedSearchResults() {
        var selected = new LinkedHashMap<String, File>();
        int count = searchResultsList.getModel().getSize();
        for (int i = 0; i < count; i++) {
            var className = searchResultsList.getItemAt(i);
            if (className != null && searchResultsList.isItemSelected(className)) {
                var file = searchResults.get(className);
                if (file != null) {
                    selected.put(className, file);
                }
            }
        }
        return selected;
    }

    public boolean isAddSupertypes() {
        return addSupertypesCheck.isSelected();
    }

    public boolean isAddReferencing() {
        return addReferencingCheck.isSelected();
    }

    public void runSearch(String pattern) {
        generationState.activateSearch();
        if (pattern.isEmpty()) return;
        LOG.info("runSearch: pattern='" + pattern + "'");

        var selectedLocal = treeManager.getSelectedLocalDirs();
        var selectedDeps = treeManager.getSelectedDependencyDirs();
        boolean noneSelected = selectedLocal.isEmpty() && selectedDeps.isEmpty();
        var searchDirs = new ArrayList<Path>();
        if (noneSelected && Ips2PlantSettings.getInstance().searchFallbackToAll) {
            searchDirs.addAll(treeManager.getAllLocalDirs());
            searchDirs.addAll(treeManager.getAllDependencyDirs());
        } else {
            searchDirs.addAll(selectedLocal);
            searchDirs.addAll(selectedDeps);
        }

        LOG.info("runSearch: searching in " + searchDirs.size() + " directories");
        var dirsToSearch = List.copyOf(searchDirs);
        ProgressManager.getInstance().run(new Task.Backgroundable(project, TASK_TITLE_SEARCH, false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Searching for IPS classes...");
                var searcher = new IpsClassSearcher();
                var results = searcher.search(pattern, dirsToSearch);
                LOG.info("runSearch: found " + results.size() + " matching classes");
                ApplicationManager.getApplication().invokeLater(() -> updateSearchResults(results));
            }
        });
    }

    public void clearSearchResults() {
        searchResults.clear();
        searchResultsList.clear();
        selectAllSearchButton.setVisible(false);
        searchResultsCardLayout.show(searchResultsCardPanel, CARD_RESULTS);
    }

    public void resetSearchOptions() {
        addSupertypesCheck.setSelected(false);
        addReferencingCheck.setSelected(false);
    }

    private void updateSearchResults(Map<String, File> results) {
        searchResults.clear();
        searchResults.putAll(results);
        searchResultsList.clear();
        if (results.isEmpty()) {
            LOG.info("Search returned no results");
            selectAllSearchButton.setVisible(false);
            searchResultsCardLayout.show(searchResultsCardPanel, CARD_NO_RESULTS);
        } else {
            LOG.info("Search returned " + results.size() + " results");
            for (var className : results.keySet()) {
                searchResultsList.addItem(className, className, true);
            }
            allSearchResultsSelected = true;
            selectAllSearchButton.setText(LABEL_DESELECT_ALL_SEARCH);
            selectAllSearchButton.setVisible(results.size() >= SELECT_ALL_SEARCH_THRESHOLD);
            var settings = Ips2PlantSettings.getInstance();
            if (settings.searchResetsOptions) {
                if (onOptionsReset != null) onOptionsReset.run();
                resetSearchOptions();
            }
            if (settings.searchSelectsAllClassTypes && onOptionsAutoEnabled != null) {
                onOptionsAutoEnabled.accept(results);
            }
            searchResultsCardLayout.show(searchResultsCardPanel, CARD_RESULTS);
            if (onResultsUpdated != null) onResultsUpdated.run();
        }
    }
}
