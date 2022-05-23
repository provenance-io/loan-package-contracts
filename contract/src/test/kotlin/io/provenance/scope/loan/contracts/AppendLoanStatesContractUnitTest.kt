package io.provenance.scope.loan.contracts

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainIgnoringCase
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.pair
import io.kotest.property.checkAll
import io.provenance.scope.loan.test.Constructors.appendLoanStatesContractWithNoExistingStates
import io.provenance.scope.loan.test.LoanPackageArbs.anyUuid
import io.provenance.scope.loan.test.LoanPackageArbs.anyValidChecksum
import io.provenance.scope.loan.test.LoanPackageArbs.anyValidLoanState
import io.provenance.scope.loan.test.LoanPackageArbs.anyValidTimestamp
import io.provenance.scope.loan.test.LoanPackageArbs.loanStateSet
import io.provenance.scope.loan.utility.ContractViolationException
import io.provenance.scope.util.toOffsetDateTime
import tech.figure.servicing.v1beta1.LoanStateOuterClass.LoanStateMetadata
import tech.figure.servicing.v1beta1.LoanStateOuterClass.ServicingData
import kotlin.math.max

class AppendLoanStatesContractUnitTest : WordSpec({
    "appendLoanStates" When {
        "given an empty input" should {
            "throw an appropriate exception" {
                shouldThrow<ContractViolationException> {
                    appendLoanStatesContractWithNoExistingStates.appendLoanStates(emptyList())
                }.let { exception ->
                    exception.message shouldContainIgnoringCase "Must supply at least one loan state"
                }
            }
        }
        "given at least one loan state with invalid fields" should {
            "throw an appropriate exception" {
                checkAll(anyValidLoanState) { randomLoanState ->
                    shouldThrow<ContractViolationException> {
                        appendLoanStatesContractWithNoExistingStates.appendLoanStates(
                            listOf(
                                LoanStateMetadata.getDefaultInstance(),
                                randomLoanState,
                            )
                        )
                    }.let { exception ->
                        exception.message shouldContainIgnoringCase "must have valid ID"
                        exception.message shouldContainIgnoringCase "missing URI"
                        exception.message shouldContainIgnoringCase "missing checksum"
                    }
                }
            }
        }
        "given loan states which duplicate existing loan state checksums" should {
            "throw an appropriate exception" {
                checkAll(anyValidLoanState, anyValidLoanState, anyValidChecksum) { randomExistingLoanState, randomNewLoanState, randomChecksum ->
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
                            listOf(
                                randomNewLoanState.toBuilder().also { loanStateBuilder ->
                                    loanStateBuilder.checksum = randomChecksum
                                }.build()
                            )
                        )
                    }.let { exception ->
                        exception.message shouldContain "Loan state with checksum ${randomChecksum.checksum} already exists"
                    }
                }
            }
        }
        "given loan states which duplicate existing loan state IDs" should {
            "throw an appropriate exception" {
                checkAll(anyValidLoanState, anyValidLoanState, anyUuid) { randomExistingLoanState, randomNewLoanState, randomUuid ->
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
                            listOf(
                                randomNewLoanState.toBuilder().also { loanStateBuilder ->
                                    loanStateBuilder.id = randomUuid
                                }.build()
                            )
                        )
                    }.let { exception ->
                        exception.message shouldContain "Loan state with ID ${randomUuid.value} already exists"
                    }
                }
            }
        }
        "given loan states which duplicate existing loan state times" should {
            "throw an appropriate exception" {
                checkAll(anyValidLoanState, anyValidLoanState, anyValidTimestamp) { randomExistingLoanState, randomNewLoanState, randomTimestamp ->
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
                            listOf(
                                randomNewLoanState.toBuilder().also { loanStateBuilder ->
                                    loanStateBuilder.effectiveTime = randomTimestamp
                                }.build()
                            )
                        )
                    }.let { exception ->
                        exception.message shouldContain "Loan state with effective time ${randomTimestamp.toOffsetDateTime()} already exists"
                    }
                }
            }
        }
        "given only new & valid loan states" should {
            "not throw an exception" {
                val stateCountRange = 2..4 // Reduce the upper bound of this range (to no lower than 3) to decrease the execution time
                val arbitraryStateCountAndSplit = Arb.int(stateCountRange).flatMap { randomStateCount ->
                    Arb.pair(arbitrary { randomStateCount }, Arb.int(0..max(randomStateCount - 1, 1)))
                }
                checkAll(arbitraryStateCountAndSplit) { (randomStateCount, randomSplit) ->
                    checkAll(loanStateSet(size = randomStateCount)) { randomStateSet ->
                        val (randomExistingStates, randomNewStates) = randomStateSet.let { orderedRandomStateSet ->
                            orderedRandomStateSet.take(randomSplit) to orderedRandomStateSet.drop(randomSplit)
                        }
                        shouldNotThrow<ContractViolationException> {
                            AppendLoanStatesContract(
                                existingServicingData = ServicingData.newBuilder().also { servicingDataBuilder ->
                                    servicingDataBuilder.clearLoanState()
                                    servicingDataBuilder.addAllLoanState(randomExistingStates)
                                }.build()
                            ).appendLoanStates(
                                randomNewStates
                            ).let { outputRecord ->
                                outputRecord.loanStateCount shouldBeExactly randomStateCount
                            }
                        }
                    }
                }
            }
        }
    }
}) {
    override fun testCaseOrder() = TestCaseOrder.Random
}
