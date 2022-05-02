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
import tech.figure.servicing.v1beta1.ServicingRightsOuterClass.ServicingRights

@Participants(roles = [PartyType.OWNER])
@ScopeSpecification(["tech.figure.loan"])
open class UpdateServicingRightsContract : P8eContract() {

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.servicingRights)
    open fun updateServicingRights(@Input(LoanScopeFacts.servicingRights) servicingRights: ServicingRights) = servicingRights.also {
        validateRequirements(VALID_INPUT,
            servicingRights.servicerId.isValid()      orError "Missing servicer UUID",
            servicingRights.servicerName.isNotBlank() orError "Missing servicer name",
        )
    }
}
