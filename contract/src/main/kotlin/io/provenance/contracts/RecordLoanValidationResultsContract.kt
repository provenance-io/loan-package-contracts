package io.provenance.contracts

import io.p8e.annotations.Input
import io.p8e.annotations.Participants
import io.p8e.annotations.ScopeSpecification
import io.provenance.LoanScopeFacts
import io.p8e.proto.ContractSpecs.PartyType.OWNER
import io.p8e.spec.P8eContract

@Participants([OWNER])
@ScopeSpecification(["tech.figure.loan"])
open class RecordLoanValidationResultsContract(
    @Record(LoanScopeFacts.validationResults) val existingResults: tech.figure.validation.v1beta1.ValidationResults,
) : P8eContract() {

    @Function(OWNER)
    @Record(LoanScopeFacts.validationResults)
    open fun recordLoanValidationResults(@Input(name = "newResults") newResults: tech.figure.validation.v1beta1.ValidationResultSet) {
        require(newResults.resultSetUuid.isValid()) { "Result Set UUID is missing" }
        require(newResults.resultSetDate.isNotBlank()) { "Result Set Date is missing" }
        require(newResults.resultSetProvider.isNotBlank()) { "Result Set Provider is missing" }
        require(newResults.ruleSetUuid.isValid()) { "Rule Set UUID is missing" }
        require(newResults.ruleSetDate.isNotBlank()) { "Rule Set Date is missing" }
        require(newResults.ruleSetProvider.isNotBlank()) { "Rule Set Provider is missing" }
        require(newResults.ruleSetName.isNotBlank()) { "Rule Set Name is missing" }
        require(newResults.validationExceptionCount >= 0) { "Invalid Validation Exception Count" }
        require(newResults.validationWarningCount >= 0) { "Invalid Validation Warning Count" }
        var newResultsBuilder = ValidationResults.newBuilder().mergeFrom(existingResults)
        newResultsBuilder.addValidationResultSet(newResults.validationResultSet)
        if (!existingResults.hasField(latestResultSetDate) || newResults.resultSetDate > existingResults.latestResultSetDate) {
            newResultsBuilder.setLatestResultSetDate(newResults.resultSetDate)
            if (newResults.hasField(ratingAgencyGrading)) newResultsBuilder.setLatestGrading(newResults.ratingAgencyGrading)
            // TODO: how to merge exception/warning counts? override vs. merge unique
        }
        return newResultsBuilder.build()
    }

}