package eu.mm.software.photofinder.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Testy architektury wymuszające izolację warstw DDD oraz zasady CQRS.
 *
 * Dozwolony kierunek zależności:
 *
 *   interfaces  →  application  →  domain  ←  infrastructure
 *
 * Reguły są CELOWO bez wyjątków — test ma być czerwony dopóki kod
 * produkcyjny nie zostanie naprawiony. Lista znanych naruszeń do usunięcia:
 *
 *   1. PhotoAttributeRepository (domain)        → PhotoParamsDto (photoparams.infrastructure)
 *   2. AIProcessor (application.query)           → AiProvider (infrastructure.ai)
 *   3. PhotosAttributesQuery (application.query) → AiProvider (infrastructure.ai)
 *   4. PhotoCommandController (interfaces)       → UserCacheService (infrastructure.repository)
 *   5. PhotosQueryController (interfaces)        → AiProvider (infrastructure.ai)
 *   6. AuditPhotosQueryController (interfaces)   → AiProvider (infrastructure.ai)
 *   7. DescribeFormValidator (interfaces)        → AiProvider (infrastructure.ai)
 *   8. PhotosQueryController (interfaces)        → UserNotFoundException (user.infrastructure.repository)
 *   9. PhotoCommandController (interfaces)       → UserNotFoundException (user.infrastructure.repository)
 *  10. AuditPhotosQueryImpl (infrastructure)     → AuditPhotoStatsDto (interfaces.rest)
 */
class ArchitectureRulesTest {

    private static final String BASE_PACKAGE = "eu.mm.software.photofinder";

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    // -------------------------------------------------------------------------
    //  DDD – warstwa DOMAIN
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DDD – warstwa domain")
    class DomainLayerRules {

        @Test
        @DisplayName("domain NIE może importować infrastructure")
        void domainMustNotDependOnInfrastructure() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..infrastructure..")
                    .because("Warstwa domain musi być niezależna od szczegółów infrastrukturalnych.");
            rule.check(classes);
        }

        @Test
        @DisplayName("domain NIE może importować interfaces (REST)")
        void domainMustNotDependOnInterfaces() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..interfaces..")
                    .because("Warstwa domain nie może znać warstwy prezentacji.");
            rule.check(classes);
        }

        @Test
        @DisplayName("domain NIE może importować application")
        void domainMustNotDependOnApplication() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..application..")
                    .because("Domain jest najbardziej wewnętrzną warstwą — nie zależy od żadnej innej.");
            rule.check(classes);
        }
    }

    // -------------------------------------------------------------------------
    //  DDD – warstwa APPLICATION
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DDD – warstwa application")
    class ApplicationLayerRules {

        @Test
        @DisplayName("application NIE może importować infrastructure")
        void applicationMustNotDependOnInfrastructure() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..infrastructure..")
                    .because("Warstwa application nie może zależeć od szczegółów infrastrukturalnych.");
            rule.check(classes);
        }

        @Test
        @DisplayName("application NIE może importować interfaces (REST)")
        void applicationMustNotDependOnInterfaces() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..interfaces..")
                    .because("Warstwa application nie może znać warstwy prezentacji.");
            rule.check(classes);
        }
    }

    // -------------------------------------------------------------------------
    //  DDD – warstwa INTERFACES
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DDD – warstwa interfaces")
    class InterfacesLayerRules {

        @Test
        @DisplayName("interfaces NIE może importować infrastructure bezpośrednio")
        void interfacesMustNotDependOnInfrastructure() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..interfaces..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..infrastructure..")
                    .because("Kontrolery REST (interfaces) powinny komunikować się przez " +
                             "serwisy application, a nie przez obiekty infrastrukturalne.");
            rule.check(classes);
        }
    }

    // -------------------------------------------------------------------------
    //  DDD – warstwa INFRASTRUCTURE
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DDD – warstwa infrastructure")
    class InfrastructureLayerRules {

        @Test
        @DisplayName("infrastructure NIE może importować interfaces (REST)")
        void infrastructureMustNotDependOnInterfaces() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..infrastructure..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..interfaces..")
                    .because("Warstwa infrastructure nie może importować warstwy interfaces/REST.");
            rule.check(classes);
        }
    }

    // -------------------------------------------------------------------------
    //  CQRS – warstwa application
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("CQRS – warstwa application")
    class CqrsApplicationRules {

        @Test
        @DisplayName("application.command NIE może importować application.query")
        void commandMustNotDependOnQuery() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..application.command..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..application.query..")
                    .because("CQRS: serwisy komend nie mogą zależeć od serwisów zapytań.");
            rule.check(classes);
        }

        @Test
        @DisplayName("application.query NIE może importować application.command")
        void queryMustNotDependOnCommand() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..application.query..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..application.command..")
                    .because("CQRS: serwisy zapytań nie mogą zależeć od serwisów komend.");
            rule.check(classes);
        }
    }

    // -------------------------------------------------------------------------
    //  CQRS – kontrolery interfaces.rest
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("CQRS – kontrolery interfaces.rest")
    class CqrsControllersRules {

        @Test
        @DisplayName("Kontrolery *Command* NIE mogą używać serwisów application.query")
        void commandControllersMustNotUseQueryServices() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..interfaces.rest..")
                    .and().haveSimpleNameContaining("Command")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..application.query..")
                    .because("CQRS: kontrolery komend (*Command*) nie powinny wywoływać serwisów zapytań.");
            rule.check(classes);
        }

        @Test
        @DisplayName("Kontrolery *Query* NIE mogą używać serwisów application.command")
        void queryControllersMustNotUseCommandServices() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..interfaces.rest..")
                    .and().haveSimpleNameContaining("Query")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..application.command..")
                    .because("CQRS: kontrolery zapytań (*Query*) nie powinny wywoływać serwisów komend.");
            rule.check(classes);
        }
    }
}
