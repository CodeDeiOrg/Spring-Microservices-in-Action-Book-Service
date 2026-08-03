package com.onlinelibrary.book;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "com.onlinelibrary.book")
class ArchitectureTest {

    // Controllers must live in the controller package
    @ArchTest
    static final ArchRule controllers_reside_in_controller_package =
            classes().that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .should().resideInAPackage("..controller..");

    // Controllers must not access repositories directly — only through services
    @ArchTest
    static final ArchRule controllers_do_not_access_repositories =
            noClasses().that().resideInAPackage("..controller..")
                    .should().accessClassesThat().resideInAPackage("..repository..");

    // Services must not depend on controllers
    @ArchTest
    static final ArchRule services_do_not_depend_on_controllers =
            noClasses().that().resideInAPackage("..service..")
                    .should().dependOnClassesThat().resideInAPackage("..controller..");

    // Exceptions must live in the exception package
    @ArchTest
    static final ArchRule exceptions_reside_in_exception_package =
            classes().that().areAssignableTo(RuntimeException.class)
                    .and().resideInAPackage("com.onlinelibrary.book..")
                    .should().resideInAPackage("..exception..");

    // No cyclic dependencies between top-level packages
    @ArchTest
    static final ArchRule no_cycles =
            slices().matching("com.onlinelibrary.book.(*)..").should().beFreeOfCycles();
}