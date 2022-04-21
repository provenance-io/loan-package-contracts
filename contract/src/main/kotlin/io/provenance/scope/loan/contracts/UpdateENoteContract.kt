package io.provenance.scope.loan.contracts

import io.dartinc.registry.v1beta1.ENote
import io.provenance.scope.contract.annotations.Function
import io.provenance.scope.contract.annotations.Input
import io.provenance.scope.contract.annotations.Participants
import io.provenance.scope.contract.annotations.Record
import io.provenance.scope.contract.annotations.ScopeSpecification
import io.provenance.scope.loan.LoanScopeFacts
import io.provenance.scope.contract.proto.Specifications.PartyType
import io.provenance.scope.contract.spec.P8eContract
import io.provenance.scope.loan.utility.LoanPackageContract
import io.provenance.scope.loan.utility.isValid
import io.provenance.scope.loan.utility.orError
import io.provenance.scope.loan.utility.validateRequirements

@Participants(roles = [PartyType.OWNER]) // TODO: Replace OWNER with CONTROLLER
@ScopeSpecification(["tech.figure.loan"])
@LoanPackageContract("UpdateENoteContract")
open class UpdateENoteContract(
    @Record(LoanScopeFacts.eNote) val existingENote: ENote?, // TODO: Confirm if this should be nullable and adjust code below accordingly
) : P8eContract() {
    @Function(invokedBy = PartyType.OWNER)  // TODO: Replace OWNER with CONTROLLER
    @Record(LoanScopeFacts.eNote)
    open fun updateENote(@Input(name = "newENote") newENote: tech.figure.util.v1beta1.DocumentMetadata): ENote {
        validateRequirements(
            (existingENote !== null)           orError "Cannot create eNote using this contract",
            newENote.id.isValid()              orError "ENote missing ID",
            newENote.uri.isNotBlank()          orError "ENote missing uri",
            newENote.contentType.isNotBlank()  orError "ENote missing content type",
            newENote.documentType.isNotBlank() orError "ENote missing document type",
            newENote.checksum.isValid()        orError "ENote missing checksum",
        )
        return existingENote!!.toBuilder().setENote(newENote).build()
    }
}
