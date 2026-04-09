package com.github.pbroman.ips2plant.ui;

/**
 * Tracks whether the tool window is in model-generation mode or search mode.
 * The two states are mutually exclusive.
 *
 * <ul>
 *   <li>{@link #activateModelGeneration()} — called when "Generate Model UML" is clicked</li>
 *   <li>{@link #activateSearch()} — called when a search is triggered</li>
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

    /** Activate model-generation mode; clears search mode. */
    public void activateModelGeneration() {
        modelGenerationActive = true;
        searchActive = false;
    }

    /** Activate search mode; clears model-generation mode. */
    public void activateSearch() {
        searchActive = true;
        modelGenerationActive = false;
    }

    /** Deactivate both modes (used when search field is cleared). */
    public void deactivateSearch() {
        searchActive = false;
        modelGenerationActive = false;
    }

    /** Deactivate both modes (used by Reset All). */
    public void reset() {
        modelGenerationActive = false;
        searchActive = false;
    }

    public boolean isModelGenerationActive() {
        return modelGenerationActive;
    }

    public boolean isSearchActive() {
        return searchActive;
    }

    /** Returns true when a scheduled regeneration should proceed. */
    public boolean shouldRegenerate() {
        return modelGenerationActive || searchActive;
    }
}
