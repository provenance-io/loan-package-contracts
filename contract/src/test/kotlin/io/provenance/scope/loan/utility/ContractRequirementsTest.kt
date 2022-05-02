package io.provenance.scope.loan.utility

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContainIgnoringCase
import io.kotest.matchers.types.shouldBeTypeOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import io.provenance.scope.loan.test.shouldHaveViolationCount
import io.provenance.scope.loan.test.LoanPackageArbs

class ContractRequirementsTest : WordSpec({
    /* Helpers */
    fun getExpectedViolationCount(enforcements: List<ContractEnforcement>) =
        enforcements.fold(
            emptyMap<ContractViolation, UInt>() to 0U
        ) { (violationMap, overallCount), (rule, violationReport) ->
            if (!rule) {
                violationMap.getOrDefault(violationReport, 0U).let { currentIndividualCount ->
                    if (currentIndividualCount == 0U) {
                        violationMap.plus(violationReport to 1U) to overallCount + 1U
                    } else {
                        violationMap.plus(violationReport to currentIndividualCount + 1U) to overallCount
                    }
                }
            } else {
                violationMap to overallCount
            }
        }.second
    /* Tests */
    "validateRequirements" When {
        "accepting a list of enforcements" should {
            "accept infix syntax" {
                checkAll<Boolean, ContractViolation> { rule, violationReport ->
                    (rule orError violationReport).shouldBeTypeOf<ContractEnforcement>()
                    (rule orError violationReport) shouldBe ContractEnforcement(rule, violationReport)
                }
            }
            "return state violations only for failed conditions" {
                checkAll(Arb.list(LoanPackageArbs.contractEnforcement)) { enforcementList ->
                    val expectedOverallViolationCount = getExpectedViolationCount(enforcementList)
                    if (expectedOverallViolationCount > 0U) {
                        shouldThrow<IllegalContractStateException> {
                            validateRequirements(ContractRequirementType.LEGAL_SCOPE_STATE, *enforcementList.toTypedArray())
                        }.let { exception ->
                            exception.message shouldContainIgnoringCase "state was invalid" // TODO: Make better check
                        }
                    } else {
                        shouldNotThrow<IllegalContractStateException> {
                            validateRequirements(ContractRequirementType.LEGAL_SCOPE_STATE, *enforcementList.toTypedArray())
                        }
                    }
                }
            }
            "return input violations only for failed conditions" {
                checkAll(Arb.list(LoanPackageArbs.contractEnforcement)) { enforcementList ->
                    val expectedOverallViolationCount = getExpectedViolationCount(enforcementList)
                    if (expectedOverallViolationCount > 0U) {
                        shouldThrow<ContractViolationException> {
                            validateRequirements(ContractRequirementType.VALID_INPUT, *enforcementList.toTypedArray())
                        }.let { exception ->
                            exception shouldHaveViolationCount expectedOverallViolationCount
                        }
                    } else {
                        shouldNotThrow<ContractViolationException> {
                            validateRequirements(ContractRequirementType.VALID_INPUT, *enforcementList.toTypedArray())
                        }
                    }
                }
            }
        }
        "invoked with a function body" should {
            "return state violations only for failed conditions" {
                checkAll(
                    Arb.list(LoanPackageArbs.contractEnforcement),
                    Arb.list(LoanPackageArbs.contractEnforcement),
                ) { enforcementListA, enforcementListB ->
                    val expectedOverallViolationCount = getExpectedViolationCount(enforcementListA + enforcementListB)
                    if (expectedOverallViolationCount > 0U) {
                        shouldThrow<IllegalContractStateException> {
                            validateRequirements(ContractRequirementType.LEGAL_SCOPE_STATE) {
                                5 shouldBeGreaterThanOrEqual 4
                                requireThat(*enforcementListA.toTypedArray())
                                3 shouldBeLessThan 4
                                requireThat(*enforcementListB.toTypedArray())
                                2 shouldBeInRange 0..9
                            }
                        }.let { exception ->
                            exception.message shouldContainIgnoringCase "state was invalid" // TODO: Make better check
                        }
                    } else {
                        shouldNotThrow<ContractViolationException> {
                            validateRequirements(ContractRequirementType.VALID_INPUT) {
                                2 shouldBeInRange 0..9
                                requireThat(*enforcementListB.toTypedArray())
                                5 shouldBeGreaterThanOrEqual 4
                                requireThat(*enforcementListA.toTypedArray())
                                3 shouldBeLessThan 4
                            }
                        }
                    }
                }
            }
            "return input violations only for failed conditions" {
                checkAll(
                    Arb.list(LoanPackageArbs.contractEnforcement),
                    Arb.list(LoanPackageArbs.contractEnforcement),
                ) { enforcementListA, enforcementListB ->
                    val expectedOverallViolationCount = getExpectedViolationCount(enforcementListA + enforcementListB)
                    if (expectedOverallViolationCount > 0U) {
                        shouldThrow<ContractViolationException> {
                            validateRequirements(ContractRequirementType.VALID_INPUT) {
                                5 shouldBeGreaterThanOrEqual 4
                                requireThat(*enforcementListB.toTypedArray())
                                3 shouldBeLessThan 4
                                requireThat(*enforcementListA.toTypedArray())
                                2 shouldBeInRange 0..9
                            }
                        }.let { exception ->
                            exception shouldHaveViolationCount expectedOverallViolationCount
                        }
                    } else {
                        shouldNotThrow<ContractViolationException> {
                            validateRequirements(ContractRequirementType.VALID_INPUT) {
                                2 shouldBeInRange 0..9
                                requireThat(*enforcementListA.toTypedArray())
                                5 shouldBeGreaterThanOrEqual 4
                                requireThat(*enforcementListB.toTypedArray())
                                3 shouldBeLessThan 4
                            }
                        }
                    }
                }
            }
        }
    }
})
