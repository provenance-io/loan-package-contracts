package io.provenance.scope.loan.contracts

import io.dartinc.registry.v1beta1.ENote
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
import io.provenance.scope.loan.utility.isValid
import io.provenance.scope.loan.utility.orError
import io.provenance.scope.loan.utility.validateRequirements
import tech.figure.util.v1beta1.DocumentMetadata
// TODO: Potentially remove in favor of append model rather than blanket overwrite
@Participants(roles = [PartyType.OWNER]) // TODO: Change to controller or ensure Authz grant to controller is made
@ScopeSpecification(["tech.figure.loan"])
open class UpdateENoteContract(
    @Record(LoanScopeFacts.eNote) val existingENote: ENote?,
) : P8eContract() {

    @Function(invokedBy = PartyType.OWNER) // TODO: Change to controller or ensure Authz grant to controller is made
    @Record(LoanScopeFacts.eNote)
    open fun updateENote(@Input(LoanScopeInputs.eNoteUpdate) newENote: DocumentMetadata): ENote {
        validateRequirements(ContractRequirementType.LEGAL_SCOPE_STATE,
            (existingENote !== null) orError "Cannot create eNote using this contract",
        )
        validateRequirements(ContractRequirementType.VALID_INPUT,
            newENote.id.isValid()              orError "ENote missing ID",
            newENote.uri.isNotBlank()          orError "ENote missing uri",
            newENote.contentType.isNotBlank()  orError "ENote missing content type",
            newENote.documentType.isNotBlank() orError "ENote missing document type",
            newENote.checksum.isValid()        orError "ENote missing checksum",
        )
        return existingENote!!.toBuilder().setENote(newENote).build()
    }
}
