package com.example.shop;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/**
 * Checks that the rules in {@link ArchitectureTest} really fail on bad code. The bad classes live in the
 * test sources (package archfixtures) and are not part of the analyzed application.
 */
class ArchitectureRulesDetectViolationsTest {

    private static final JavaClasses VIOLATIONS = new ClassFileImporter().importPackages("archfixtures");

    private static void assertViolation(ArchRule rule, JavaClasses classes) {
        assertThat(rule.evaluate(classes).hasViolation())
                .as("Expected a violation for rule: " + rule.getDescription())
                .isTrue();
    }

    @Test
    void should_detect_a_violation_when_the_domain_uses_helidon() {
        assertViolation(ArchitectureTest.domainAndApplicationAreFrameworkFree, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_the_domain_uses_json() {
        assertViolation(ArchitectureTest.domainAndApplicationHaveNoOrmOrJson, VIOLATIONS);
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
    void should_detect_a_violation_when_an_adapter_uses_a_configuration_class() {
        assertViolation(ArchitectureTest.onlyTheCompositionRootUsesConfiguration, VIOLATIONS);
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
    void should_detect_a_violation_when_a_raw_http_client_is_used() {
        assertViolation(ArchitectureTest.noRawHttpClient, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_the_application_uses_the_shared_db_client() {
        assertViolation(ArchitectureTest.sharedPersistenceIsUsedOnlyByPersistenceAdapters, VIOLATIONS);
    }

    private static final JavaClasses CONTEXT_VIOLATIONS = new ClassFileImporter().importPackages(
            "com.example.shop.order.fixture", "com.example.shop.billing.fixture");

    @Test
    void should_detect_a_violation_when_order_uses_billing_outside_its_domain_adapter() {
        assertViolation(ArchitectureTest.orderReachesBillingOnlyThroughItsDomainAdapter, CONTEXT_VIOLATIONS);
        assertViolation(ArchitectureTest.orderUsesOnlyBillingIncomingPorts, CONTEXT_VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_billing_uses_order() {
        assertViolation(ArchitectureTest.billingDoesNotUseOrder, CONTEXT_VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_a_port_is_not_an_interface() {
        assertViolation(ArchitectureTest.portsAreInterfaces, VIOLATIONS);
    }
}
