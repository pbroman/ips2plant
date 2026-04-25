# XSL Stylesheets

The `quarkus/src/main/resources/xsl/` directory is the **source of truth** for all XSLT stylesheets used by ips2plant.

## Do not edit the plugin copy

The IntelliJ plugin also uses these files, but its copy under
`intellij-plugin/src/main/resources/xsl/` is **generated** — it is copied here
automatically by the `syncXsl` Gradle task when the plugin is built, and is not
committed to version control. Always make changes here, never there.

## Files

| File | Purpose |
|------|---------|
| `ips2plant.xsl` | Main entry point, orchestrates the other stylesheets |
| `component-types.xsl` | Policy and product component type rendering |
| `enum-types.xsl` | Enum type and enum content rendering |
| `table-structures.xsl` | Table structure rendering |
| `relationships.xsl` | Association and inheritance relationship rendering |
| `helpers.xsl` | Shared utility templates (word-wrap, formatting, etc.) |

## Development workflow

1. Edit XSL files here (in the quarkus dir)
2. Build and test via the Quarkus CLI (see `build-quarkus.sh` / `/test-xsl` skill)
3. Build the plugin — `syncXsl` copies the files automatically before packaging