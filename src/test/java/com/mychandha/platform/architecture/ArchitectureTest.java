package com.mychandha.platform.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

@AnalyzeClasses(
        packages = "com.mychandha.platform",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule controllers_do_not_access_jdbc =
            noClasses().that().haveSimpleNameEndingWith("Controller")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework.jdbc..");

    @ArchTest
    static final ArchRule filters_are_final =
            classes().that().haveSimpleNameEndingWith("Filter")
                    .should(new ArchCondition<>("be final") {
                        @Override
                        public void check(JavaClass item, ConditionEvents events) {
                            events.add(new SimpleConditionEvent(
                                    item,
                                    item.getModifiers().contains(JavaModifier.FINAL),
                                    item.getName() + " is not final"));
                        }
                    });

    @ArchTest
    static final ArchRule api_controllers_stay_in_platform_modules =
            classes().that().haveSimpleNameEndingWith("Controller")
                    .should().resideInAnyPackage(
                            "..identity..", "..tenancy..", "..security..");
}
