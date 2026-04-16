package com.github.pbroman.ips2plant.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

@State(name = "Ips2PlantSettings", storages = @Storage("ips2plant.xml"))
public class Ips2PlantSettings implements PersistentStateComponent<Ips2PlantSettings> {

    public String locale = "de";
    public boolean generateFallbackToAll = true;
    public boolean generateResetsOptions = false;
    public boolean searchFallbackToAll = true;
    public boolean searchSelectsAllClassTypes = true;
    public boolean searchResetsOptions = false;
    public boolean retriggerOnDirChange = false;
    public boolean selectAllIgnoresDescriptions = true;
    public boolean showConnectorLength = false;

    public static Ips2PlantSettings getInstance() {
        return ApplicationManager.getApplication().getService(Ips2PlantSettings.class);
    }

    @Override
    public Ips2PlantSettings getState() {
        return this;
    }

    @Override
    public void loadState(Ips2PlantSettings state) {
        this.locale = state.locale;
        this.generateFallbackToAll = state.generateFallbackToAll;
        this.generateResetsOptions = state.generateResetsOptions;
        this.searchFallbackToAll = state.searchFallbackToAll;
        this.searchSelectsAllClassTypes = state.searchSelectsAllClassTypes;
        this.searchResetsOptions = state.searchResetsOptions;
        this.retriggerOnDirChange = state.retriggerOnDirChange;
        this.selectAllIgnoresDescriptions = state.selectAllIgnoresDescriptions;
        this.showConnectorLength = state.showConnectorLength;
    }
}
