package com.jnzader.apigen.core.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Architecture tests to enforce layered architecture and naming conventions. Uses ArchUnit to
 * validate the codebase structure.
 *
 * <p>Note: These tests use allowEmptyShould(true) because apigen-core is a library that provides
 * base classes. Consumers will implement the actual layers (domain, application, infrastructure).
 * The rules will be enforced when the packages exist.
 */
@DisplayName("Architecture Tests")
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses =
                new ClassFileImporter()
                        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                        .importPackages("com.jnzader.apigen.core");
    }

    @Nested
    @DisplayName("Layered Architecture")
    class LayeredArchitectureTests {

        @Test
        @DisplayName("should follow layered architecture: domain -> application -> infrastructure")
        void shouldFollowLayeredArchitecture() {
            layeredArchitecture()
                    .consideringAllDependencies()
                    .layer("Domain")
                    .definedBy("..domain..")
                    .optionalLayer("Application")
                    .definedBy("..application..")
                    .optionalLayer("Infrastructure")
                    .definedBy("..infrastructure..")
                    .whereLayer("Domain")
                    .mayNotAccessAnyLayer()
                    .whereLayer("Application")
                    .mayOnlyAccessLayers("Domain")
                    .whereLayer("Infrastructure")
                    .mayOnlyAccessLayers("Application", "Domain")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Naming Conventions")
    class NamingConventionTests {

        @Test
        @DisplayName("services should be suffixed with 'Service' or 'ServiceImpl'")
        void servicesShouldHaveProperSuffix() {
            classes()
                    .that()
                    .resideInAPackage("..application.service..")
                    .and()
                    .areNotInterfaces()
                    .should()
                    .haveSimpleNameEndingWith("ServiceImpl")
                    .orShould()
                    .haveSimpleNameEndingWith("Service")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("controllers should be suffixed with 'Controller' or 'ControllerImpl'")
        void controllersShouldHaveProperSuffix() {
            classes()
                    .that()
                    .resideInAPackage("..infrastructure.controller..")
                    .and()
                    .areNotInterfaces()
                    .should()
                    .haveSimpleNameEndingWith("Controller")
                    .orShould()
                    .haveSimpleNameEndingWith("ControllerImpl")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("exceptions should be suffixed with 'Exception'")
        void exceptionsShouldHaveProperSuffix() {
            classes()
                    .that()
                    .resideInAPackage("..domain.exception..")
                    .should()
                    .haveSimpleNameEndingWith("Exception")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("DTOs should be suffixed with 'DTO'")
        void dtosShouldHaveProperSuffix() {
            classes()
                    .that()
                    .resideInAPackage("..application.dto..")
                    .and()
                    .areNotInterfaces()
                    .and()
                    .doNotHaveSimpleName("ValidationGroups")
                    .should()
                    .haveSimpleNameEndingWith("DTO")
                    .orShould()
                    .haveSimpleNameEndingWith("Request")
                    .orShould()
                    .haveSimpleNameEndingWith("Response")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Dependency Rules")
    class DependencyRuleTests {

        @Test
        @DisplayName("domain should not depend on Spring framework")
        void domainShouldNotDependOnSpring() {
            noClasses()
                    .that()
                    .resideInAPackage("..domain.entity..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("org.springframework..")
                    .because("Domain entities should be framework-agnostic (except JPA)")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("domain exceptions should not depend on infrastructure")
        void domainExceptionsShouldNotDependOnInfrastructure() {
            noClasses()
                    .that()
                    .resideInAPackage("..domain.exception..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..infrastructure..")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Package Structure")
    class PackageStructureTests {

        @Test
        @DisplayName("events should reside in domain.event package")
        void eventsShouldResideInDomainEventPackage() {
            classes()
                    .that()
                    .haveSimpleNameEndingWith("Event")
                    .and()
                    .areNotInterfaces()
                    .should()
                    .resideInAPackage("..domain.event..")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("configurations should reside in infrastructure.config package")
        void configurationsShouldResideInInfrastructureConfigPackage() {
            classes()
                    .that()
                    .haveSimpleNameEndingWith("Config")
                    .should()
                    .resideInAPackage("..infrastructure.config..")
                    .orShould()
                    .resideInAPackage("..autoconfigure..")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }
}
