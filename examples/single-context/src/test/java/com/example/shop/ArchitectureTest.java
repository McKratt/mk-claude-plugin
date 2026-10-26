package com.example.shop;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

@AnalyzeClasses(packages = "com.example.shop", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    static final String[] ORM_AND_JSON = {
            "jakarta.persistence..", "javax.persistence..", "org.hibernate..",
            "org.eclipse.persistence..", "org.mybatis..",
            "com.fasterxml.jackson..", "tools.jackson..", "jakarta.json..", "com.google.gson..", "org.json.."};

    // Lombok (lombok..) is deliberately not forbidden anywhere in the domain.
    @ArchTest
    static final ArchRule domainHasNoOrmOrJson = noClasses()
            .that().resideInAPackage("..application.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(ORM_AND_JSON);

    @ArchTest
    static final ArchRule modelIsFrameworkFreeAndSelfContained = noClasses()
            .that().resideInAPackage("..application.domain.model..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application.domain.service..", "..application.port..", "..adapter..",
                    "org.springframework..");

    @ArchTest
    static final ArchRule servicesDoNotUseAdapters = noClasses()
            .that().resideInAPackage("..application.domain.service..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..");

    // @Service and @Component are forbidden on services; only @Transactional (with its Propagation/Isolation members) is allowed.
    @ArchTest
    static final ArchRule servicesUseSpringOnlyForTransactional = noClasses()
            .that().resideInAPackage("..application.domain.service..")
            .should().dependOnClassesThat(
                    resideInAPackage("org.springframework..")
                            .and(not(resideInAPackage("org.springframework.transaction.annotation"))));

    // @Transactional goes on methods, never on the class: a class-level annotation would open a
    // read-write transaction for every public method, including those that only read.
    @ArchTest
    static final ArchRule transactionalIsNeverOnAServiceClass = noClasses()
            .that().resideInAPackage("..application.domain.service..")
            .should().beAnnotatedWith(Transactional.class);

    @ArchTest
    static final ArchRule commonIsIndependent = noClasses()
            .that().resideInAPackage("..common..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application..", "..adapter..", "com.example.shop..configuration..");

    @ArchTest
    static final ArchRule applicationDoesNotDependOnAdapters = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..");

    @ArchTest
    static final ArchRule inboundAdaptersDoNotUseOutboundAdapters = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out..");

    @ArchTest
    static final ArchRule outboundAdaptersDoNotUseInboundAdapters = noClasses()
            .that().resideInAPackage("..adapter.out..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.in..");

    @ArchTest
    static final ArchRule nothingDependsOnConfiguration = noClasses()
            .that().resideOutsideOfPackage("com.example.shop..configuration..")
            .should().dependOnClassesThat().resideInAPackage("com.example.shop..configuration..");

    // Adapters are created by @Bean methods in the adapter's own @Configuration class, so they carry no
    // stereotype (@Component, @Service, @Repository...). Controllers are the exception: Spring MVC only
    // detects handlers annotated with @Controller / @RestController.
    @ArchTest
    static final ArchRule adaptersHaveNoSpringStereotype = noClasses()
            .that().resideInAPackage("..adapter..")
            .and().areNotAnnotatedWith(Configuration.class)
            .and().areNotMetaAnnotatedWith(Controller.class)
            .should().beMetaAnnotatedWith(Component.class);

    // ... which is what lets every adapter class stay package-private.
    @ArchTest
    static final ArchRule adaptersArePackagePrivate = classes()
            .that().resideInAPackage("..adapter..")
            .and().areTopLevelClasses()
            .should().notBePublic();

    @ArchTest
    static final ArchRule nobodyDependsOnConfigurationClasses = noClasses()
            .should().dependOnClassesThat().areAnnotatedWith(Configuration.class);

    @ArchTest
    static final ArchRule portsAreInterfaces = classes()
            .that().haveSimpleNameEndingWith("Port").or().haveSimpleNameEndingWith("UseCase")
            .should().beInterfaces();


}
