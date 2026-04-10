# IPS to PlantUML - IntelliJ Plugin

An IntelliJ IDEA plugin that generates [PlantUML](https://plantuml.com/) class diagrams from [Faktor-IPS](https://github.com/faktorips) model files, directly inside your IDE.

## Supported IPS File Types

| Extension                  | Description              |
|----------------------------|--------------------------|
| `.ipspolicycmpttype`       | Policy component types   |
| `.ipsproductcmpttype`      | Product component types  |
| `.ipsenumtype`             | Enum types               |
| `.ipsenumcontent`          | Enum content (values)    |
| `.ipstablestructure`       | Table structures         |

## Installation

1. Build the plugin: `./gradlew buildPlugin`
2. The plugin zip will be at `build/distributions/ips2plant-intellij-plugin-<version>.zip`
3. In IntelliJ: **Settings > Plugins > gear icon > Install Plugin from Disk...** and select the zip

### Prerequisites

* IntelliJ IDEA 2024.1 or later
* Java 21+

## Usage

There are two ways to generate a diagram:

### Right-Click Action

1. In the **Project** view, right-click on one or more directories containing IPS model files
2. Select **Generate PlantUML Diagram**
3. The generated `.puml` opens in the editor

The right-click action uses the diagram options configured in the tool window (see below).

### Tool Window

1. Open the **IPS to PlantUML** tool window (right panel)
2. The plugin auto-detects `.ipsproject` files in your project and shows the model directories as a checkbox tree
3. Select the model directories you want to include
4. Click **Generate Model UML** — if no directories are selected, all detected directories are used (configurable in Settings)
5. The generated `.puml` opens in the editor
6. Configure the diagram options as needed. The diagram regenerates automatically when options are changed
7. **Generate Model UML** always clears any active search and generates from model directories

### Resolve Dependencies

The plugin can include IPS model files from Maven dependency JARs in the diagram. This is useful when your project depends on shared base models (e.g. `de.faktorzehn` artifacts) and you want to see the full class hierarchy.

1. Select one or more model directories in the tree (or leave all unchecked to use all detected directories)
2. Click **Resolve Dependencies**
3. The plugin runs `mvn dependency:build-classpath` on the corresponding `pom.xml`, scans the resolved JARs for IPS model files (`model/**/*.ips*`), and extracts them to a temporary directory
4. The extracted dependency models appear under a **dependencies** node in the tree
5. Check the dependency models you want to include and click **Generate Model UML**

Only external `de.faktorzehn` group dependencies are resolved — JARs belonging to modules within the current project are automatically excluded. The plugin locates Maven via `MAVEN_HOME`, `M2_HOME`, `PATH`, or common installation directories.

### Search

The search panel lets you find specific IPS classes by name and generate a diagram for just those classes.

1. Enter a class name pattern in the **Search** field (supports `*` wildcard, e.g. `*Contract*`, `Policy*`, `*Type`; and regex, e.g. `Contract|Policy`, `Policy.*Type`)
2. Press **Enter** or click **Search**
3. Matching classes appear as a checkbox list — all are selected by default
4. A PlantUML diagram is generated automatically for the checked results
5. Uncheck classes to exclude them and the diagram regenerates accordingly
6. Use **Select All / Deselect All** to toggle all results at once (appears when 3+ results)
7. Clear the search field to dismiss the search results and return to directory-based generation

The search searches all selected model directories, including selected dependencies. If no directories are selected, all model directories (local and resolved dependencies) are searched. The search is case-insensitive and matches against the simple class name (not the fully qualified name). The pattern supports `*` as a wildcard (expanded to `.*`) and full Java regex syntax — both can be combined freely.

**Add Supertypes**: Transitively adds all parent classes (supertypes, their supertypes, etc.) of the found classes to the diagram. The added classes are not part of the search results and have no checkboxes — they only appear in the generated PlantUML.

**Add Referencing Classes**: Adds all classes that directly reference any of the found classes through associations (composition, association, aggregation). Like supertypes, these are only included in the diagram, not in the search result list.

### Diagram Options

| Option                  | Default | Description                                                                     |
|-------------------------|---------|---------------------------------------------------------------------------------|
| Policy Components       | ✓       | Includes policy component types in the diagram                                  |
| Product Components      |         | Includes product component types in the diagram                                 |
| Table Structures        |         | Includes table structures in the diagram                                        |
| Table Usage             |         | Shows table usage by product component types                                    |
| Enum Types              |         | Includes enum types in the diagram                                              |
| Enum Content            |         | Shows content values of extensible enum types                                   |
| Packages                |         | Groups classes into their packages                                              |
| Target Roles            |         | Shows the `targetRolePlural` on composition arrows                              |
| External Supertypes     |         | Adds inheritance of supertypes not in the selected packages                     |
| External Associations   |         | Adds associations to classes not in the selected packages                       |
| Maven Modules           |         | Shows in which Maven module each class is defined                               |
| Enum Associations       |         | Shows enum associations (including external enums)                              |
| Descriptions            |         | Shows IPS description texts as PlantUML notes (language set in Settings)        |
| Package Filter          |         | Limits the diagram to a specific package and its associations                   |
| Connector Length        | 2       | Length of association connectors                                                |

**Select All** enables all option checkboxes at once (by default, Descriptions is excluded from Select All — see Settings). **Select None** turns them off. **Reset to Default** reverts the options to their initial state (Policy Components on, all others off, package filter cleared, connector length 2). **Reset All** unchecks all model directories, clears the search, resets all options to default, and clears the diagram.

### Settings

Open **Settings > Tools > IPS to PlantUML** to configure plugin behaviour.

| Setting | Default | Description |
|---------|---------|-------------|
| Locale  | `de`    | Language used for description texts and enum content labels |
| Model generation falls back on all directories when none selected | ✓ | When no directories are checked, **Generate Model UML** uses all detected directories |
| Model generation resets the options to default (showing only Policy Components) | | When **Generate Model UML** is clicked, all options are reset first |
| Search falls back on all directories when none selected | ✓ | When no directories are checked, the search runs across all directories |
| Search selects all class types found | ✓ | After a successful search, **Policy Components** / **Product Components** / etc. are automatically enabled for every class type present in the results |
| Search resets all options (other than class types found, when selected) | | When a search completes, all options (except those auto-enabled by the previous setting) are reset first |
| Selecting / deselecting model directories retriggers model generation / search | | Checking or unchecking a directory in the tree immediately regenerates the current diagram or search |
| Options Select All ignores Descriptions | ✓ | Clicking **Select All** does not enable the **Descriptions** option; deselecting still clears it |

## Attribute Types

Attribute visibility markers represent Faktor-IPS attribute types:

| Marker | Attribute Type |
|--------|----------------|
| `+`    | changeable     |
| `~`    | derived        |
| `#`    | computed       |
| `-`    | constant       |

As shown in generated plantUml:

![Attribute type legend](../docu/attr_type_legend.png)

## Tips

* If you have the [PlantUML Integration](https://plugins.jetbrains.com/plugin/7017-plantuml-integration) plugin installed, the generated diagram will render automatically when opened.
* You can select multiple directories at once, e.g. a base model and a product-specific model, to get a combined diagram.
* To include descriptions in the diagram, check **Descriptions** in the options and set the desired language in **Settings > Tools > IPS to PlantUML**.

## Building from Source

```bash
cd intellij-plugin
./gradlew buildPlugin
```
