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
    void should_detect_a_violation_when_the_model_uses_spring() {
        assertViolation(ArchitectureTest.modelIsFrameworkFreeAndSelfContained, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_the_domain_uses_an_orm() {
        assertViolation(ArchitectureTest.domainHasNoOrmOrJson, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_a_service_class_is_annotated_transactional() {
        assertViolation(ArchitectureTest.transactionalIsNeverOnAServiceClass, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_a_service_has_a_stereotype() {
        assertViolation(ArchitectureTest.servicesUseSpringOnlyForTransactional, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_a_service_uses_an_adapter() {
        assertViolation(ArchitectureTest.servicesDoNotUseAdapters, VIOLATIONS);
        assertViolation(ArchitectureTest.applicationDoesNotDependOnAdapters, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_common_uses_the_application() {
        assertViolation(ArchitectureTest.commonIsIndependent, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_an_inbound_adapter_uses_an_outbound_adapter() {
        assertViolation(ArchitectureTest.inboundAdaptersDoNotUseOutboundAdapters, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_an_adapter_has_a_stereotype() {
        assertViolation(ArchitectureTest.adaptersHaveNoSpringStereotype, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_an_adapter_is_public() {
        assertViolation(ArchitectureTest.adaptersArePackagePrivate, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_an_interface_is_named_with_a_port_suffix_or_i_prefix() {
        assertViolation(ArchitectureTest.interfaceNamesFollowJavaConventions, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_a_class_has_an_impl_suffix() {
        assertViolation(ArchitectureTest.noImplSuffixOnClasses, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_a_port_is_not_an_interface() {
        assertViolation(ArchitectureTest.portsAreInterfaces, VIOLATIONS);
    }
    private static final JavaClasses CONTEXT_VIOLATIONS = new ClassFileImporter().importPackages(
            "com.example.shop.sales.order.fixture", "com.example.shop.sales.billing.fixture");

    @Test
    void should_detect_a_violation_when_order_uses_billing_outside_its_domain_adapter() {
        assertViolation(ArchitectureTest.orderReachesBillingOnlyThroughItsDomainAdapter, CONTEXT_VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_billing_uses_order() {
        assertViolation(ArchitectureTest.billingDoesNotUseOrder, CONTEXT_VIOLATIONS);
    }
}
