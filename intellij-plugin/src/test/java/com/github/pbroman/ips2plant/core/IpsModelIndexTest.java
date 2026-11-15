package com.github.pbroman.ips2plant.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class IpsModelIndexTest {

    @TempDir
    Path tempDir;

    private Path dir(String name) throws IOException {
        return Files.createDirectories(tempDir.resolve(name));
    }

    private void write(Path dir, String fqn, String attrs, String... assocs) throws IOException {
        var file = dir.resolve(fqn.replace('.', '/') + ".ipspolicycmpttype");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "<PolicyCmptType " + attrs + ">" + String.join("", assocs) + "</PolicyCmptType>");
    }

    private static String assoc(String type, String target, String role, boolean constrain) {
        return "<Association associationType=\"" + type + "\" target=\"" + target + "\" targetRoleSingular=\"" + role
                + "\"" + (constrain ? " constrain=\"true\"" : "") + "><Label locale=\"en\" value=\"x\"/></Association>";
    }

    @Test
    void build_parsesAbstractSupertypeAndAssociations() throws IOException {
        // given
        var d = dir("m");
        write(d, "a.Base", "abstract=\"true\"");
        write(d, "a.Child", "supertype=\"a.Base\"", assoc("comp", "a.Part", "Part", false));

        // when
        var index = IpsModelIndex.build(List.of(d), List.of(d));

        // then
        assertThat(index.get("a.Base")).get().satisfies(i -> {
            assertThat(i.isAbstract()).isTrue();
            assertThat(i.supertype()).isNull();
        });
        assertThat(index.get("a.Child")).get().satisfies(i -> {
            assertThat(i.supertype()).isEqualTo("a.Base");
            assertThat(i.associations()).containsExactly(new IpsModelIndex.Assoc("comp", "a.Part", "Part", false));
        });
    }

    @Test
    void build_ignoresNonPolicyFiles() throws IOException {
        // given
        var d = dir("m");
        write(d, "a.Policy", "");
        Files.writeString(d.resolve("Prod.ipsproductcmpttype"), "<ProductCmptType2/>");

        // when
        var index = IpsModelIndex.build(List.of(d), List.of(d));

        // then
        assertThat(index.allClasses()).containsExactly("a.Policy");
    }

    @Test
    void supertypeChain_walksNearestToFarthestAcrossDirs() throws IOException {
        // given
        var local = dir("local");
        var dep = dir("dep");
        write(local, "a.Leaf", "supertype=\"a.Mid\"");
        write(dep, "a.Mid", "supertype=\"a.Top\"");
        write(dep, "a.Top", "");

        // when
        var index = IpsModelIndex.build(List.of(local, dep), List.of(local));

        // then
        assertThat(index.supertypeChain("a.Leaf")).containsExactly("a.Mid", "a.Top");
        assertThat(index.isInScope("a.Leaf")).isTrue();
        assertThat(index.isInScope("a.Mid")).isFalse();
    }

    @Test
    void supertypeChain_terminatesOnCycleAndUnknownSupertype() throws IOException {
        // given
        var d = dir("m");
        write(d, "a.X", "supertype=\"a.Y\"");
        write(d, "a.Y", "supertype=\"a.X\"");
        write(d, "a.Z", "supertype=\"ext.Unknown\"");

        // when
        var index = IpsModelIndex.build(List.of(d), List.of(d));

        // then
        assertThat(index.supertypeChain("a.X")).containsExactly("a.Y");
        assertThat(index.supertypeChain("a.Z")).containsExactly("ext.Unknown");
    }

    @Test
    void unresolvedSupertypes_findsMissingTypeBeyondDirectSupertype() throws IOException {
        // given
        var d = dir("m");
        write(d, "a.Leaf", "supertype=\"a.Mid\"");
        write(d, "a.Mid", "supertype=\"ext.Base\"");
        write(d, "a.Other", "supertype=\"ext.Base\"");
        write(d, "a.Plain", "");
        write(d, "a.X", "supertype=\"a.Y\"");
        write(d, "a.Y", "supertype=\"a.X\"");

        // when
        var index = IpsModelIndex.build(List.of(d), List.of(d));

        // then
        assertThat(index.unresolvedSupertypes(List.of("a.Leaf", "a.Other", "a.Plain", "a.X"))).containsExactly("ext.Base");
    }

    @Test
    void subtypesOf_invertsSupertypeRelation() throws IOException {
        // given
        var d = dir("m");
        write(d, "a.Base", "abstract=\"true\"");
        write(d, "a.One", "supertype=\"a.Base\"");
        write(d, "a.Two", "supertype=\"a.Base\"");

        // when
        var index = IpsModelIndex.build(List.of(d), List.of(d));

        // then
        assertThat(index.subtypesOf("a.Base")).containsExactlyInAnyOrder("a.One", "a.Two");
        assertThat(index.subtypesOf("a.One")).isEmpty();
    }

    @Test
    void effectiveAssociations_includesInheritedWithOwner() throws IOException {
        // given
        var d = dir("m");
        write(d, "a.Base", "abstract=\"true\"", assoc("comp", "a.Limit", "Limit", false));
        write(d, "a.Child", "supertype=\"a.Base\"", assoc("ass", "a.Other", "Other", false));

        // when
        var result = IpsModelIndex.build(List.of(d), List.of(d)).effectiveAssociations("a.Child");

        // then
        assertThat(result).extracting(IpsModelIndex.OwnedAssoc::owner, o -> o.assoc().target())
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("a.Child", "a.Other"),
                        org.assertj.core.groups.Tuple.tuple("a.Base", "a.Limit"));
    }

    @Test
    void effectiveAssociations_constrainedRedeclarationKeepsSubtypeTarget() throws IOException {
        // given
        var d = dir("m");
        write(d, "a.Base", "abstract=\"true\"", assoc("comp", "a.Variant", "Variant", false));
        write(d, "a.Child", "supertype=\"a.Base\"", assoc("comp", "a.SpecialVariant", "Variant", true));

        // when
        var result = IpsModelIndex.build(List.of(d), List.of(d)).effectiveAssociations("a.Child");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).owner()).isEqualTo("a.Child");
        assertThat(result.get(0).assoc().target()).isEqualTo("a.SpecialVariant");
    }

    @Test
    void resolve_matchesFqnAndUniqueSimpleNameButNotAmbiguous() throws IOException {
        // given
        var d = dir("m");
        write(d, "a.Contract", "");
        write(d, "a.Unique", "");
        write(d, "b.Contract", "");

        // when
        var index = IpsModelIndex.build(List.of(d), List.of(d));

        // then
        assertThat(index.resolve("a.Contract")).contains("a.Contract");
        assertThat(index.resolve("Unique")).contains("a.Unique");
        assertThat(index.resolve("Contract")).isEmpty();
        assertThat(index.resolve("Nope")).isEmpty();
    }

    @Test
    void resolve_isCaseInsensitiveWhenNoExactMatchExists() throws IOException {
        // given
        var d = dir("m");
        write(d, "a.LiaContract", "");
        write(d, "a.Other", "");
        write(d, "b.OTHER", "");

        // when
        var index = IpsModelIndex.build(List.of(d), List.of(d));

        // then
        assertThat(index.resolve("liacontract")).contains("a.LiaContract");
        assertThat(index.resolve("A.LIACONTRACT")).contains("a.LiaContract");
        assertThat(index.resolve("other")).isEmpty();
        assertThat(index.resolve("Other")).contains("a.Other");
    }
}
