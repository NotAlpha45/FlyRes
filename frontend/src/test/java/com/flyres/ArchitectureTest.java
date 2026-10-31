package com.flyres;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Enforces the layering rules from CLAUDE.md. Lives in the frontend module because only
 * its test classpath sees both modules. A failure here means the code is wrong, not the test.
 */
@AnalyzeClasses(packages = "com.flyres", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String UI = "com.flyres.ui..";

    /** Rule 1: the UI talks to the backend only through the facade, controls, domain and exceptions. */
    @ArchTest
    static final ArchRule uiUsesOnlyPublicBackendApi = noClasses()
            .that().resideInAPackage(UI)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.flyres.repository..",
                    "com.flyres.data..",
                    "com.flyres.common.security..",
                    "com.flyres.common.validation..",
                    "java.sql..",
                    "org.sqlite..")
            .allowEmptyShould(true);

    /** Rule 2: the backend has no JavaFX dependency. */
    @ArchTest
    static final ArchRule backendDoesNotUseJavaFx = noClasses()
            .that().resideOutsideOfPackage(UI)
            .should().dependOnClassesThat().resideInAnyPackage("javafx..", "atlantafx..")
            .allowEmptyShould(true);

    /** Rule 3: controls depend on repository interfaces, never on the SQLite implementations. */
    @ArchTest
    static final ArchRule controlsDoNotUseSqliteImplementations = noClasses()
            .that().resideInAPackage("com.flyres.control..")
            .should().dependOnClassesThat().resideInAPackage("com.flyres.data.sqlite..")
            .allowEmptyShould(true);

    /** Rule 4: the domain depends only on the domain, common and the JDK. */
    @ArchTest
    static final ArchRule domainDependsOnlyOnDomainAndCommon = classes()
            .that().resideInAPackage("com.flyres.domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                    "com.flyres.domain..", "com.flyres.common..", "java..")
            .allowEmptyShould(true);

    /** Rule 5 (bytecode proxy): only the data layer touches JDBC, so SQL cannot live anywhere else. */
    @ArchTest
    static final ArchRule onlyDataLayerUsesJdbc = noClasses()
            .that().resideOutsideOfPackage("com.flyres.data..")
            .should().dependOnClassesThat().resideInAnyPackage("java.sql..", "org.sqlite..")
            .allowEmptyShould(true);

    /** Naming: JavaFX controllers are *ViewController so they never collide with BCE controls. */
    @ArchTest
    static final ArchRule uiControllersAreViewControllers = classes()
            .that().resideInAPackage(UI).and().haveSimpleNameEndingWith("Controller")
            .should().haveSimpleNameEndingWith("ViewController")
            .allowEmptyShould(true);

    /** Naming: BCE control classes live in the control package. */
    @ArchTest
    static final ArchRule controlsLiveInControlPackage = classes()
            .that().resideOutsideOfPackage(UI).and().haveSimpleNameEndingWith("Controller")
            .should().resideInAPackage("com.flyres.control")
            .allowEmptyShould(true);
}
