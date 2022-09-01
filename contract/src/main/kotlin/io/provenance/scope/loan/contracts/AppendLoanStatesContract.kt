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
import io.provenance.scope.loan.utility.ContractRequirementType.VALID_INPUT
import io.provenance.scope.loan.utility.updateServicingData
import io.provenance.scope.loan.utility.validateRequirements
import tech.figure.servicing.v1beta1.LoanStateOuterClass.LoanStateSubmission
import tech.figure.servicing.v1beta1.LoanStateOuterClass.ServicingData

@Participants(roles = [PartyType.SERVICER])
@ScopeSpecification(["tech.figure.loan"])
open class AppendLoanStatesContract(
    @Record(name = LoanScopeFacts.servicingData, optional = true) val existingServicingData: ServicingData?,
) : P8eContract() {

    @Function(invokedBy = PartyType.SERVICER)
    @Record(LoanScopeFacts.servicingData)
    open fun appendLoanStates(@Input(LoanScopeInputs.newLoanStates) newLoanStates: LoanStateSubmission): ServicingData =
        validateRequirements(VALID_INPUT) {
            updateServicingData(
                existingServicingData = existingServicingData ?: ServicingData.getDefaultInstance(),
                newServicingData = ServicingData.newBuilder().also { incomingBuilder ->
                    incomingBuilder.addAllLoanState(newLoanStates.loanStateList)
                }.build(),
                expectLoanStates = true,
            )
        }
}
