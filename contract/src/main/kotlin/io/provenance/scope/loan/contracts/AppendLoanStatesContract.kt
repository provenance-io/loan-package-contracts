package io.provenance.scope.loan.contracts

import io.provenance.scope.contract.annotations.Function
import io.provenance.scope.contract.annotations.Input
import io.provenance.scope.contract.annotations.Participants
import io.provenance.scope.contract.annotations.Record
import io.provenance.scope.contract.annotations.ScopeSpecification
import io.provenance.scope.contract.proto.Specifications.PartyType
import io.provenance.scope.contract.spec.P8eContract
import io.provenance.scope.loan.LoanScopeFacts
import io.provenance.scope.loan.utility.ContractRequirementType.VALID_INPUT
import io.provenance.scope.loan.utility.isValid
import io.provenance.scope.loan.utility.orError
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
    open fun appendLoanStates(@Input(LoanScopeFacts.servicingData) newLoanStates: List<LoanStateMetadata>): ServicingData {
        val updatedServicingData = ServicingData.newBuilder().mergeFrom(existingServicingData)
        validateRequirements(VALID_INPUT) {
            for (state in newLoanStates) {
                // TODO: Improve check for duplicates (ID, effectiveTime, & checksum) & confirm if/which we want to silently ignore or raise violation
                requireThat(
                    state.id.isValid()              orError "Invalid id",
                    state.effectiveTime.isValid()   orError "Invalid effective time",
                    state.uri.isNotBlank()          orError "Invalid accrued interest",
                    state.checksum.isValid()        orError "Invalid checksum"
                )
                if (existingServicingData.loanStateList.none { it.effectiveTime == state.effectiveTime }) {
                    updatedServicingData.addLoanState(state)
                }
            }
        }
        return updatedServicingData.build()
    }
}
