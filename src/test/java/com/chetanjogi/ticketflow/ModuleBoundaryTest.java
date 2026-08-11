package com.chetanjogi.ticketflow;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

class ModuleBoundaryTest {
    // Enforces ADR-001: modules (booking, payment, notification, ai) must stay
    // independent so they can later be extracted into separate microservices.
    // No module may reference another module's classes. Proven to catch violations
    // during issue #4 (a deliberate payment->booking dependency was flagged, then removed).
    @Test
    void modulesShouldNotDependOnEachOther() {
        JavaClasses importedClasses =
                new ClassFileImporter().importPackages("com.chetanjogi.ticketflow");

        Architectures.LayeredArchitecture rule = layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .withOptionalLayers(true)
                .layer("Booking").definedBy("com.chetanjogi.ticketflow.booking..")
                .layer("Payment").definedBy("com.chetanjogi.ticketflow.payment..")
                .layer("Notification").definedBy("com.chetanjogi.ticketflow.notification..")
                .layer("Ai").definedBy("com.chetanjogi.ticketflow.ai..")

                .whereLayer("Booking").mayNotBeAccessedByAnyLayer()
                .whereLayer("Payment").mayNotBeAccessedByAnyLayer()
                .whereLayer("Notification").mayNotBeAccessedByAnyLayer()
                .whereLayer("Ai").mayNotBeAccessedByAnyLayer();

        rule.check(importedClasses);
    }
}