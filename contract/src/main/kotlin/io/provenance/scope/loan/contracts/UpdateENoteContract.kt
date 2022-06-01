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
import io.provenance.scope.loan.utility.documentModificationValidation
import io.provenance.scope.loan.utility.eNoteDocumentValidation
import io.provenance.scope.loan.utility.validateRequirements
import tech.figure.util.v1beta1.DocumentMetadata

@Participants(roles = [PartyType.OWNER]) // TODO: Change to controller or ensure Authz grant to controller is made
@ScopeSpecification(["tech.figure.loan"])
open class UpdateENoteContract(
    @Record(name = LoanScopeFacts.eNote, optional = false) val existingENote: ENote,
) : P8eContract() {

    @Function(invokedBy = PartyType.OWNER) // TODO: Change to controller or ensure Authz grant to controller is made
    @Record(LoanScopeFacts.eNote)
    open fun updateENote(@Input(LoanScopeInputs.eNoteUpdate) newENote: DocumentMetadata): ENote {
        validateRequirements(ContractRequirementType.VALID_INPUT) {
            eNoteDocumentValidation(newENote)
            documentModificationValidation(
                existingDocument = existingENote.eNote,
                newDocument = newENote,
            )
        }
        return existingENote.toBuilder().setENote(newENote).build()
    }
}
