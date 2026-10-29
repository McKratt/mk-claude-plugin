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

    // Lombok (lombok..) and JSpecify (org.jspecify..) are deliberately not forbidden in domain and application.
    @ArchTest
    static final ArchRule domainAndApplicationHaveNoOrmOrJson = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat().resideInAnyPackage(ORM_AND_JSON);

    // Domain and application are free of Helidon and of any container API. The application services own the
    // transaction boundary, through an outgoing port (TransactionRunner).
    @ArchTest
    static final ArchRule domainAndApplicationAreFrameworkFree = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "io.helidon..", "jakarta.inject..", "jakarta.enterprise..", "jakarta.ws.rs..",
                    "org.springframework..");

    @ArchTest
    static final ArchRule domainIsSelfContained = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..adapter..");

    @ArchTest
    static final ArchRule servicesDoNotUseAdapters = noClasses()
            .that().resideInAPackage("..application.service..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..");

    @ArchTest
    static final ArchRule commonIsIndependent = noClasses()
            .that().resideInAPackage("..common..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..domain..", "..application..", "..adapter..", "..configuration..");

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

    // Without a container, the configuration classes are the composition root: only other configuration
    // classes and the main class may call them.
    @ArchTest
    static final ArchRule onlyTheCompositionRootUsesConfiguration = noClasses()
            .that().resideOutsideOfPackage("..configuration..")
            .and().haveSimpleNameNotEndingWith("Configuration")
            .and().doNotHaveFullyQualifiedName("com.example.shop.ShopApplication")
            .should().dependOnClassesThat().resideInAPackage("..configuration..")
            .orShould().dependOnClassesThat().haveSimpleNameEndingWith("AdapterConfiguration");

    // Each adapter package exposes one public *AdapterConfiguration that creates its adapters, so every
    // adapter class stays package-private.
    @ArchTest
    static final ArchRule adaptersArePackagePrivate = classes()
            .that().resideInAPackage("..adapter..")
            .and().areTopLevelClasses()
            .and().haveSimpleNameNotEndingWith("AdapterConfiguration")
            .should().notBePublic();

    // The application's shared DbClient (common.persistence) is technology: only persistence adapters and
    // configuration classes may use it, never the application.
    @ArchTest
    static final ArchRule sharedPersistenceIsUsedOnlyByPersistenceAdapters = noClasses()
            .that().resideOutsideOfPackages("..adapter.out.persistence..", "..configuration..", "..common.persistence..")
            .should().dependOnClassesThat().resideInAPackage("..common.persistence..");

    // Bounded contexts of the same application: order -> billing only through the DomainAdapter, and only
    // through billing's incoming ports. The configuration package, outside both contexts, wires them.
    @ArchTest
    static final ArchRule orderReachesBillingOnlyThroughItsDomainAdapter = noClasses()
            .that().resideInAPackage("com.example.shop.order..")
            .and().resideOutsideOfPackage("com.example.shop.order.adapter.out.billing..")
            .should().dependOnClassesThat().resideInAPackage("com.example.shop.billing..");

    @ArchTest
    static final ArchRule orderUsesOnlyBillingIncomingPorts = classes()
            .that().resideInAPackage("com.example.shop.order..")
            .should().onlyDependOnClassesThat(
                    not(resideInAPackage("com.example.shop.billing.."))
                            .or(resideInAPackage("com.example.shop.billing.application.port.in..")));

    @ArchTest
    static final ArchRule billingDoesNotUseOrder = noClasses()
            .that().resideInAPackage("com.example.shop.billing..")
            .should().dependOnClassesThat().resideInAPackage("com.example.shop.order..");

    // Outgoing HTTP goes through Helidon's WebClient, never through a raw HTTP client.
    @ArchTest
    static final ArchRule noRawHttpClient = noClasses()
            .should().dependOnClassesThat().resideInAnyPackage("java.net.http..", "org.apache.hc..", "okhttp3..")
            .orShould().dependOnClassesThat().haveFullyQualifiedName("java.net.HttpURLConnection");

    @ArchTest
    static final ArchRule portsAreInterfaces = classes()
            .that().resideInAPackage("..application.port..")
            .and().areNotAssignableTo(Record.class)
            .should().beInterfaces();

    // Interface names follow the usual Java conventions (https://www.baeldung.com/java-interface-naming-conventions):
    // a noun or an adjective, no "I" prefix, no "Impl" suffix, and no Port / UseCase suffix.
    @ArchTest
    static final ArchRule interfaceNamesFollowJavaConventions = noClasses()
            .that().areInterfaces()
            .should().haveSimpleNameEndingWith("Port")
            .orShould().haveSimpleNameEndingWith("UseCase")
            .orShould().haveSimpleNameEndingWith("Impl")
            .orShould().haveNameMatching(".*[.]I[A-Z][^.]*");

    @ArchTest
    static final ArchRule noImplSuffixOnClasses = noClasses()
            .should().haveSimpleNameEndingWith("Impl");
}
