package io.provenance.scope.loan.contracts

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainIgnoringCase
import io.kotest.property.Arb
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.pair
import io.kotest.property.checkAll
import io.provenance.scope.loan.test.Constructors.appendLoanStatesContractWithNoExistingStates
import io.provenance.scope.loan.test.KotestConfig
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyPastNonEpochTimestamp
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyUuid
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidChecksum
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidLoanState
import io.provenance.scope.loan.test.MetadataAssetModelArbs.loanStateSet
import io.provenance.scope.loan.test.shouldHaveViolationCount
import io.provenance.scope.loan.test.toPair
import io.provenance.scope.loan.utility.ContractViolationException
import io.provenance.scope.util.toOffsetDateTime
import tech.figure.servicing.v1beta1.LoanStateOuterClass.LoanStateMetadata
import tech.figure.servicing.v1beta1.LoanStateOuterClass.LoanStateSubmission
import tech.figure.servicing.v1beta1.LoanStateOuterClass.ServicingData
import kotlin.math.max

class AppendLoanStatesContractUnitTest : WordSpec({
    "appendLoanStates" When {
        "given an empty input" should {
            "throw an appropriate exception" {
                shouldThrow<ContractViolationException> {
                    appendLoanStatesContractWithNoExistingStates.appendLoanStates(LoanStateSubmission.getDefaultInstance())
                }.let { exception ->
                    exception.message shouldContain "Servicing data is not set"
                }
            }
        }
        "given at least one input loan state with invalid fields" should {
            "throw an appropriate exception" {
                checkAll(anyValidLoanState) { randomValidLoanState ->
                    shouldThrow<ContractViolationException> {
                        appendLoanStatesContractWithNoExistingStates.appendLoanStates(
                            LoanStateSubmission.newBuilder().also { listBuilder ->
                                listBuilder.addAllLoanState(
                                    listOf(
                                        LoanStateMetadata.getDefaultInstance(),
                                        randomValidLoanState,
                                    )
                                )
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 4
                        exception.message shouldContainIgnoringCase "Loan state must have valid ID"
                        exception.message shouldContainIgnoringCase "Loan state is missing URI"
                        exception.message shouldContainIgnoringCase "Loan state's checksum is not set"
                        exception.message shouldContainIgnoringCase "Loan state must have valid effective time"
                    }
                }
            }
        }
        "given input loan states which duplicate existing loan state checksums" should {
            "throw an appropriate exception" {
                checkAll(
                    loanStateSet(size = 2).toPair(),
                    anyValidChecksum,
                ) { (randomExistingLoanState, randomNewLoanState), randomChecksum ->
                    shouldThrow<ContractViolationException> {
                        AppendLoanStatesContract(
                            existingServicingData = ServicingData.newBuilder().also { servicingDataBuilder ->
                                servicingDataBuilder.clearLoanState()
                                servicingDataBuilder.addLoanState(
                                    randomExistingLoanState.toBuilder().also { loanStateBuilder ->
                                        loanStateBuilder.checksum = randomChecksum
                                    }.build()
                                )
                            }.build()
                        ).appendLoanStates(
                            LoanStateSubmission.newBuilder().also { listBuilder ->
                                listBuilder.addAllLoanState(
                                    listOf(
                                        randomNewLoanState.toBuilder().also { loanStateBuilder ->
                                            loanStateBuilder.checksum = randomChecksum
                                        }.build()
                                    )
                                )
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Loan state with checksum ${randomChecksum.checksum} already exists"
                    }
                }
            }
        }
        "given input loan states which duplicate existing loan state IDs" should {
            "throw an appropriate exception" {
                checkAll(
                    loanStateSet(size = 2).toPair(),
                    anyUuid,
                ) { (randomExistingLoanState, randomNewLoanState), randomUuid ->
                    shouldThrow<ContractViolationException> {
                        AppendLoanStatesContract(
                            existingServicingData = ServicingData.newBuilder().also { servicingDataBuilder ->
                                servicingDataBuilder.clearLoanState()
                                servicingDataBuilder.addLoanState(
                                    randomExistingLoanState.toBuilder().also { loanStateBuilder ->
                                        loanStateBuilder.id = randomUuid
                                    }.build()
                                )
                            }.build()
                        ).appendLoanStates(
                            LoanStateSubmission.newBuilder().also { listBuilder ->
                                listBuilder.addAllLoanState(
                                    listOf(
                                        randomNewLoanState.toBuilder().also { loanStateBuilder ->
                                            loanStateBuilder.id = randomUuid
                                        }.build()
                                    )
                                )
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Loan state with ID ${randomUuid.value} already exists"
                    }
                }
            }
        }
        "given input loan states which duplicate existing loan state times" should {
            "throw an appropriate exception" {
                checkAll(
                    loanStateSet(size = 2).toPair(),
                    anyPastNonEpochTimestamp,
                ) { (randomExistingLoanState, randomNewLoanState), randomTimestamp ->
                    shouldThrow<ContractViolationException> {
                        AppendLoanStatesContract(
                            existingServicingData = ServicingData.newBuilder().also { servicingDataBuilder ->
                                servicingDataBuilder.clearLoanState()
                                servicingDataBuilder.addLoanState(
                                    randomExistingLoanState.toBuilder().also { loanStateBuilder ->
                                        loanStateBuilder.effectiveTime = randomTimestamp
                                    }.build()
                                )
                            }.build()
                        ).appendLoanStates(
                            LoanStateSubmission.newBuilder().also { listBuilder ->
                                listBuilder.addAllLoanState(
                                    listOf(
                                        randomNewLoanState.toBuilder().also { loanStateBuilder ->
                                            loanStateBuilder.effectiveTime = randomTimestamp
                                        }.build()
                                    )
                                )
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Loan state with effective time ${randomTimestamp.toOffsetDateTime()} already exists"
                    }
                }
            }
        }
        "given an input of loan states with duplicate checksums" should {
            "throw an appropriate exception" {
                checkAll(
                    loanStateSet(size = 2).toPair(),
                    anyValidChecksum,
                ) { (randomFirstNewLoanState, randomSecondNewLoanState), randomChecksum ->
                    shouldThrow<ContractViolationException> {
                        AppendLoanStatesContract(
                            existingServicingData = ServicingData.getDefaultInstance(),
                        ).appendLoanStates(
                            LoanStateSubmission.newBuilder().also { listBuilder ->
                                listBuilder.addAllLoanState(
                                    listOf(
                                        randomFirstNewLoanState.toBuilder().also { loanStateBuilder ->
                                            loanStateBuilder.checksum = randomChecksum
                                        }.build(),
                                        randomSecondNewLoanState.toBuilder().also { loanStateBuilder ->
                                            loanStateBuilder.checksum = randomChecksum
                                        }.build(),
                                    )
                                )
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Loan state with checksum ${randomChecksum.checksum} is provided more than once in input"
                    }
                }
            }
        }
        "given an input of loan states with duplicate IDs" should {
            "throw an appropriate exception" {
                checkAll(
                    loanStateSet(size = 2).toPair(),
                    anyUuid,
                ) { (randomFirstNewLoanState, randomSecondNewLoanState), randomId ->
                    shouldThrow<ContractViolationException> {
                        AppendLoanStatesContract(
                            existingServicingData = ServicingData.getDefaultInstance(),
                        ).appendLoanStates(
                            LoanStateSubmission.newBuilder().also { listBuilder ->
                                listBuilder.addAllLoanState(
                                    listOf(
                                        randomFirstNewLoanState.toBuilder().also { loanStateBuilder ->
                                            loanStateBuilder.id = randomId
                                        }.build(),
                                        randomSecondNewLoanState.toBuilder().also { loanStateBuilder ->
                                            loanStateBuilder.id = randomId
                                        }.build(),
                                    )
                                )
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContainIgnoringCase "Loan state with ID ${randomId.value} is provided more than once in input"
                    }
                }
            }
        }
        "given an input of loan states with duplicate times" should {
            "throw an appropriate exception" {
                checkAll(
                    loanStateSet(size = 2).toPair(),
                    anyPastNonEpochTimestamp,
                ) { (randomFirstNewLoanState, randomSecondNewLoanState), randomEffectiveTime ->
                    shouldThrow<ContractViolationException> {
                        AppendLoanStatesContract(
                            existingServicingData = ServicingData.getDefaultInstance(),
                        ).appendLoanStates(
                            LoanStateSubmission.newBuilder().also { listBuilder ->
                                listBuilder.addAllLoanState(
                                    listOf(
                                        randomFirstNewLoanState.toBuilder().also { loanStateBuilder ->
                                            loanStateBuilder.effectiveTime = randomEffectiveTime
                                        }.build(),
                                        randomSecondNewLoanState.toBuilder().also { loanStateBuilder ->
                                            loanStateBuilder.effectiveTime = randomEffectiveTime
                                        }.build(),
                                    )
                                )
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain
                            "Loan state with effective time ${randomEffectiveTime.toOffsetDateTime()} is provided more than once in input"
                    }
                }
            }
        }
        "given only new & valid input loan states" should {
            "not throw an exception" {
                val stateCountRange = 2..(if (KotestConfig.runTestsExtended) 8 else 3)
                checkAll(
                    Arb.int(stateCountRange).flatMap { randomStateCount ->
                        Arb.pair(
                            loanStateSet(size = randomStateCount),
                            Arb.int(0..max(randomStateCount - 1, 1)),
                        )
                    }
                ) { (randomStateSet, randomSplit) ->
                    val (randomExistingStates, randomNewStates) = randomStateSet.let { orderedRandomStateSet ->
                        orderedRandomStateSet.take(randomSplit) to orderedRandomStateSet.drop(randomSplit)
                    }
                    AppendLoanStatesContract(
                        existingServicingData = ServicingData.newBuilder().also { servicingDataBuilder ->
                            servicingDataBuilder.clearLoanState()
                            servicingDataBuilder.addAllLoanState(randomExistingStates)
                        }.build()
                    ).appendLoanStates(
                        LoanStateSubmission.newBuilder().also { listBuilder ->
                            listBuilder.addAllLoanState(randomNewStates)
                        }.build()
                    ).let { outputRecord ->
                        outputRecord.loanStateCount shouldBeExactly randomStateSet.size
                    }
                }
            }
        }
    }
}) {
    override fun testCaseOrder() = TestCaseOrder.Random
}
