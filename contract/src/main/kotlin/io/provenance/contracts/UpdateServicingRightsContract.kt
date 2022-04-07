package io.provenance.contracts

import io.p8e.annotations.Input
import io.p8e.annotations.Participants
import io.p8e.annotations.ScopeSpecification
import io.provenance.LoanScopeFacts
import io.p8e.proto.ContractSpecs.PartyType.OWNER
import io.p8e.spec.P8eContract

@Participants([OWNER])
@ScopeSpecification(["tech.figure.loan"])
open class UpdateServicingRightsContract : P8eContract() {

    @Function(OWNER)
    @Record(LoanScopeFacts.servicingRights)
    open fun updateServicingRights(@Input(LoanScopeFacts.servicingRights) servicingRights: tech.figure.servicing.v1beta1.ServicingRights) = servicingRights.also {
        require(servicingRights.servicerUuid.isValid()) { "Missing servicer UUID" }
        require(servicingRights.servicerName.isNotBlank()) { "Missing servicer name" }
    }
}