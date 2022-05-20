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
import io.provenance.scope.loan.utility.appendLoanStates
import io.provenance.scope.loan.utility.validateRequirements
import tech.figure.servicing.v1beta1.LoanStateOuterClass.LoanStateMetadata
import tech.figure.servicing.v1beta1.LoanStateOuterClass.ServicingData

@Participants(roles = [PartyType.OWNER]) // TODO: Eventually update to servicer
@ScopeSpecification(["tech.figure.loan"])
open class AppendLoanStatesContract(
    @Record(LoanScopeFacts.servicingData) val existingServicingData: ServicingData,
) : P8eContract() {

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.servicingData)
    open fun appendLoanStates(@Input(LoanScopeInputs.newLoanStates) newLoanStates: Collection<LoanStateMetadata>): ServicingData =
        ServicingData.newBuilder(existingServicingData).also { servicingDataBuilder ->
            validateRequirements(ContractRequirementType.VALID_INPUT) {
                appendLoanStates(servicingDataBuilder, newLoanStates)
            }
        }.build()
}
