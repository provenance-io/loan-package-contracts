package io.provenance.scope.loan.contracts

import io.provenance.scope.contract.annotations.Function
import io.provenance.scope.contract.annotations.Input
import io.provenance.scope.contract.annotations.Participants
import io.provenance.scope.contract.annotations.Record
import io.provenance.scope.contract.annotations.ScopeSpecification
import io.provenance.scope.contract.proto.Specifications.PartyType
import io.provenance.scope.contract.spec.P8eContract
import io.provenance.scope.loan.LoanScopeFacts
import io.provenance.scope.loan.LoanScopeInputs
import io.provenance.scope.loan.utility.ContractRequirementType
import io.provenance.scope.loan.utility.isSet
import io.provenance.scope.loan.utility.isValid
import io.provenance.scope.loan.utility.orError
import io.provenance.scope.loan.utility.raiseError
import io.provenance.scope.loan.utility.validateRequirements
import tech.figure.validation.v1beta1.LoanValidation
import tech.figure.validation.v1beta1.ValidationResponse

@Participants(roles = [PartyType.OWNER]) // TODO: Ensure Authz grant to validator has been made
@ScopeSpecification(["tech.figure.loan"])
open class RecordLoanValidationResultsContract(
    @Record(LoanScopeFacts.loanValidations) val validationRecord: LoanValidation,
) : P8eContract() {

    @Function(invokedBy = PartyType.OWNER) // TODO: Ensure Authz grant to validator has been made
    @Record(LoanScopeFacts.loanValidations)
    open fun recordLoanValidationResults(
        @Input(LoanScopeInputs.validationResponse) submission: ValidationResponse
    ): LoanValidation {
        validateRequirements(ContractRequirementType.LEGAL_SCOPE_STATE,
            validationRecord.isSet() orError "A validation iteration must exist for results to be submitted",
        )
        validateRequirements(ContractRequirementType.VALID_INPUT) {
            requireThat(
                submission.requestId.isValid() orError "Response must have valid ID",
            )
            submission.results.takeIf { it.isSet() }?.let { submittedResults ->
                requireThat(
                    submittedResults.resultSetUuid.isValid()          orError "Response is missing result set UUID",
                    submittedResults.resultSetEffectiveTime.isValid() orError "Response is missing timestamp",
                    (submittedResults.validationExceptionCount >= 0)  orError "Results report an invalid validation exception count",
                    (submittedResults.validationWarningCount >= 0)    orError "Results report an invalid validation warning count",
                    (submittedResults.validationItemsCount > 0)       orError "Results must have at least one validation item",
                    submittedResults.resultSetProvider.isNotBlank()   orError "Results missing provider name",
                )
            } ?: raiseError("Response is missing results")
            validationRecord.iterationList.singleOrNull { iteration ->
                iteration.request.requestId == submission.requestId
            }.let { maybeIteration ->
                requireThat(
                    if (maybeIteration === null) {
                        false orError "No single validation iteration with a matching request ID exists"
                    } else {
                        (submission.results.resultSetProvider == maybeIteration.request.validatorName) orError
                            "Result set provider does not match what was requested in this validation iteration"
                    }
                )
            }
        }
        return validationRecord.iterationList.indexOfLast { iteration -> // The enforcement above ensures exactly one match
            iteration.request.requestId == submission.requestId
        }.let { index ->
            validationRecord.toBuilder().also { recordBuilder ->
                recordBuilder.setIteration(
                    index,
                    validationRecord.iterationList[index].toBuilder().apply {
                        results = submission.results
                    }.build()
                )
            }.build()
        }
    }
}
