package com.github.pbroman.ips2plant.ui.manager;

/**
 * Tracks whether the tool window is in model-generation mode, search mode or tree mode.
 * The three states are mutually exclusive.
 *
 * <ul>
 *   <li>{@link #activateModelGeneration()} — called when "Generate Model UML" is clicked</li>
 *   <li>{@link #activateSearch()} — called when a search is triggered</li>
 *   <li>{@link #activateTree()} — called when a tree lookup is triggered</li>
 *   <li>{@link #deactivateSearch()} — called when the search field is cleared</li>
 *   <li>{@link #reset()} — called by "Reset All"</li>
 * </ul>
 *
 * Options checkboxes should regenerate when {@link #shouldRegenerate()} is true.
 * The "Add Supertypes" / "Add Referencing Classes" checkboxes should only regenerate
 * when {@link #isSearchActive()} is true.
 */
public class GenerationStateManager {

    private boolean modelGenerationActive;
    private boolean searchActive;
    private boolean treeActive;

    /** Activate model-generation mode; clears search and tree mode. */
    public void activateModelGeneration() {
        modelGenerationActive = true;
        searchActive = false;
        treeActive = false;
    }

    /** Activate search mode; clears model-generation and tree mode. */
    public void activateSearch() {
        searchActive = true;
        modelGenerationActive = false;
        treeActive = false;
    }

    /** Activate tree mode; clears model-generation and search mode. */
    public void activateTree() {
        treeActive = true;
        searchActive = false;
        modelGenerationActive = false;
    }

    /** Deactivate all modes (used when search field is cleared). */
    public void deactivateSearch() {
        reset();
    }

    /** Deactivate all modes (used by Reset All). */
    public void reset() {
        modelGenerationActive = false;
        searchActive = false;
        treeActive = false;
    }

    public boolean isModelGenerationActive() {
        return modelGenerationActive;
    }

    public boolean isSearchActive() {
        return searchActive;
    }

    public boolean isTreeActive() {
        return treeActive;
    }

    /** Returns true when a scheduled regeneration should proceed. */
    public boolean shouldRegenerate() {
        return modelGenerationActive || searchActive || treeActive;
    }
}
