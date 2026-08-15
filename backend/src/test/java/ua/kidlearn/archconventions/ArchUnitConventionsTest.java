package ua.kidlearn.archconventions;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

/**
 * Structural conventions from docs/CONVENTIONS.md, enforced as a real test rather than
 * review-only checklist items — see that doc for which conventions are enforced here vs. left to
 * per-feature tests or human review (ArchUnit can't express behavioral rules like "ownership
 * failure returns 404" or "no parent PII in responses").
 *
 * DoNotIncludeTests: these are all statements about production architecture (module boundaries,
 * DI style, controller/service layering). Test classes routinely use @Autowired field injection
 * for MockMvc/repositories — the standard Spring Boot Test idiom, not a violation of "constructor
 * injection only" (which is about how main code wires its own collaborators).
 */
@AnalyzeClasses(packages = "ua.kidlearn", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchUnitConventionsTest {

	@ArchTest
	static final ArchRule no_cyclic_dependencies_between_packages = slices()
			.matching("ua.kidlearn.(*)..")
			.namingSlices("$1")
			.should().beFreeOfCycles()
			.because("a modular monolith stays modular only if its modules don't import each other "
					+ "in both directions — see docs/Технічний_фундамент.md §1's module list");

	@ArchTest
	static final ArchRule controllers_must_not_access_repositories_directly = noClasses()
			.that().areAnnotatedWith(RestController.class)
			.should().accessClassesThat().areAssignableTo(Repository.class)
			.because("ownership/authorization checks live in the @Service layer (see ChildService, "
					+ "GroupService, etc.) — a controller reaching into a repository directly would bypass "
					+ "them, including the 404-not-403 ownership-failure convention");

	@ArchTest
	static final ArchRule no_field_injection = noFields()
			.should().beAnnotatedWith(Autowired.class)
			.because("constructor injection only — see docs/CONVENTIONS.md");

	@ArchTest
	static final ArchRule rest_controllers_are_named_consistently = classes()
			.that().areAnnotatedWith(RestController.class)
			.should().haveSimpleNameEndingWith("Controller");

	@ArchTest
	static final ArchRule services_are_named_consistently = classes()
			.that().areAnnotatedWith(Service.class)
			.should().haveSimpleNameEndingWith("Service");

	// "Feature" packages: everything except the foundational/infra ones (config, ratelimit,
	// scenario, users, common) that config/scenario are allowed to depend on. Kept as an explicit
	// list rather than "everything except config/scenario" so a genuinely new infra package
	// doesn't have to fight this rule to exist.
	private static final String[] FEATURE_PACKAGES = {
			"ua.kidlearn.admin..", "ua.kidlearn.aipipeline..", "ua.kidlearn.attempts..",
			"ua.kidlearn.audit..", "ua.kidlearn.auth..", "ua.kidlearn.children..",
			"ua.kidlearn.consents..", "ua.kidlearn.devauth..", "ua.kidlearn.devseed..",
			"ua.kidlearn.groups..", "ua.kidlearn.lessons..",
	};

	@ArchTest
	static final ArchRule config_and_scenario_stay_leaves = noClasses()
			.that().resideInAnyPackage("ua.kidlearn.config..", "ua.kidlearn.scenario..")
			.should().dependOnClassesThat().resideInAnyPackage(FEATURE_PACKAGES)
			.because("config and scenario are foundational — feature packages depend on them, never "
					+ "the reverse, or every feature drags every other feature in transitively");

}
