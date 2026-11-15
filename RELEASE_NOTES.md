# Release notes

Baseline is version 0.4.4; each entry describes only what the version adds.

## 0.5.0 (unreleased)

### IntelliJ plugin

- **Tree tab**: draws everything reachable from a root policy component type (descendants), everything that reaches it
  (ancestors), or both. Options: depth (association hops), include abstract classes, merge supertypes. The root class is
  drawn with a bold red border. The root name is case-insensitive.
- **Classpath restriction**: the tree walk only sees the model directories on the Maven classpath of the root's module.
- **Merge supertypes**: inherited attributes are shown inside each class below a dotted line, and inherited associations
  are drawn from the class itself with a grey arrow. Supertypes appear only if another class points at them. Arrows to
  an abstract class are drawn to its concrete subtypes.

### Diagram (XSL, shared with the Quarkus CLI)

- Inherited attributes (`inherited="true"` in the collection) are rendered below a `..` separator, inherited
  associations get a grey line.
- New stylesheet parameter `highlightClass` (fully qualified class name) draws that class with a bold red border.
  It is not yet available as a CLI flag.
- **Package filter exclusions**: `!prefix` excludes classes, `*` includes everything (implicit if the filter has only
  exclusions). The longest matching prefix decides, on a tie the inclusion wins, e.g. `coverage,!coverage.Foo` or
  `*,!coverage,coverage.Foo`. Applies to the CLI (`-pf`) and the plugin's Package Filter field.
- Empty entries in the package filter (e.g. a trailing comma in `policy,`) are now ignored; before, they matched every class.

### Quarkus CLI

- `-pf` / `--package-filter` supports the new `!` and `*` entries (use single quotes in the shell).
  The Tree features are not yet available in the CLI.
