# IPS to PlantUML - IntelliJ Plugin

An IntelliJ IDEA plugin that generates [PlantUML](https://plantuml.com/) class diagrams from [Faktor-IPS](https://github.com/faktorips) model files, directly inside your IDE.

## Supported IPS File Types

| Extension                  | Description              |
|----------------------------|--------------------------|
| `.ipspolicycmpttype`       | Policy component types   |
| `.ipsproductcmpttype`      | Product component types  |
| `.ipsenumtype`             | Enum types               |
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

1. Open the **IPS to PlantUML** tool window (bottom panel)
2. The plugin auto-detects `.ipsproject` files in your project and shows the model directories as a checkbox tree
3. Select the model directories you want to include
4. Configure the diagram options as needed
5. Click **Generate PlantUML**
6. The generated `.puml` opens in the editor
7. The diagram is regenerated when options are changed

## Diagram Options

| Option                  | Description                                                       |
|-------------------------|-------------------------------------------------------------------|
| Packages                | Groups classes into their packages                                |
| Print target role       | Shows the `targetRolePlural` on composition arrows                |
| External supertypes     | Adds inheritance for supertypes not present in the scanned models |
| External associations   | Adds associations to classes not present in the scanned models    |
| Show tables             | Includes table structures in the diagram                          |
| Show table usage        | Shows table usage by product component types                      |
| Show enum types         | Includes enum types in the diagram                                |
| Show enum associations  | Shows enum associations (including external enums)                |
| Show product components | Includes product component types in the diagram                   |
| Package filter          | Limits the diagram to a specific package and its associations     |
| Connector length        | Length of association connectors (default: 2)                     |

## Attribute Types

Attribute visibility markers represent Faktor-IPS attribute types:

| Marker | Attribute Type |
|--------|---------------|
| `+`    | changeable    |
| `~`    | derived       |
| `#`    | computed      |
| `-`    | constant      |

## Tips

* If you have the [PlantUML Integration](https://plugins.jetbrains.com/plugin/7017-plantuml-integration) plugin installed, the generated diagram will render automatically when opened.
* You can select multiple directories at once, e.g. a base model and a product-specific model, to get a combined diagram.

## Building from Source

```bash
cd intellij-plugin
./gradlew buildPlugin
```

To run a development instance of IntelliJ with the plugin loaded:

```bash
./gradlew runIde
```