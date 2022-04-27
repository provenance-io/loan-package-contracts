package io.provenance.scope.loan.contracts

import io.dartinc.registry.v1beta1.ENote
import io.provenance.scope.contract.annotations.Function
import io.provenance.scope.contract.annotations.Input
import io.provenance.scope.contract.annotations.Participants
import io.provenance.scope.contract.annotations.Record
import io.provenance.scope.contract.annotations.ScopeSpecification
import io.provenance.scope.contract.proto.Specifications
import io.provenance.scope.contract.spec.P8eContract
import io.provenance.scope.loan.LoanScopeFacts
import io.provenance.scope.loan.utility.isValid
import io.provenance.scope.loan.utility.orError
import io.provenance.scope.loan.utility.requireThat
import io.provenance.scope.loan.utility.validateRequirements

@Participants(roles = [Specifications.PartyType.OWNER])
@ScopeSpecification(["tech.figure.loan"])
open class RecordENoteContract: P8eContract() {

    @Function(invokedBy = Specifications.PartyType.OWNER)
    @Record(LoanScopeFacts.eNote)
    open fun recordENote(@Input(LoanScopeFacts.eNote) eNote: ENote) = eNote.also {
        validateRequirements(
            // TODO: Decide which fields should only be required if DART is listed as mortgagee of record/active custodian
            eNote.controller.controllerUuid.isValid()    orError "ENote missing controller UUID",
            eNote.controller.controllerName.isNotBlank() orError "ENote missing controller Name",
            eNote.eNote.id.isValid()                     orError "ENote missing ID",
            eNote.eNote.uri.isNotBlank()                 orError "ENote missing uri",
            eNote.eNote.contentType.isNotBlank()         orError "ENote missing content type",
            eNote.eNote.documentType.isNotBlank()        orError "ENote missing document type",
            eNote.eNote.checksum.isValid()               orError "ENote missing checksum",
            eNote.signedDate.isValid()                   orError "ENote missing signed date",
            eNote.vaultName.isNotBlank()                 orError "ENote missing vault name",
        )
    }
}