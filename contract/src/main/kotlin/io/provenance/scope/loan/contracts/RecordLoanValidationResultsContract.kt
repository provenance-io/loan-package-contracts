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
import io.provenance.scope.loan.utility.isValid
import tech.figure.validation.v1beta1.ValidationIteration
import tech.figure.validation.v1beta1.ValidationResults

@Participants(roles = [PartyType.OWNER]) // TODO: Change to VALIDATOR?
@ScopeSpecification(["tech.figure.loan"])
open class RecordLoanValidationResultsContract(
    @Record(LoanScopeFacts.validationResults) val validationIteration: ValidationIteration,
) : P8eContract() {

    @Function(invokedBy = PartyType.OWNER) // TODO: Change to VALIDATOR?
    @Record(LoanScopeFacts.validationResults)
    open fun recordLoanValidationResults(@Input(name = "newResults") newResults: ValidationResults): ValidationResults {
        validateRequirements(
            newResults.resultSetUuid.isValid()          orError "Result set UUID is missing",
            newResults.resultSetEffectiveTime.isValid() orError "Result set date is missing",
            (newResults.validationExceptionCount >= 0)  orError "Invalid validation exception count",
            (newResults.validationWarningCount >= 0)    orError "Invalid validation warning count",
            (newResults.validationItemsCount > 0)       orError "No validation items were provided",
            newResults.resultSetProvider.isNotBlank()   orError "Result set provider is missing",
            (newResults.resultSetProvider == validationIteration.request.requesterName)
                    orError "Result set provider does not match what was requested in this validation iteration",
        )
        // TODO: Update just the relevant ValidationIteration and change method signature & annotations accordingly
        /*val newResultsBuilder = ValidationResults.newBuilder().mergeFrom(existingResults)
        newResultsBuilder.addValidationResultSet(newResults.validationResultSet)
        if (!existingResults.hasField(latestResultSetDate) || newResults.resultSetDate > existingResults.latestResultSetDate) {
            newResultsBuilder.setLatestResultSetDate(newResults.resultSetDate)
            if (newResults.hasField(ratingAgencyGrading)) newResultsBuilder.setLatestGrading(newResults.ratingAgencyGrading)
            // TODO: how to merge exception/warning counts? override vs. merge unique
        }
        return newResultsBuilder.build()*/
        return newResults // TODO: Temporary stopgap
    }
}
