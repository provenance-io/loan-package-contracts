package io.provenance.scope.loan.contracts

import io.provenance.scope.contract.annotations.Function
import io.provenance.scope.contract.annotations.Input
import io.provenance.scope.contract.annotations.Participants
import io.provenance.scope.contract.annotations.Record
import io.provenance.scope.contract.annotations.ScopeSpecification
import io.provenance.scope.loan.LoanScopeFacts
import io.provenance.scope.loan.utility.orError
import io.provenance.scope.loan.utility.validateRequirements
import io.provenance.scope.contract.proto.Specifications.PartyType
import io.provenance.scope.contract.spec.P8eContract
import io.provenance.scope.loan.proto.ValidationResultSubmission
import io.provenance.scope.loan.utility.isValid
import tech.figure.validation.v1beta1.Validation

@Participants(roles = [PartyType.OWNER]) // TODO: Change to VALIDATOR?
@ScopeSpecification(["tech.figure.loan"])
open class RecordLoanValidationResultsContract(
    @Record(LoanScopeFacts.validationResults) val validationRecord: Validation,
) : P8eContract() {

    @Function(invokedBy = PartyType.CUSTODIAN) // TODO: Change to VALIDATOR?
    @Record(LoanScopeFacts.validationResults)
    open fun recordLoanValidationResults(
        @Input(name = "resultSubmission") submission: ValidationResultSubmission
    ): Validation {
        validateRequirements {
            requireThat(
                submission.results.resultSetUuid.isValid()          orError "Result set UUID is missing",
                submission.requestId.isValid()                      orError "Request ID is missing",
                submission.results.resultSetEffectiveTime.isValid() orError "Result set date is missing",
                (submission.results.validationExceptionCount >= 0)  orError "Invalid validation exception count",
                (submission.results.validationWarningCount >= 0)    orError "Invalid validation warning count",
                (submission.results.validationItemsCount > 0)       orError "No validation items were provided",
                submission.results.resultSetProvider.isNotBlank()   orError "Result set provider is missing",
            )
            validationRecord.iterationList.singleOrNull { iteration ->
                iteration.request.requestId == submission.requestId
            }.let { maybeIteration ->
                requireThat(
                    if (maybeIteration === null) {
                        false orError "No single validation iteration with a matching request ID exists"
                    } else {
                        (submission.results.resultSetProvider == maybeIteration.request.requesterName) orError
                            "Result set provider does not match what was requested in this validation iteration"
                    }
                )
            }
        }
        return validationRecord.iterationList.indexOfLast { iteration -> // The enforcement above ensures exactly one match
            iteration.request.requestId == submission.requestId
        }.let { index ->
            validationRecord.toBuilder().also { recordBuilder ->
                recordBuilder.iterationList[index] = validationRecord.iterationList[index].toBuilder().apply {
                    results = submission.results
                }.build()
            }.build()
        }
    }
}
