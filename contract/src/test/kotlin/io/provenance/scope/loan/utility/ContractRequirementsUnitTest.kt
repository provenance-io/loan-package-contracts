package io.provenance.scope.loan.utility

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainIgnoringCase
import io.kotest.matchers.types.shouldBeTypeOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.pair
import io.kotest.property.arbitrary.set
import io.kotest.property.checkAll
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidDocumentMetadata
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidServicingData
import io.provenance.scope.loan.test.PrimitiveArbs.anyContractEnforcement
import io.provenance.scope.loan.test.shouldHaveViolationCount

class ContractRequirementsUnitTest : WordSpec({
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
                checkAll(Arb.list(anyContractEnforcement)) { enforcementList ->
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
                checkAll(Arb.list(anyContractEnforcement)) { enforcementList ->
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
            "properly handle calling requireThatEach on a collection with violations in multiple iterations" {
                checkAll(
                    Arb.int(2..30).flatMap { itemCount ->
                        Arb.pair(
                            Arb.set(Arb.int(), size = itemCount, slippage = 50),
                            Arb.int(1..itemCount)
                        )
                    }.map { (items, invalidItemCount) ->
                        items.toList().let { itemsAsList ->
                            Pair(
                                itemsAsList,
                                itemsAsList.take(invalidItemCount)
                            )
                        }
                    },
                ) { (items, invalidItems) ->
                    shouldThrow<ContractViolationException> {
                        validateRequirements(ContractRequirementType.VALID_INPUT) {
                            items.requireThatEach { item ->
                                requireThat(
                                    (item !in invalidItems) orError "Item is invalid"
                                )
                            }
                        }
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        if (invalidItems.size > 5) {
                            Regex("${invalidItems.size} instances").pattern
                        } else {
                            Regex(
                                "Iteration${if (invalidItems.size == 1) "" else "s"} (\\d+, )*\\d+(, \\.\\.\\.\\(\\d+ more omitted\\))?"
                            ).pattern
                        }.let { violationCountSnippet ->
                            exception.message!! shouldContain Regex("Item is invalid \\[$violationCountSnippet]")
                        }
                    }
                }
            }
            "return state violations only for failed conditions" {
                checkAll(
                    Arb.list(anyContractEnforcement),
                    Arb.list(anyContractEnforcement),
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
                    Arb.list(anyContractEnforcement),
                    Arb.list(anyContractEnforcement),
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
            "return the result of its body when no violations are raised" {
                checkAll(
                    Arb.int(min = 0, max = 6).flatMap { documentCount ->
                        anyValidServicingData(loanStateAndDocumentCount = documentCount)
                    },
                    anyValidDocumentMetadata,
                ) { randomServicingData, randomDocumentMetadata ->
                    validateRequirements(ContractRequirementType.VALID_INPUT) {
                        randomServicingData.toBuilder().also { servicingDataBuilder ->
                            servicingDataBuilder.addDocMeta(randomDocumentMetadata)
                        }.build()
                    }.let { modifiedServicingData ->
                        modifiedServicingData.docMetaCount shouldBeExactly randomServicingData.docMetaCount + 1
                        modifiedServicingData.docMetaList shouldContainExactlyInAnyOrder randomServicingData.docMetaList + randomDocumentMetadata
                    }
                }
            }
        }
    }
}) {
    override fun testCaseOrder() = TestCaseOrder.Random
}
