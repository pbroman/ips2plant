package com.github.pbroman.ips2plant.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class IpsTreeWalkerTest {

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

    private static String assoc(String type, String target) {
        return "<Association associationType=\"" + type + "\" target=\"" + target
                + "\" targetRoleSingular=\"" + target + "\"/>";
    }

    private IpsTreeWalker walkerOver(Path scope, Path... others) {
        var all = new java.util.ArrayList<Path>(List.of(scope));
        all.addAll(List.of(others));
        return new IpsTreeWalker(IpsModelIndex.build(all, List.of(scope)));
    }

    @Test
    void descendants_depthLimitsAssociationHops() throws IOException {
        // given
        var d = dir("m");
        write(d, "Root", "", assoc("comp", "A"));
        write(d, "A", "", assoc("comp", "B"));
        write(d, "B", "", assoc("comp", "C"));
        write(d, "C", "");
        var walker = walkerOver(d);

        // when / then
        assertThat(walker.walk("Root", true, false, 1, false).keySet()).containsExactlyInAnyOrder("Root", "A");
        assertThat(walker.walk("Root", true, false, 2, false).keySet()).containsExactlyInAnyOrder("Root", "A", "B");
        assertThat(walker.walk("Root", true, false, null, false).keySet())
                .containsExactlyInAnyOrder("Root", "A", "B", "C");
    }

    @Test
    void descendants_followCompAssAndAggr() throws IOException {
        // given
        var d = dir("m");
        write(d, "Root", "", assoc("comp", "A"), assoc("ass", "B"), assoc("aggr", "C"));
        write(d, "A", "");
        write(d, "B", "");
        write(d, "C", "");

        // when
        var result = walkerOver(d).walk("Root", true, false, null, false);

        // then
        assertThat(result.keySet()).containsExactlyInAnyOrder("Root", "A", "B", "C");
    }

    @Test
    void descendants_sharedChildAndCycleTerminate() throws IOException {
        // given
        var d = dir("m");
        write(d, "Root", "", assoc("comp", "A"), assoc("comp", "B"));
        write(d, "A", "", assoc("comp", "Shared"));
        write(d, "B", "", assoc("comp", "Shared"));
        write(d, "Shared", "", assoc("comp", "Root"));

        // when
        var result = walkerOver(d).walk("Root", true, false, null, false);

        // then
        assertThat(result.keySet()).containsExactlyInAnyOrder("Root", "A", "B", "Shared");
    }

    @Test
    void descendants_followInheritedAssociationFromOutOfScopeSupertype() throws IOException {
        // given
        var local = dir("local");
        var dep = dir("dep");
        write(local, "Root", "supertype=\"ext.Base\"");
        write(local, "Part", "");
        write(dep, "ext.Base", "abstract=\"true\"", assoc("comp", "Part"));

        // when
        var result = walkerOver(local, dep).walk("Root", true, false, null, false);

        // then
        assertThat(result.keySet()).containsExactlyInAnyOrder("Root", "Part", "ext.Base");
    }

    @Test
    void descendants_dropOutOfScopeTargets() throws IOException {
        // given
        var local = dir("local");
        var dep = dir("dep");
        write(local, "Root", "", assoc("comp", "ext.Foreign"), assoc("comp", "Own"));
        write(local, "Own", "");
        write(dep, "ext.Foreign", "");

        // when
        var result = walkerOver(local, dep).walk("Root", true, false, null, false);

        // then
        assertThat(result.keySet()).containsExactlyInAnyOrder("Root", "Own");
    }

    @Test
    void descendants_abstractLeafPrunedUnlessIncluded() throws IOException {
        // given
        var d = dir("m");
        write(d, "Root", "", assoc("comp", "AbsLeaf"), assoc("comp", "Concrete"));
        write(d, "AbsLeaf", "abstract=\"true\"");
        write(d, "Concrete", "");
        var walker = walkerOver(d);

        // when / then
        assertThat(walker.walk("Root", true, false, null, false).keySet())
                .containsExactlyInAnyOrder("Root", "Concrete");
        assertThat(walker.walk("Root", true, false, null, true).keySet())
                .containsExactlyInAnyOrder("Root", "Concrete", "AbsLeaf");
    }

    @Test
    void descendants_abstractTargetExpandsToConcreteSubtypesAtSameDepth() throws IOException {
        // given
        var d = dir("m");
        write(d, "Root", "", assoc("comp", "AbsTarget"));
        write(d, "AbsTarget", "abstract=\"true\"");
        write(d, "ImplOne", "supertype=\"AbsTarget\"", assoc("comp", "Deep"));
        write(d, "ImplTwo", "supertype=\"AbsTarget\"");
        write(d, "Deep", "");

        // when
        var depthOne = walkerOver(d).walk("Root", true, false, 1, false);
        var depthTwo = walkerOver(d).walk("Root", true, false, 2, false);

        // then
        assertThat(depthOne.keySet()).containsExactlyInAnyOrder("Root", "AbsTarget", "ImplOne", "ImplTwo");
        assertThat(depthTwo.keySet()).containsExactlyInAnyOrder("Root", "AbsTarget", "ImplOne", "ImplTwo", "Deep");
    }

    @Test
    void descendants_concreteTargetAlsoReachesItsConcreteSubtypes() throws IOException {
        // given
        var d = dir("m");
        write(d, "Root", "", assoc("comp", "Peril"));
        write(d, "Peril", "");
        write(d, "SpecialPeril", "supertype=\"Peril\"");
        var walker = walkerOver(d);

        // when
        var result = walker.walk("Root", true, false, null, false).keySet();

        // then
        assertThat(result).containsExactlyInAnyOrder("Root", "Peril", "SpecialPeril");
    }

    @Test
    void ancestors_viaReverseComp() throws IOException {
        // given
        var d = dir("m");
        write(d, "Root", "", assoc("reverseComp", "Parent"));
        write(d, "Parent", "", assoc("comp", "Root"), assoc("reverseComp", "Grand"));
        write(d, "Grand", "", assoc("comp", "Parent"));
        write(d, "Unrelated", "");

        // when
        var result = walkerOver(d).walk("Root", false, true, null, false);

        // then
        assertThat(result.keySet()).containsExactlyInAnyOrder("Root", "Parent", "Grand");
    }

    @Test
    void ancestors_viaInverseOfCompAssAndAggrWithoutReverseDeclaration() throws IOException {
        // given
        var d = dir("m");
        write(d, "Root", "");
        write(d, "ByComp", "", assoc("comp", "Root"));
        write(d, "ByAss", "", assoc("ass", "Root"));
        write(d, "ByAggr", "", assoc("aggr", "Root"));
        write(d, "Unrelated", "", assoc("comp", "Other"));
        write(d, "Other", "");

        // when
        var result = walkerOver(d).walk("Root", false, true, null, false);

        // then
        assertThat(result.keySet()).containsExactlyInAnyOrder("Root", "ByComp", "ByAss", "ByAggr");
    }

    @Test
    void ancestors_depthLimited() throws IOException {
        // given
        var d = dir("m");
        write(d, "Root", "");
        write(d, "P1", "", assoc("comp", "Root"));
        write(d, "P2", "", assoc("comp", "P1"));

        // when
        var result = walkerOver(d).walk("Root", false, true, 1, false);

        // then
        assertThat(result.keySet()).containsExactlyInAnyOrder("Root", "P1");
    }

    @Test
    void ancestors_abstractOwnerWithConcreteSubtypeIsKept() throws IOException {
        // given
        var d = dir("m");
        write(d, "Root", "");
        write(d, "AbsParent", "abstract=\"true\"", assoc("comp", "Root"));
        write(d, "ConcreteParent", "supertype=\"AbsParent\"");

        // when
        var result = walkerOver(d).walk("Root", false, true, null, false);

        // then
        assertThat(result.keySet()).containsExactlyInAnyOrder("Root", "AbsParent");
    }

    @Test
    void ancestors_abstractOwnerWithoutConcreteSubtypeIsDropped() throws IOException {
        // given
        var d = dir("m");
        write(d, "Root", "");
        write(d, "AbsParent", "abstract=\"true\"", assoc("comp", "Root"));
        var walker = walkerOver(d);

        // when / then
        assertThat(walker.walk("Root", false, true, null, false).keySet()).containsExactly("Root");
        assertThat(walker.walk("Root", false, true, null, true).keySet())
                .containsExactlyInAnyOrder("Root", "AbsParent");
    }

    @Test
    void descendants_abstractDeadEndIsNotTraversed() throws IOException {
        // given
        var d = dir("m");
        write(d, "Root", "", assoc("comp", "AbsNoImpl"));
        write(d, "AbsNoImpl", "abstract=\"true\"", assoc("comp", "BehindAbstract"));
        write(d, "BehindAbstract", "");
        var walker = walkerOver(d);

        // when / then
        assertThat(walker.walk("Root", true, false, null, false).keySet()).containsExactly("Root");
        assertThat(walker.walk("Root", true, false, null, true).keySet())
                .containsExactlyInAnyOrder("Root", "AbsNoImpl", "BehindAbstract");
    }

    @Test
    void descendants_abstractWithConcreteSubtypeOutsideScopeIsADeadEnd() throws IOException {
        // given
        var local = dir("local");
        var dep = dir("dep");
        write(local, "Root", "", assoc("comp", "Abs"));
        write(local, "Abs", "abstract=\"true\"");
        write(dep, "ext.Impl", "supertype=\"Abs\"");

        // when
        var result = walkerOver(local, dep).walk("Root", true, false, null, false);

        // then
        assertThat(result.keySet()).containsExactly("Root");
    }

    @Test
    void merge_descendantsDoNotAddOutOfScopeOwnerOfInheritedAssociation() throws IOException {
        // given
        var local = dir("local");
        var dep = dir("dep");
        write(local, "Root", "supertype=\"ext.Base\"");
        write(local, "Part", "");
        write(dep, "ext.Base", "abstract=\"true\"", assoc("comp", "Part"));

        // when
        var result = walkerOver(local, dep).walk("Root", true, false, null, false, true);

        // then
        assertThat(result.keySet()).containsExactlyInAnyOrder("Root", "Part");
    }

    @Test
    void merge_ancestorsDoNotAddOutOfScopeOwner() throws IOException {
        // given
        var local = dir("local");
        var dep = dir("dep");
        write(local, "Root", "");
        write(local, "Contract", "supertype=\"ext.BaseContract\"");
        write(dep, "ext.BaseContract", "abstract=\"true\"", assoc("comp", "Root"));

        // when
        var result = walkerOver(local, dep).walk("Root", false, true, null, false, true);

        // then
        assertThat(result.keySet()).containsExactlyInAnyOrder("Root", "Contract");
    }

    @Test
    void ancestors_outOfScopeOwnerOfInheritedAssociationAddsInScopeInheritorAndOwner() throws IOException {
        // given
        var local = dir("local");
        var dep = dir("dep");
        write(local, "Root", "");
        write(local, "Contract", "supertype=\"ext.BaseContract\"");
        write(dep, "ext.BaseContract", "abstract=\"true\"", assoc("comp", "Root"));

        // when
        var result = walkerOver(local, dep).walk("Root", false, true, null, false);

        // then
        assertThat(result.keySet()).containsExactlyInAnyOrder("Root", "Contract", "ext.BaseContract");
    }

    @Test
    void ancestors_inScopeAbstractOwnerIsParentButItsSubtypesAreNot() throws IOException {
        // given
        var d = dir("m");
        write(d, "Root", "");
        write(d, "Base", "abstract=\"true\"", assoc("comp", "Root"));
        write(d, "Sub", "supertype=\"Base\"");

        // when
        var result = walkerOver(d).walk("Root", false, true, null, false);

        // then
        assertThat(result.keySet()).containsExactlyInAnyOrder("Root", "Base");
    }

    @Test
    void walk_bothDirectionsCombine() throws IOException {
        // given
        var d = dir("m");
        write(d, "Parent", "", assoc("comp", "Root"));
        write(d, "Root", "", assoc("comp", "Child"));
        write(d, "Child", "");

        // when
        var result = walkerOver(d).walk("Root", true, true, null, false);

        // then
        assertThat(result.keySet()).containsExactlyInAnyOrder("Parent", "Root", "Child");
    }

    @Test
    void walk_unknownOrOutOfScopeRootReturnsEmpty() throws IOException {
        // given
        var local = dir("local");
        var dep = dir("dep");
        write(local, "Own", "");
        write(dep, "ext.Foreign", "");
        var walker = walkerOver(local, dep);

        // when / then
        assertThat(walker.walk("Missing", true, true, null, false)).isEmpty();
        assertThat(walker.walk("ext.Foreign", true, true, null, false)).isEmpty();
    }

    @Test
    void merge_ancestorsDropSupertypesOfFoundClassesUnlessTheyAreArrowTargets() throws IOException {
        // given
        var d = dir("m");
        write(d, "RootBase", "abstract=\"true\"");
        write(d, "Root", "supertype=\"RootBase\"");
        write(d, "ParentBase", "abstract=\"true\"", assoc("comp", "RootBase"));
        write(d, "Parent", "supertype=\"ParentBase\"", assoc("comp", "Root"));

        // when
        var result = walkerOver(d).walk("Root", false, true, 1, false, true);

        // then
        assertThat(result.keySet()).containsExactlyInAnyOrder("Root", "Parent");
    }

    @Test
    void merge_descendantsReplaceAbstractTargetOfAnArrowByItsSubtypes() throws IOException {
        // given
        var d = dir("m");
        write(d, "Root", "", assoc("comp", "AbsTarget"));
        write(d, "AbsTarget", "abstract=\"true\"");
        write(d, "Impl", "supertype=\"AbsTarget\"");

        // when
        var result = walkerOver(d).walk("Root", true, false, null, false, true);

        // then
        assertThat(result.keySet()).containsExactlyInAnyOrder("Root", "Impl");
    }

    @Test
    void descendants_mergedSupertypes_abstractAssociationTargetIsReplacedByConcreteSubtype() throws IOException {
        // given
        var d = dir("m");
        write(d, "Root", "supertype=\"Base\"");
        write(d, "Base", "", assoc("comp", "Abs"));
        write(d, "Abs", "abstract=\"true\"");
        write(d, "Concrete", "supertype=\"Abs\"");

        // when
        var result = walkerOver(d).walk("Root", true, false, null, false, true);

        // then
        assertThat(result.keySet()).containsExactlyInAnyOrder("Root", "Concrete");
    }
}
