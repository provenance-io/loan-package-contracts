package io.provenance.contracts

import io.p8e.annotations.Input
import io.p8e.annotations.Participants
import io.p8e.annotations.ScopeSpecification
import io.provenance.LoanScopeFacts
import io.p8e.proto.ContractSpecs.PartyType.OWNER
import io.p8e.spec.P8eContract

@Participants([OWNER])
@ScopeSpecification(["tech.figure.loan"])
open class RecordLoanContract(
    @Record(LoanScopeFacts.asset) val existingAsset: Asset,
    @Record(LoanScopeFacts.eNote) val existingENote: io.dartinc.v1beta1.ENote,
) : P8eContract() {

    @Function(OWNER)
    @Record(LoanScopeFacts.asset)
    open fun recordAsset(@Input(LoanScopeFacts.asset) asset: Asset) = asset
        .also {
            if (existingAsset != null) {
                // Flag that the asset is an eNote
                require(existingAsset.kv.loan.isENote.isFalse()) { "asset cannot be updated" }
                // optional: make sure nothing important changed
                // examples:
                require(existingAsset.id == asset.id) { "cannot change asset ID" }
                require(existingAsset.type == asset.type) { "cannot change asset type" }
                require(existingAsset.kv.loan.originatorUuid == asset.kv.loan.originatorUuid) { "cannot change loan originator UUID" }
                require(existingAsset.kv.loan.originatorName == asset.kv.loan.originatorName) { "cannot change loan originator name" }
            } else {
                // other validation rules, such as:
                require(asset.id.isValid()) { "asset.id is missing" }
                require(asset.type.isNotBlank()) { "asset.type is missing" }
                require(asset.kv.loan.originatorUuid.isValid()) { "asset.kv.loan.originatorUuid is missing" }
                require(asset.kv.loan.originatorName.isNotBlank()) { "asset.kv.loan.originatorName is missing" }
            }
        }

    @Function(OWNER)
    @Record(LoanScopeFacts.servicingRights)
    open fun recordServicingRights(@Input(LoanScopeFacts.servicingRights) servicingRights: tech.figure.servicing.v1beta1.ServicingRights) = servicingRights

    @Function(OWNER)
    @Record(LoanScopeFacts.documents)
    open fun recordDocuments(@Input(LoanScopeFacts.documents) documents: tech.figure.util.v1beta1.DocumentList) = documents

    @Function(OWNER)
    @Record(LoanScopeFacts.loanStates)
    open fun recordLoanStates(@Input(LoanScopeFacts.loanStates) loanStates: tech.figure.servicing.v1beta1.LoanStateList) = loanStates

    @Function(OWNER)
    @Record(LoanScopeFacts.validationResults)
    open fun recordValidationResults(@Input(LoanScopeFacts.validationResults) validationResults: tech.figure.validation.v1beta1.ValidationResults) = validationResults

    @Function(OWNER)
    @Record(LoanScopeFacts.eNote)
    open fun recordENote(@Input(LoanScopeFacts.eNote) eNote: io.dartinc.registry.v1beta1.ENote) = eNote?
    .also {
        if (existingENote != null) {
            require(existingENote.checksum == eNote.checksum) { "cannot modify or remove eNote during loan onboarding" } // use specific contract instead
        }
        // TODO: Decide which fields should only be required if DART is listed as mortgagee of record/active custodian
        require(eNote.controller.controllerUuid.isValid()) { "ENote missing controller UUID" }
        require(eNote.controller.controllerName.isNotBlank()) { "ENote missing controller Name" }
        require(eNote.eNote.id.isValid()) { "ENote missing ID" }
        require(eNote.eNote.uri.isNotBlank()) { "ENote missing uri" }
        require(eNote.eNote.content_type.isNotBlank()) { "ENote missing content type" }
        require(eNote.eNote.document_type.isNotBlank()) { "ENote missing document type" }
        require(eNote.eNote.checksum.isNotBlank()) { "ENote missing checksum" }
        require(eNote.signedDate.isNotBlank()) { "ENote missing signed date" }
        require(eNote.vaultName.isNotBlank()) { "ENote missing vault name" }
    }

}