package com.example.shop;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

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

    @ArchTest
    static final ArchRule portsAreInterfaces = classes()
            .that().haveSimpleNameEndingWith("Port").or().haveSimpleNameEndingWith("UseCase")
            .should().beInterfaces();

    // Bounded contexts of the same component: order -> billing only through the DomainAdapter,
    // and only through billing's incoming ports.
    @ArchTest
    static final ArchRule orderReachesBillingOnlyThroughItsDomainAdapter = noClasses()
            .that().resideInAPackage("com.example.shop.sales.order..")
            .and().resideOutsideOfPackage("com.example.shop.sales.order.adapter.out.billing..")
            .should().dependOnClassesThat().resideInAPackage("com.example.shop.sales.billing..");

    @ArchTest
    static final ArchRule orderDomainAdapterUsesOnlyBillingIncomingPorts = classes()
            .that().resideInAPackage("com.example.shop.sales.order.adapter.out.billing..")
            .should().onlyDependOnClassesThat(
                    not(resideInAPackage("com.example.shop.sales.billing.."))
                            .or(resideInAPackage("com.example.shop.sales.billing.application.port.in..")));

    @ArchTest
    static final ArchRule billingDoesNotUseOrder = noClasses()
            .that().resideInAPackage("com.example.shop.sales.billing..")
            .should().dependOnClassesThat().resideInAPackage("com.example.shop.sales.order..");
}
