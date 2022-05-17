package io.provenance.scope.loan.contracts

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainIgnoringCase
import io.kotest.property.checkAll
import io.provenance.scope.loan.test.Constructors.randomProtoUuid
import io.provenance.scope.loan.test.Constructors.resultsContractWithEmptyExistingRecord
import io.provenance.scope.loan.test.Constructors.resultsContractWithSingleRequest
import io.provenance.scope.loan.test.Constructors.validResultSubmission
import io.provenance.scope.loan.test.LoanPackageArbs.anyNonEmptyString
import io.provenance.scope.loan.test.LoanPackageArbs.anyUuid
import io.provenance.scope.loan.utility.ContractViolationException
import io.provenance.scope.loan.utility.IllegalContractStateException
import tech.figure.validation.v1beta1.ValidationResponse

class RecordLoanValidationResultsUnitTest : WordSpec({
    "recordLoanValidationResults" When {
        "executed without a validation request existing in the scope" should {
            "throw an appropriate exception" {
                shouldThrow<IllegalContractStateException> {
                    resultsContractWithEmptyExistingRecord.apply {
                        recordLoanValidationResults(validResultSubmission(randomProtoUuid))
                    }
                }.let { exception ->
                    exception.message shouldContainIgnoringCase "validation iteration must exist"
                }
            }
        }
        "given an empty input" should {
            "throw an appropriate exception" {
                ValidationResponse.getDefaultInstance().let { emptyResultSubmission ->
                    shouldThrow<ContractViolationException> {
                        resultsContractWithSingleRequest(randomProtoUuid).apply {
                            recordLoanValidationResults(emptyResultSubmission)
                        }
                    }.let { exception ->
                        exception.message shouldContain "Result set UUID is missing"
                    }
                }
            }
        }
        "given a valid input" should {
            "not throw an exception" {
                checkAll(anyUuid, anyNonEmptyString) { randomUuid, randomValidatorName ->
                    shouldNotThrow<ContractViolationException> {
                        resultsContractWithSingleRequest(
                            requestID = randomUuid,
                            validatorName = randomValidatorName,
                        ).apply {
                            recordLoanValidationResults(
                                validResultSubmission(
                                    iterationRequestID = randomUuid,
                                    resultSetProvider = randomValidatorName,
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}) {
    override fun testCaseOrder() = TestCaseOrder.Random
}
