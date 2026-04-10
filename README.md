# IPS to PlantUML

Generates [PlantUML](https://plantuml.com/) class diagrams from [Faktor-IPS](https://github.com/faktorips) model files.

Three implementations are available:

| Implementation | Status | Description |
|---|---|---|
| **IntelliJ Plugin** (`intellij-plugin/`) | Active | IDE tool window + right-click action |
| **Quarkus CLI** (`quarkus/`) | Active | Java CLI, no extra tooling required |
| **Bash script** (`ips2plant.sh`) | Legacy | Requires `xsltproc` (Linux/WSL/Docker) |

---

## IntelliJ Plugin

See [`intellij-plugin/README.md`](intellij-plugin/README.md) for full documentation.

---

## Quarkus CLI

### Build & Run

```bash
./build-quarkus.sh          # builds quarkus/target/ips2plant-cli-runner.jar
./run-quarkus.sh [flags]    # runs the built jar
```

### Usage

```
./run-quarkus.sh -p <model-dir> [options] -o output.puml
```

For multiple model directories repeat `-p`:

```bash
./run-quarkus.sh -p path/to/im/domain/model -p path/to/ipm/business/model -o output.puml
```

### Options

| Flag | Long form | Description |
|------|-----------|-------------|
| `-p` | `--paths` | Path(s) to model directories (repeat for multiple) |
| `-o` | `--output` | Output file path (`.puml`) |
| `-w` | `--workdir` | Working directory for intermediate `collection.xml` |
| `-k` | `--packages` | Group classes into their packages |
| `-r` | `--print-target-role` | Print `targetRolePlural` on composition arrows |
| `-s` | `--add-super-type` | Add supertypes not present in the scanned models |
| `-a` | `--add-associations` | Add associations to classes not in the scanned models |
| `-pr` | `--show-products` | Show product component types |
| `-np` | `--no-policies` | Hide policy component types |
| `-t` | `--show-tables` | Show table structures |
| `-tu` | `--show-table-usage` | Show table usage by product component types |
| `-et` | `--show-enum-types` | Show enum types |
| `-ec` | `--show-enum-content` | Show enum content (values of extensible enum types) |
| `-ea` | `--show-enum-assoc` | Show enum associations (including external enums) |
| `-m` | `--maven` | Show Maven module for each class |
| `-d` | `--descriptions` | Show description texts as PlantUML notes |
| `-c` | `--locale` | Language for descriptions and enum content (default: `de`) |
| `-pf` | `--package-filter` | Limit diagram to a specific package and its associations |
| `-l` | `--connector-length` | Length of association connectors (default: 2) |

### Example

```bash
./run-quarkus.sh \
  -p path/to/im/domain/model \
  -p path/to/ipm/business/model \
  -k -r -s -pr -t -tu -et \
  -o output/diagram.puml
```

---

## Bash / Linux Version (Legacy)

> **Not actively developed.** Requires `xsltproc` on Linux/WSL, or Docker.

### Prerequisites

* Linux, macOS, or WSL with `xsltproc` installed, **or** Docker

### Using with Docker

```bash
docker build . -t alpine-ips2plant
# Windows:
ips2plantDocker.bat <path-to-repos> [options]
# Prefix paths with /repos/ inside the container:
ips2plantDocker.bat C:\Users\me\repos -p "/repos/im/domain/model"
```

### Usage

```
./ips2plant.sh --help
```

---

## Attribute Types

Attribute visibility markers represent Faktor-IPS attribute types:

| Marker | Attribute Type |
|--------|----------------|
| `+`    | changeable     |
| `~`    | derived        |
| `#`    | computed       |
| `-`    | constant       |

![Attribute Type Legend](docu/attr_type_legend.png)
