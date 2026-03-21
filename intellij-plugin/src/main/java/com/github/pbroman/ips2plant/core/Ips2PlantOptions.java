package com.github.pbroman.ips2plant.core;

import java.util.HashMap;
import java.util.Map;

public class Ips2PlantOptions {

    private boolean packages;
    private boolean printTargetRole;
    private boolean addSuperType;
    private boolean addAssociations;
    private boolean showTables;
    private boolean showTableUsage;
    private boolean showEnumTypes;
    private boolean showEnumAssociations;
    private boolean showProductComponents;
    private String packageFilter = "";
    private int connectorLength = 2;

    public Map<String, String> toXsltParams() {
        var params = new HashMap<String, String>();

        String connector = "-".repeat(connectorLength);
        String dottedConnector = ".".repeat(connectorLength);
        params.put("connector", connector);
        params.put("dottedConnector", dottedConnector);

        if (packages) params.put("packages", "true");
        if (printTargetRole) params.put("printTargetRole", "true");
        if (addSuperType) params.put("addSuperType", "true");
        if (addAssociations) params.put("addAssociations", "true");
        if (showTables) params.put("showTables", "true");
        if (showTableUsage) params.put("showTableUsage", "true");
        if (showEnumTypes) params.put("showEnumTypes", "true");
        if (showEnumAssociations) params.put("showEnumAssociations", "true");
        if (showProductComponents) params.put("showProductComponents", "true");
        if (!packageFilter.isBlank()) params.put("packageFilter", packageFilter);

        return params;
    }

    public boolean isPackages() { return packages; }
    public void setPackages(boolean packages) { this.packages = packages; }

    public boolean isPrintTargetRole() { return printTargetRole; }
    public void setPrintTargetRole(boolean printTargetRole) { this.printTargetRole = printTargetRole; }

    public boolean isAddSuperType() { return addSuperType; }
    public void setAddSuperType(boolean addSuperType) { this.addSuperType = addSuperType; }

    public boolean isAddAssociations() { return addAssociations; }
    public void setAddAssociations(boolean addAssociations) { this.addAssociations = addAssociations; }

    public boolean isShowTables() { return showTables; }
    public void setShowTables(boolean showTables) { this.showTables = showTables; }

    public boolean isShowTableUsage() { return showTableUsage; }
    public void setShowTableUsage(boolean showTableUsage) { this.showTableUsage = showTableUsage; }

    public boolean isShowEnumTypes() { return showEnumTypes; }
    public void setShowEnumTypes(boolean showEnumTypes) { this.showEnumTypes = showEnumTypes; }

    public boolean isShowEnumAssociations() { return showEnumAssociations; }
    public void setShowEnumAssociations(boolean showEnumAssociations) { this.showEnumAssociations = showEnumAssociations; }

    public boolean isShowProductComponents() { return showProductComponents; }
    public void setShowProductComponents(boolean showProductComponents) { this.showProductComponents = showProductComponents; }

    public String getPackageFilter() { return packageFilter; }
    public void setPackageFilter(String packageFilter) { this.packageFilter = packageFilter; }

    public int getConnectorLength() { return connectorLength; }
    public void setConnectorLength(int connectorLength) { this.connectorLength = connectorLength; }
}