package com.example.shop;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
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

    // Lombok (lombok..) and JSpecify (org.jspecify..) are deliberately not forbidden anywhere.
    @ArchTest
    static final ArchRule domainAndApplicationHaveNoOrmOrJson = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat().resideInAnyPackage(ORM_AND_JSON);

    // The domain (entities, value objects, optional domain services in domain.service) is plain Java:
    // no Spring at all, not even @Transactional, which belongs to the application services.
    @ArchTest
    static final ArchRule domainIsFrameworkFreeAndSelfContained = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application..", "..adapter..", "org.springframework..");

    @ArchTest
    static final ArchRule servicesDoNotUseAdapters = noClasses()
            .that().resideInAPackage("..application.service..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..");

    // Application services own the cross-cutting concerns of a use case: transactions (@Transactional, with its
    // Propagation/Isolation members) and method security (@PreAuthorize, @Secured...). Nothing else from Spring:
    // no @Service or @Component, the services are created by @Bean methods.
    @ArchTest
    static final ArchRule applicationUsesSpringOnlyForCrossCuttingConcerns = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat(
                    resideInAPackage("org.springframework..")
                            .and(not(resideInAnyPackage("org.springframework.transaction.annotation",
                                    "org.springframework.security.access.prepost",
                                    "org.springframework.security.access.annotation"))));

    // @Transactional goes on methods, never on the class: a class-level annotation would open a
    // read-write transaction for every public method, including those that only read.
    @ArchTest
    static final ArchRule transactionalIsNeverOnAServiceClass = noClasses()
            .that().resideInAPackage("..application.service..")
            .should().beAnnotatedWith(Transactional.class);

    @ArchTest
    static final ArchRule commonIsIndependent = noClasses()
            .that().resideInAPackage("..common..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..domain..", "..application..", "..adapter..", "com.example.shop..configuration..");

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

    // Outgoing HTTP goes through Spring's RestClient (built from the auto-configured RestClient.Builder),
    // never through a raw HTTP client.
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
