package com.example.shop;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/**
 * Checks that the rules in {@link ArchitectureTest} really fail on bad code. The bad classes live in the
 * test sources (package archfixtures) and are not part of the analysed application.
 */
class ArchitectureRulesDetectViolationsTest {

    private static final JavaClasses VIOLATIONS = new ClassFileImporter().importPackages("archfixtures");

    private static void assertViolation(ArchRule rule, JavaClasses classes) {
        assertTrue(rule.evaluate(classes).hasViolation(), "Expected a violation for rule: " + rule.getDescription());
    }

    @Test
    void springInTheModelIsDetected() {
        assertViolation(ArchitectureTest.modelIsFrameworkFreeAndSelfContained, VIOLATIONS);
    }

    @Test
    void ormInTheDomainIsDetected() {
        assertViolation(ArchitectureTest.domainHasNoOrmOrJson, VIOLATIONS);
    }

    @Test
    void classLevelTransactionalOnAServiceIsDetected() {
        assertViolation(ArchitectureTest.transactionalIsNeverOnAServiceClass, VIOLATIONS);
    }

    @Test
    void stereotypeOnAServiceIsDetected() {
        assertViolation(ArchitectureTest.servicesUseSpringOnlyForTransactional, VIOLATIONS);
    }

    @Test
    void serviceUsingAnAdapterIsDetected() {
        assertViolation(ArchitectureTest.servicesDoNotUseAdapters, VIOLATIONS);
        assertViolation(ArchitectureTest.applicationDoesNotDependOnAdapters, VIOLATIONS);
    }

    @Test
    void commonUsingTheApplicationIsDetected() {
        assertViolation(ArchitectureTest.commonIsIndependent, VIOLATIONS);
    }

    @Test
    void inboundAdapterUsingOutboundAdapterIsDetected() {
        assertViolation(ArchitectureTest.inboundAdaptersDoNotUseOutboundAdapters, VIOLATIONS);
    }

    @Test
    void anAdapterWithAStereotypeIsDetected() {
        assertViolation(ArchitectureTest.adaptersHaveNoSpringStereotype, VIOLATIONS);
    }

    @Test
    void aPublicAdapterIsDetected() {
        assertViolation(ArchitectureTest.adaptersArePackagePrivate, VIOLATIONS);
    }

    @Test
    void portSuffixAndIPrefixAreDetected() {
        assertViolation(ArchitectureTest.interfaceNamesFollowJavaConventions, VIOLATIONS);
    }

    @Test
    void implSuffixIsDetected() {
        assertViolation(ArchitectureTest.noImplSuffixOnClasses, VIOLATIONS);
    }

    @Test
    void aPortThatIsNotAnInterfaceIsDetected() {
        assertViolation(ArchitectureTest.portsAreInterfaces, VIOLATIONS);
    }
    private static final JavaClasses CONTEXT_VIOLATIONS = new ClassFileImporter().importPackages(
            "com.example.shop.sales.order.fixture", "com.example.shop.sales.billing.fixture");

    @Test
    void orderUsingBillingOutsideItsDomainAdapterIsDetected() {
        assertViolation(ArchitectureTest.orderReachesBillingOnlyThroughItsDomainAdapter, CONTEXT_VIOLATIONS);
    }

    @Test
    void billingUsingOrderIsDetected() {
        assertViolation(ArchitectureTest.billingDoesNotUseOrder, CONTEXT_VIOLATIONS);
    }
}
