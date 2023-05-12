package io.provenance.scope.loan.contracts

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.WordSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filterNot
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.pair
import io.kotest.property.arbitrary.set
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.provenance.scope.loan.LoanScopeProperties.assetLoanKey
import io.provenance.scope.loan.test.KotestConfig
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyFutureTimestamp
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyInvalidUuid
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyPastNonEpochTimestampPair
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyUuid
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidAch
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidAsset
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidFinancialAccount
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidFunding
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidProvenanceAccount
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidWire
import io.provenance.scope.loan.test.PrimitiveArbs.anyBlankString
import io.provenance.scope.loan.test.PrimitiveArbs.anyNonEmptyString
import io.provenance.scope.loan.test.PrimitiveArbs.anyNonUuidString
import io.provenance.scope.loan.test.shouldContainAtLeastOneOf
import io.provenance.scope.loan.test.shouldHaveViolationCount
import io.provenance.scope.loan.utility.ContractViolationException
import io.provenance.scope.loan.utility.toFigureTechLoan
import tech.figure.loan.v1beta1.Funding
import tech.figure.util.v1beta1.UUID
import tech.figure.util.v1beta1.ACH.AccountType as ACHAccountType

class UpdateFundingContractUnitTest : WordSpec({
    /* Helpers */
    fun <T> fundingWithDataSet(disbursementCount: IntRange, dataArb: (Int) -> Arb<List<T>>): Arb<Pair<Funding, Pair<Set<Int>, List<T>>>> =
        anyValidFunding(disbursementCount = disbursementCount).flatMap { funding ->
            Arb.pair(
                Arb.of(funding),
                funding.disbursementsCount.let { disbursementCount ->
                    Arb.int(min = 2, max = disbursementCount).flatMap { dataCount ->
                        Arb.pair(
                            Arb.set(gen = Arb.int(min = 0, max = disbursementCount - 1), size = dataCount, slippage = 15),
                            dataArb(dataCount)
                        )
                    }
                },
            )
        }
    /* Tests */
    "updateFunding" When {
        "given an empty input" should {
            "throw an appropriate exception" {
                checkAll(anyValidAsset) { randomExistingAsset ->
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            Funding.getDefaultInstance()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Funding is not set"
                    }
                }
            }
        }
        "given an input without a funding status" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    anyValidFunding(disbursementCount = 1..if (KotestConfig.runTestsExtended) 6 else 3),
                ) { randomExistingAsset, randomNewFunding ->
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding.toBuilder().clearStatus().build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Funding status must be set"
                    }
                }
            }
        }
        "given an input with an invalid funding status value" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    anyValidFunding(disbursementCount = 1..if (KotestConfig.runTestsExtended) 6 else 3),
                    Arb.string().filterNot { it in listOf("UNFUNDED", "INITIATED", "FUNDED", "CANCELLED") },
                ) { randomExistingAsset, randomNewFunding, randomInvalidFundingStatus ->
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding.toBuilder().also { fundingBuilder ->
                                fundingBuilder.status = fundingBuilder.statusBuilder.also { statusBuilder ->
                                    statusBuilder.status = randomInvalidFundingStatus
                                }.build()
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Funding status must be valid"
                    }
                }
            }
        }
        "given an input without a funding status effective time" should {
            "not throw an exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    anyValidFunding(disbursementCount = 1..if (KotestConfig.runTestsExtended) 6 else 3),
                ) { randomExistingAsset, randomNewFunding ->
                    randomNewFunding.toBuilder().also { fundingBuilder ->
                        fundingBuilder.status = fundingBuilder.statusBuilder.also { statusBuilder ->
                            statusBuilder.clearEffectiveTime()
                        }.build()
                    }.build().let { modifiedFundingData ->
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(modifiedFundingData).let { newAsset ->
                            newAsset.kvMap[assetLoanKey]?.toFigureTechLoan()?.funding shouldBe modifiedFundingData
                        }
                    }
                }
            }
        }
        "given an input with a funding status effective time in the future" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    anyValidFunding(disbursementCount = 1..if (KotestConfig.runTestsExtended) 6 else 3),
                    anyFutureTimestamp,
                ) { randomExistingAsset, randomNewFunding, randomFutureTimestamp ->
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding.toBuilder().also { fundingBuilder ->
                                fundingBuilder.status = fundingBuilder.statusBuilder.also { statusBuilder ->
                                    statusBuilder.effectiveTime = randomFutureTimestamp
                                }.build()
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Funding status must have valid effective time"
                    }
                }
            }
        }
        "given an input with a funding start time in the future" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    anyValidFunding(disbursementCount = 1..if (KotestConfig.runTestsExtended) 6 else 3),
                    anyFutureTimestamp,
                ) { randomExistingAsset, randomNewFunding, randomFutureTimestamp ->
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding.toBuilder().also { fundingBuilder ->
                                fundingBuilder.status = fundingBuilder.statusBuilder.also { statusBuilder ->
                                    statusBuilder.status = "INITIATED"
                                }.build()
                                fundingBuilder.started = randomFutureTimestamp
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Funding start time must be valid"
                    }
                }
            }
        }
        "given an input with a funding end time in the future" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    anyValidFunding(disbursementCount = 1..if (KotestConfig.runTestsExtended) 6 else 3),
                    anyFutureTimestamp,
                ) { randomExistingAsset, randomNewFunding, randomFutureTimestamp ->
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding.toBuilder().also { fundingBuilder ->
                                fundingBuilder.status = fundingBuilder.statusBuilder.also { statusBuilder ->
                                    statusBuilder.status = "FUNDED"
                                }.build()
                                fundingBuilder.completed = randomFutureTimestamp
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Completed funding's end time must be valid"
                    }
                }
            }
        }
        "given an input with a funding start time which is after the funding end time" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    anyValidFunding(disbursementCount = 1..if (KotestConfig.runTestsExtended) 6 else 3),
                    anyPastNonEpochTimestampPair,
                ) { randomExistingAsset, randomNewFunding, randomTimestampPair ->
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding.toBuilder().also { fundingBuilder ->
                                fundingBuilder.status = fundingBuilder.statusBuilder.also { statusBuilder ->
                                    statusBuilder.status = "FUNDED"
                                }.build()
                                fundingBuilder.started = randomTimestampPair.laterTimestamp
                                fundingBuilder.completed = randomTimestampPair.earlierTimestamp
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Funding end time must be after start time"
                    }
                }
            }
        }
        "given an input without disbursements" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    anyValidFunding(disbursementCount = 0..0),
                ) { randomExistingAsset, randomNewFunding ->
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Must supply at least one disbursement entry"
                    }
                }
            }
        }
        "given an input with duplicate disbursement IDs" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    anyValidFunding(disbursementCount = 2..if (KotestConfig.runTestsExtended) 6 else 3).flatMap { funding ->
                        Arb.pair(
                            Arb.of(funding),
                            funding.disbursementsCount.let { disbursementCount ->
                                Arb.int(min = 2, max = disbursementCount).flatMap { duplicateIdCount ->
                                    Arb.set(gen = Arb.int(min = 0, max = disbursementCount - 1), size = duplicateIdCount, slippage = 15)
                                }
                            },
                        )
                    },
                    anyUuid,
                ) { randomExistingAsset, (randomNewFunding, duplicateIdIndices), randomId ->
                    randomNewFunding.toBuilder().also { fundingBuilder ->
                        duplicateIdIndices.forEach { index ->
                            fundingBuilder.setDisbursements(
                                index,
                                fundingBuilder.getDisbursementsBuilder(index).also { disbursementBuilder ->
                                    disbursementBuilder.id = randomId
                                }.build()
                            )
                        }
                    }.build().let { newFunding ->
                        shouldThrow<ContractViolationException> {
                            UpdateFundingContract(
                                existingAsset = randomExistingAsset,
                            ).updateFunding(
                                newFunding
                            )
                        }.let { exception ->
                            exception shouldHaveViolationCount 1
                            newFunding.disbursementsList.count { disbursement ->
                                disbursement.id.value == randomId.value
                            } shouldBeGreaterThanOrEqual duplicateIdIndices.size
                            exception.message shouldContain Regex("Disbursement ID ${randomId.value} is not unique \\(\\d+ usages\\)")
                        }
                    }
                }
            }
        }
        "given an input with one or more invalid disbursement IDs" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    fundingWithDataSet(disbursementCount = 2..if (KotestConfig.runTestsExtended) 5 else 3) { invalidIdCount ->
                        Arb.set(anyNonUuidString, size = invalidIdCount).map { it.toList() }
                    },
                ) { randomExistingAsset, (randomNewFunding, invalidIdData) ->
                    val invalidIdIndices = invalidIdData.first
                    val invalidIds = invalidIdData.second.toMutableList()
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding.toBuilder().also { fundingBuilder ->
                                invalidIdIndices.forEach { index ->
                                    fundingBuilder.setDisbursements(
                                        index,
                                        fundingBuilder.getDisbursementsBuilder(index).also { disbursementBuilder ->
                                            disbursementBuilder.id = UUID.newBuilder().also { idBuilder ->
                                                idBuilder.value = invalidIds.removeAt(0)
                                            }.build()
                                        }.build()
                                    )
                                }
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain
                            "Disbursement must have valid ID [Iteration${if (invalidIdIndices.size == 1) "" else "s"} " +
                            "${invalidIdIndices.sorted().joinToString()}]"
                    }
                }
            }
        }
        "given an input with one or more invalid disbursement start times" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    fundingWithDataSet(disbursementCount = 2..if (KotestConfig.runTestsExtended) 5 else 3) { invalidStartTimeCount ->
                        Arb.list(anyFutureTimestamp, range = invalidStartTimeCount..invalidStartTimeCount)
                    },
                    anyNonEmptyString.filterNot { it in listOf("UNFUNDED", "UNKNOWN") },
                ) { randomExistingAsset, (randomNewFunding, invalidStartTimeData), disbursementStatus ->
                    val invalidStartTimeIndices = invalidStartTimeData.first
                    val invalidStartTimes = invalidStartTimeData.second.toMutableList()
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding.toBuilder().also { fundingBuilder ->
                                invalidStartTimeIndices.forEach { index ->
                                    fundingBuilder.setDisbursements(
                                        index,
                                        fundingBuilder.getDisbursementsBuilder(index).also { disbursementBuilder ->
                                            disbursementBuilder.status = disbursementBuilder.statusBuilder.also { statusBuilder ->
                                                statusBuilder.status = disbursementStatus
                                            }.build()
                                            disbursementBuilder.started = invalidStartTimes.removeAt(0)
                                        }.build()
                                    )
                                }
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain
                            "Disbursement start time must be valid [Iteration${if (invalidStartTimeIndices.size == 1) "" else "s"} " +
                            "${invalidStartTimeIndices.sorted().joinToString()}]"
                    }
                }
            }
        }
        "given an input with one or more invalid disbursement end times" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    fundingWithDataSet(disbursementCount = 2..if (KotestConfig.runTestsExtended) 5 else 3) { invalidEndTimeCount ->
                        Arb.list(anyFutureTimestamp, range = invalidEndTimeCount..invalidEndTimeCount)
                    },
                ) { randomExistingAsset, (randomNewFunding, invalidEndTimeData) ->
                    val invalidEndTimeIndices = invalidEndTimeData.first
                    val invalidEndTimes = invalidEndTimeData.second.toMutableList()
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding.toBuilder().also { fundingBuilder ->
                                invalidEndTimeIndices.forEach { index ->
                                    fundingBuilder.setDisbursements(
                                        index,
                                        fundingBuilder.getDisbursementsBuilder(index).also { disbursementBuilder ->
                                            disbursementBuilder.status = disbursementBuilder.statusBuilder.also { statusBuilder ->
                                                statusBuilder.status = "FUNDED"
                                            }.build()
                                            disbursementBuilder.completed = invalidEndTimes.removeAt(0)
                                        }.build()
                                    )
                                }
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain
                            "Completed disbursement's end time must be valid [Iteration${if (invalidEndTimeIndices.size == 1) "" else "s"} " +
                            "${invalidEndTimeIndices.sorted().joinToString()}]"
                    }
                }
            }
        }
        "given an input with one or more invalid end times for cancelled disbursements" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    fundingWithDataSet(disbursementCount = 2..if (KotestConfig.runTestsExtended) 5 else 3) { invalidEndTimeCount ->
                        Arb.list(anyFutureTimestamp, range = invalidEndTimeCount..invalidEndTimeCount)
                    },
                ) { randomExistingAsset, (randomNewFunding, invalidEndTimeData) ->
                    val invalidEndTimeIndices = invalidEndTimeData.first
                    val invalidEndTimes = invalidEndTimeData.second.toMutableList()
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding.toBuilder().also { fundingBuilder ->
                                invalidEndTimeIndices.forEach { index ->
                                    fundingBuilder.setDisbursements(
                                        index,
                                        fundingBuilder.getDisbursementsBuilder(index).also { disbursementBuilder ->
                                            disbursementBuilder.status = disbursementBuilder.statusBuilder.also { statusBuilder ->
                                                statusBuilder.status = "CANCELLED"
                                            }.build()
                                            disbursementBuilder.completed = invalidEndTimes.removeAt(0)
                                        }.build()
                                    )
                                }
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain
                            "Cancelled disbursement's end time must be valid [Iteration${if (invalidEndTimeIndices.size == 1) "" else "s"} " +
                            "${invalidEndTimeIndices.sorted().joinToString()}]"
                    }
                }
            }
        }
        "given an input with one or more disbursements with start times which are after their end times" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    fundingWithDataSet(disbursementCount = 2..if (KotestConfig.runTestsExtended) 5 else 3) { invalidEndTimeCount ->
                        Arb.list(anyPastNonEpochTimestampPair, range = invalidEndTimeCount..invalidEndTimeCount)
                    },
                ) { randomExistingAsset, (randomNewFunding, invalidEndTimeData) ->
                    val invalidTimeIndices = invalidEndTimeData.first
                    val invalidTimePairs = invalidEndTimeData.second.toMutableList()
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding.toBuilder().also { fundingBuilder ->
                                invalidTimeIndices.forEach { index ->
                                    fundingBuilder.setDisbursements(
                                        index,
                                        fundingBuilder.getDisbursementsBuilder(index).also { disbursementBuilder ->
                                            disbursementBuilder.status = disbursementBuilder.statusBuilder.also { statusBuilder ->
                                                statusBuilder.status = "FUNDED"
                                            }.build()
                                            invalidTimePairs.removeAt(0).let { timestamps ->
                                                disbursementBuilder.started = timestamps.laterTimestamp
                                                disbursementBuilder.completed = timestamps.earlierTimestamp
                                            }
                                        }.build()
                                    )
                                }
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain
                            "Disbursement end time must be after start time [Iteration${if (invalidTimeIndices.size == 1) "" else "s"} " +
                            "${invalidTimeIndices.sorted().joinToString()}]"
                    }
                }
            }
        }
        "given an input with one or more invalid disbursement amounts" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    fundingWithDataSet(disbursementCount = 2..if (KotestConfig.runTestsExtended) 5 else 3) { invalidAmountCount ->
                        Arb.list(
                            Arb.string().filterNot { it.matches(Regex("^([0-9]+(?:[.][0-9]+)?|\\.[0-9]+)$")) },
                            range = invalidAmountCount..invalidAmountCount,
                        )
                    },
                ) { randomExistingAsset, (randomNewFunding, invalidAmountData) ->
                    val invalidAmountIndices = invalidAmountData.first
                    val invalidAmounts = invalidAmountData.second.toMutableList()
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding.toBuilder().also { fundingBuilder ->
                                invalidAmountIndices.forEach { index ->
                                    fundingBuilder.setDisbursements(
                                        index,
                                        fundingBuilder.getDisbursementsBuilder(index).also { disbursementBuilder ->
                                            disbursementBuilder.amount = disbursementBuilder.amountBuilder.also { amountBuilder ->
                                                amountBuilder.value = invalidAmounts.removeAt(0)
                                            }.build()
                                        }.build()
                                    )
                                }
                            }.build()
                        )
                    }.let { exception ->
                        exception.message!! shouldContainAtLeastOneOf listOf(
                            "Disbursement amount must not be negative [Iteration${if (invalidAmountIndices.size == 1) "" else "s"} " +
                                "${invalidAmountIndices.sorted().joinToString()}]",
                            "Disbursement amount must have a valid value [Iteration${if (invalidAmountIndices.size == 1) "" else "s"} " +
                                "${invalidAmountIndices.sorted().joinToString()}]",
                        )
                    }
                }
            }
        }
        "given an input with disbursements which have no account" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    anyValidFunding(disbursementCount = 1..if (KotestConfig.runTestsExtended) 5 else 3).flatMap { funding ->
                        Arb.pair(
                            Arb.of(funding),
                            funding.disbursementsCount.let { disbursementCount ->
                                Arb.int(min = 1, max = disbursementCount).flatMap { missingAccountCount ->
                                    Arb.set(gen = Arb.int(min = 0, max = disbursementCount - 1), size = missingAccountCount, slippage = 15)
                                }
                            },
                        )
                    },
                ) { randomExistingAsset, (randomNewFunding, missingAccountIndices) ->
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding.toBuilder().also { fundingBuilder ->
                                missingAccountIndices.forEach { index ->
                                    fundingBuilder.setDisbursements(
                                        index,
                                        fundingBuilder.getDisbursementsBuilder(index).also { disbursementBuilder ->
                                            disbursementBuilder.clearDisburseAccount()
                                        }.build()
                                    )
                                }
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message!! shouldContain "Disbursement account is not set " +
                            "[Iteration${if (missingAccountIndices.size == 1) "" else "s"} ${missingAccountIndices.sorted().joinToString()}]"
                    }
                }
            }
        }
        "given an input with a disbursement without an account ID" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    anyValidFunding(disbursementCount = 1..if (KotestConfig.runTestsExtended) 7 else 3),
                    anyInvalidUuid,
                ) { randomExistingAsset, randomNewFunding, randomInvalidId ->
                    val invalidAccountIdIndex = randomNewFunding.disbursementsCount - 1
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding.toBuilder().also { fundingBuilder ->
                                fundingBuilder.setDisbursements(
                                    invalidAccountIdIndex,
                                    fundingBuilder.getDisbursementsBuilder(invalidAccountIdIndex).also { disbursementBuilder ->
                                        disbursementBuilder.disburseAccount = disbursementBuilder.disburseAccountBuilder.also { accountBuilder ->
                                            accountBuilder.accountOwnerId = randomInvalidId
                                        }.build()
                                    }.build()
                                )
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message!! shouldContain "Disbursement account must have valid owner ID [Iteration $invalidAccountIdIndex]"
                    }
                }
            }
        }
        "given an input with a disbursement without a bank account ID" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    anyValidFunding(disbursementCount = 1..if (KotestConfig.runTestsExtended) 7 else 3),
                    anyValidFinancialAccount,
                ) { randomExistingAsset, randomNewFunding, randomBankAccount ->
                    val invalidAccountIdIndex = randomNewFunding.disbursementsCount - 1
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding.toBuilder().also { fundingBuilder ->
                                fundingBuilder.setDisbursements(
                                    invalidAccountIdIndex,
                                    fundingBuilder.getDisbursementsBuilder(invalidAccountIdIndex).also { disbursementBuilder ->
                                        disbursementBuilder.disburseAccount = disbursementBuilder.disburseAccountBuilder.also { accountBuilder ->
                                            accountBuilder.clearProvenance()
                                            accountBuilder.financial = randomBankAccount.toBuilder().also { bankAccountBuilder ->
                                                bankAccountBuilder.clearId()
                                            }.build()
                                        }.build()
                                    }.build()
                                )
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message!! shouldContain "Disbursement bank account ID must be set [Iteration $invalidAccountIdIndex]"
                    }
                }
            }
        }
        "given an input with a disbursement without a valid bank account number" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    anyValidFunding(disbursementCount = 1..if (KotestConfig.runTestsExtended) 7 else 3),
                    anyValidFinancialAccount,
                    Arb.string().filterNot { it.length in 4..17 },
                ) { randomExistingAsset, randomNewFunding, randomBankAccount, randomInvalidAccountNumber ->
                    val invalidAccountNumberIndex = randomNewFunding.disbursementsCount - 1
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding.toBuilder().also { fundingBuilder ->
                                fundingBuilder.setDisbursements(
                                    invalidAccountNumberIndex,
                                    fundingBuilder.getDisbursementsBuilder(invalidAccountNumberIndex).also { disbursementBuilder ->
                                        disbursementBuilder.disburseAccount = disbursementBuilder.disburseAccountBuilder.also { accountBuilder ->
                                            accountBuilder.clearProvenance()
                                            accountBuilder.financial = randomBankAccount.toBuilder().also { bankAccountBuilder ->
                                                bankAccountBuilder.accountNumber = randomInvalidAccountNumber
                                            }.build()
                                        }.build()
                                    }.build()
                                )
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message!! shouldContain "Disbursement bank account must have a valid account number " +
                            "[Iteration $invalidAccountNumberIndex]"
                    }
                }
            }
        }
        "given an input with a disbursement without a valid bank routing number" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    anyValidFunding(disbursementCount = 1..if (KotestConfig.runTestsExtended) 7 else 3),
                    anyValidFinancialAccount,
                    Arb.string().filterNot { it.length == 9 },
                ) { randomExistingAsset, randomNewFunding, randomBankAccount, randomInvalidRoutingNumber ->
                    val invalidAccountIndex = randomNewFunding.disbursementsCount - 1
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding.toBuilder().also { fundingBuilder ->
                                fundingBuilder.setDisbursements(
                                    invalidAccountIndex,
                                    fundingBuilder.getDisbursementsBuilder(invalidAccountIndex).also { disbursementBuilder ->
                                        disbursementBuilder.disburseAccount = disbursementBuilder.disburseAccountBuilder.also { accountBuilder ->
                                            accountBuilder.clearProvenance()
                                            accountBuilder.financial = randomBankAccount.toBuilder().also { bankAccountBuilder ->
                                                bankAccountBuilder.routingNumber = randomInvalidRoutingNumber
                                            }.build()
                                        }.build()
                                    }.build()
                                )
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message!! shouldContain "Disbursement bank account must have a valid routing number " +
                            "[Iteration $invalidAccountIndex]"
                    }
                }
            }
        }
        "given an input with a disbursement without a valid bank routing number" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    anyValidFunding(disbursementCount = 1..if (KotestConfig.runTestsExtended) 7 else 3),
                    anyValidFinancialAccount,
                    Arb.string().filterNot { it.length == 9 },
                ) { randomExistingAsset, randomNewFunding, randomBankAccount, randomInvalidRoutingNumber ->
                    val invalidAccountIndex = randomNewFunding.disbursementsCount - 1
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding.toBuilder().also { fundingBuilder ->
                                fundingBuilder.setDisbursements(
                                    invalidAccountIndex,
                                    fundingBuilder.getDisbursementsBuilder(invalidAccountIndex).also { disbursementBuilder ->
                                        disbursementBuilder.disburseAccount = disbursementBuilder.disburseAccountBuilder.also { accountBuilder ->
                                            accountBuilder.clearProvenance()
                                            accountBuilder.financial = randomBankAccount.toBuilder().also { bankAccountBuilder ->
                                                bankAccountBuilder.routingNumber = randomInvalidRoutingNumber
                                            }.build()
                                        }.build()
                                    }.build()
                                )
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message!! shouldContain "Disbursement bank account must have a valid routing number " +
                            "[Iteration $invalidAccountIndex]"
                    }
                }
            }
        }
        "given an input with a disbursement which has a money movement with an invalid ACH account type" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    anyValidFunding(disbursementCount = 1..if (KotestConfig.runTestsExtended) 7 else 3),
                    anyValidFinancialAccount,
                    anyValidAch,
                ) { randomExistingAsset, randomNewFunding, randomBankAccount, randomAch ->
                    val invalidAccountIndex = randomNewFunding.disbursementsCount - 1
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding.toBuilder().also { fundingBuilder ->
                                fundingBuilder.setDisbursements(
                                    invalidAccountIndex,
                                    fundingBuilder.getDisbursementsBuilder(invalidAccountIndex).also { disbursementBuilder ->
                                        disbursementBuilder.disburseAccount = disbursementBuilder.disburseAccountBuilder.also { accountBuilder ->
                                            accountBuilder.clearProvenance()
                                            accountBuilder.financial = randomBankAccount.toBuilder().also { bankAccountBuilder ->
                                                val invalidMovementIndex = randomBankAccount.movementCount - 1
                                                bankAccountBuilder.setMovement(
                                                    invalidMovementIndex,
                                                    bankAccountBuilder.getMovementBuilder(invalidMovementIndex).also { movementBuilder ->
                                                        movementBuilder.clearWire()
                                                        movementBuilder.ach = randomAch.toBuilder().also { achBuilder ->
                                                            achBuilder.accountType = ACHAccountType.ACCOUNT_TYPE_UNKNOWN
                                                        }.build()
                                                    }.build()
                                                )
                                            }.build()
                                        }.build()
                                    }.build()
                                )
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message!! shouldContain Regex("Disbursement .* \\[1 instance] \\[Iteration $invalidAccountIndex]")
                    }
                }
            }
        }
        "given an input with a disbursement which has a money movement without a wire account address" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    anyValidFunding(disbursementCount = 1..if (KotestConfig.runTestsExtended) 7 else 3),
                    anyValidFinancialAccount,
                    anyValidWire,
                ) { randomExistingAsset, randomNewFunding, randomBankAccount, randomWire ->
                    val invalidAccountIndex = randomNewFunding.disbursementsCount - 1
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding.toBuilder().also { fundingBuilder ->
                                fundingBuilder.setDisbursements(
                                    invalidAccountIndex,
                                    fundingBuilder.getDisbursementsBuilder(invalidAccountIndex).also { disbursementBuilder ->
                                        disbursementBuilder.disburseAccount = disbursementBuilder.disburseAccountBuilder.also { accountBuilder ->
                                            accountBuilder.clearProvenance()
                                            accountBuilder.financial = randomBankAccount.toBuilder().also { bankAccountBuilder ->
                                                val invalidMovementIndex = randomBankAccount.movementCount - 1
                                                bankAccountBuilder.setMovement(
                                                    invalidMovementIndex,
                                                    bankAccountBuilder.getMovementBuilder(invalidMovementIndex).also { movementBuilder ->
                                                        movementBuilder.clearAch()
                                                        movementBuilder.wire = randomWire.toBuilder().also { achBuilder ->
                                                            achBuilder.clearAccountAddress()
                                                        }.build()
                                                    }.build()
                                                )
                                            }.build()
                                        }.build()
                                    }.build()
                                )
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message!! shouldContain "Disbursement bank account for wires must have a valid address " +
                            "[1 instance] [Iteration $invalidAccountIndex]"
                    }
                }
            }
        }
        "given an input with a disbursement which has a money movement without wire instructions" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    anyValidFunding(disbursementCount = 1..if (KotestConfig.runTestsExtended) 7 else 3),
                    anyValidFinancialAccount,
                    anyValidWire,
                    anyBlankString,
                ) { randomExistingAsset, randomNewFunding, randomBankAccount, randomWire, randomBlankString ->
                    val invalidAccountIndex = randomNewFunding.disbursementsCount - 1
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding.toBuilder().also { fundingBuilder ->
                                fundingBuilder.setDisbursements(
                                    invalidAccountIndex,
                                    fundingBuilder.getDisbursementsBuilder(invalidAccountIndex).also { disbursementBuilder ->
                                        disbursementBuilder.disburseAccount = disbursementBuilder.disburseAccountBuilder.also { accountBuilder ->
                                            accountBuilder.clearProvenance()
                                            accountBuilder.financial = randomBankAccount.toBuilder().also { bankAccountBuilder ->
                                                val invalidMovementIndex = randomBankAccount.movementCount - 1
                                                bankAccountBuilder.setMovement(
                                                    invalidMovementIndex,
                                                    bankAccountBuilder.getMovementBuilder(invalidMovementIndex).also { movementBuilder ->
                                                        movementBuilder.clearAch()
                                                        movementBuilder.wire = randomWire.toBuilder().also { achBuilder ->
                                                            achBuilder.wireInstructions = randomBlankString
                                                        }.build()
                                                    }.build()
                                                )
                                            }.build()
                                        }.build()
                                    }.build()
                                )
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message!! shouldContain "Disbursement bank account for wires must have valid wire instructions " +
                            "[1 instance] [Iteration $invalidAccountIndex]"
                    }
                }
            }
        }
        "given an input with a disbursement which has a money movement without a SWIFT bank account ID" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    anyValidFunding(disbursementCount = 1..if (KotestConfig.runTestsExtended) 7 else 3),
                    anyValidFinancialAccount,
                    anyValidWire,
                ) { randomExistingAsset, randomNewFunding, randomBankAccount, randomWire ->
                    val invalidAccountIndex = randomNewFunding.disbursementsCount - 1
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding.toBuilder().also { fundingBuilder ->
                                fundingBuilder.setDisbursements(
                                    invalidAccountIndex,
                                    fundingBuilder.getDisbursementsBuilder(invalidAccountIndex).also { disbursementBuilder ->
                                        disbursementBuilder.disburseAccount = disbursementBuilder.disburseAccountBuilder.also { accountBuilder ->
                                            accountBuilder.clearProvenance()
                                            accountBuilder.financial = randomBankAccount.toBuilder().also { bankAccountBuilder ->
                                                val invalidMovementIndex = randomBankAccount.movementCount - 1
                                                bankAccountBuilder.setMovement(
                                                    invalidMovementIndex,
                                                    bankAccountBuilder.getMovementBuilder(invalidMovementIndex).also { movementBuilder ->
                                                        movementBuilder.clearAch()
                                                        movementBuilder.wire = randomWire.toBuilder().also { achBuilder ->
                                                            achBuilder.swiftInstructions = achBuilder.swiftInstructionsBuilder.also { swiftBuilder ->
                                                                swiftBuilder.clearSwiftId()
                                                            }.build()
                                                        }.build()
                                                    }.build()
                                                )
                                            }.build()
                                        }.build()
                                    }.build()
                                )
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message!! shouldContain "Disbursement bank account for wires must have valid SWIFT bank account ID " +
                            "[1 instance] [Iteration $invalidAccountIndex]"
                    }
                }
            }
        }
        "given an input with a disbursement which has a money movement without a SWIFT bank mailing address" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    anyValidFunding(disbursementCount = 1..if (KotestConfig.runTestsExtended) 7 else 3),
                    anyValidFinancialAccount,
                    anyValidWire,
                ) { randomExistingAsset, randomNewFunding, randomBankAccount, randomWire ->
                    val invalidAccountIndex = randomNewFunding.disbursementsCount - 1
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding.toBuilder().also { fundingBuilder ->
                                fundingBuilder.setDisbursements(
                                    invalidAccountIndex,
                                    fundingBuilder.getDisbursementsBuilder(invalidAccountIndex).also { disbursementBuilder ->
                                        disbursementBuilder.disburseAccount = disbursementBuilder.disburseAccountBuilder.also { accountBuilder ->
                                            accountBuilder.clearProvenance()
                                            accountBuilder.financial = randomBankAccount.toBuilder().also { bankAccountBuilder ->
                                                val invalidMovementIndex = randomBankAccount.movementCount - 1
                                                bankAccountBuilder.setMovement(
                                                    invalidMovementIndex,
                                                    bankAccountBuilder.getMovementBuilder(invalidMovementIndex).also { movementBuilder ->
                                                        movementBuilder.clearAch()
                                                        movementBuilder.wire = randomWire.toBuilder().also { achBuilder ->
                                                            achBuilder.swiftInstructions = achBuilder.swiftInstructionsBuilder.also { swiftBuilder ->
                                                                swiftBuilder.clearSwiftBankAddress()
                                                            }.build()
                                                        }.build()
                                                    }.build()
                                                )
                                            }.build()
                                        }.build()
                                    }.build()
                                )
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message!! shouldContain "Disbursement bank account for wires must have valid SWIFT bank mailing address " +
                            "[1 instance] [Iteration $invalidAccountIndex]"
                    }
                }
            }
        }
        "given an input with a disbursement without a valid Provenance account address" should {
            "throw an appropriate exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    anyValidFunding(disbursementCount = 1..if (KotestConfig.runTestsExtended) 7 else 3),
                    anyValidProvenanceAccount,
                    Arb.string(maxSize = 40),
                ) { randomExistingAsset, randomNewFunding, randomBankAccount, randomInvalidProvenanceAddress ->
                    val invalidAccountIndex = randomNewFunding.disbursementsCount - 1
                    shouldThrow<ContractViolationException> {
                        UpdateFundingContract(
                            existingAsset = randomExistingAsset,
                        ).updateFunding(
                            randomNewFunding.toBuilder().also { fundingBuilder ->
                                fundingBuilder.setDisbursements(
                                    invalidAccountIndex,
                                    fundingBuilder.getDisbursementsBuilder(invalidAccountIndex).also { disbursementBuilder ->
                                        disbursementBuilder.disburseAccount = disbursementBuilder.disburseAccountBuilder.also { accountBuilder ->
                                            accountBuilder.clearFinancial()
                                            accountBuilder.provenance = randomBankAccount.toBuilder().also { bankAccountBuilder ->
                                                bankAccountBuilder.address = randomInvalidProvenanceAddress
                                            }.build()
                                        }.build()
                                    }.build()
                                )
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message!! shouldContain Regex("Disbursement account.* \\[Iteration $invalidAccountIndex]")
                    }
                }
            }
        }
        "given a valid input" should {
            "not throw an exception" {
                checkAll(
                    if (KotestConfig.runTestsExtended) anyValidAsset else anyValidAsset(hasMismoLoan = false, hasFunding = false),
                    Arb.int(range = 1..10).flatMap { disbursementCount -> anyValidFunding(disbursementCount = 1..disbursementCount) },
                ) { randomExistingAsset, randomNewFunding ->
                    UpdateFundingContract(
                        existingAsset = randomExistingAsset,
                    ).updateFunding(
                        newFunding = randomNewFunding,
                    ).let { newAsset ->
                        newAsset.kvMap[assetLoanKey]?.toFigureTechLoan()?.funding shouldBe randomNewFunding
                    }
                }
            }
        }
    }
}) {
    override fun testCaseOrder() = TestCaseOrder.Random
    @ExperimentalKotest
    override fun concurrency() = 1
}
