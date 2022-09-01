package io.provenance.scope.loan.contracts

import io.dartinc.registry.v1beta1.ENote
import io.provenance.scope.contract.annotations.Function
import io.provenance.scope.contract.annotations.Input
import io.provenance.scope.contract.annotations.Participants
import io.provenance.scope.contract.annotations.Record
import io.provenance.scope.contract.annotations.ScopeSpecification
import io.provenance.scope.contract.annotations.SkipIfRecordExists
import io.provenance.scope.contract.proto.Specifications.PartyType
import io.provenance.scope.contract.spec.P8eContract
import io.provenance.scope.loan.LoanScopeFacts
import io.provenance.scope.loan.utility.ContractRequirementType.VALID_INPUT
import io.provenance.scope.loan.utility.eNoteInputValidation
import io.provenance.scope.loan.utility.updateServicingData
import io.provenance.scope.loan.utility.validateRequirements
import tech.figure.servicing.v1beta1.LoanStateOuterClass.ServicingData

@Participants(roles = [PartyType.OWNER])
@ScopeSpecification(["tech.figure.loan"])
open class RecordENoteContract(
    @Record(name = LoanScopeFacts.eNote, optional = true) val existingENote: ENote?,
    @Record(name = LoanScopeFacts.servicingData, optional = true) val existingServicingData: ServicingData?,
) : P8eContract() {

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.eNote)
    @SkipIfRecordExists(LoanScopeFacts.eNote)
    open fun recordENote(@Input(LoanScopeFacts.eNote) eNote: ENote): ENote = eNote.also(eNoteInputValidation)

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.servicingData)
    @SkipIfRecordExists(LoanScopeFacts.servicingData)
    open fun recordServicingData(@Input(LoanScopeFacts.servicingData) newServicingData: ServicingData): ServicingData =
        validateRequirements(VALID_INPUT) {
            updateServicingData(newServicingData = newServicingData)
        }
}
