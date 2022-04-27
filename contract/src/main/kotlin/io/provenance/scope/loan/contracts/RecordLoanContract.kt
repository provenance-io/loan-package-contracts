package io.provenance.scope.loan.contracts

import io.dartinc.registry.v1beta1.ENote
import io.provenance.scope.contract.annotations.Function
import io.provenance.scope.contract.annotations.Input
import io.provenance.scope.contract.annotations.Participants
import io.provenance.scope.contract.annotations.ScopeSpecification
import io.provenance.scope.contract.annotations.Record
import io.provenance.scope.contract.proto.Specifications.PartyType
import io.provenance.scope.contract.spec.P8eContract
import io.provenance.scope.loan.LoanScopeFacts
import io.provenance.scope.loan.utility.isValid
import io.provenance.scope.loan.utility.orError
import io.provenance.scope.loan.utility.validateRequirements
import tech.figure.asset.v1beta1.Asset
import tech.figure.loan.v1beta1.LoanDocuments
import tech.figure.servicing.v1beta1.LoanStateOuterClass.LoanStates
import tech.figure.servicing.v1beta1.ServicingRightsOuterClass.ServicingRights
import tech.figure.validation.v1beta1.ValidationResults

@Participants(roles = [PartyType.OWNER])
@ScopeSpecification(["tech.figure.loan"])
open class RecordLoanContract(
    @Record(LoanScopeFacts.asset) val existingAsset: Asset? = null,
    @Record(LoanScopeFacts.eNote) val existingENote: ENote? = null,
) : P8eContract() {

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.asset)
    open fun recordAsset(@Input(LoanScopeFacts.asset) asset: Asset) = asset.also {
        // TODO: Add or implement correct extension for safe casting of kvMap values like "loan"
        /*validateRequirements {
            if (existingAsset != null) {
                requireThat(
                    // Flag that the asset is an eNote
                    existingAsset.kvMap["loan"].isENote.isFalse() orError "asset cannot be updated",
                    // optional: make sure nothing important changed
                    // examples:
                    existingAsset.id == asset.id orError "cannot change asset ID",
                    existingAsset.type == asset.type orError "cannot change asset type",
                    existingAsset.kvMap["loan"].originatorUuid == asset.kvMap["loan"].originatorUuid orError "cannot change loan originator UUID",
                    existingAsset.kvMap["loan"].originatorName == asset.kvMap["loan"].originatorName orError "cannot change loan originator name",
                )
            } else {
                requireThat(
                    // other validation rules, such as:
                    asset.id.isValid() orError "asset.id is missing",
                    asset.type.isNotBlank() orError "asset.type is missing",
                    asset.kvMap["loan"].originatorUuid.isValid() orError "asset.kv.loan.originatorUuid is missing",
                    asset.kvMap["loan"].originatorName.isNotBlank() orError "asset.kv.loan.originatorName is missing",
                )
            }
        }*/
    }

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.servicingRights)
    open fun recordServicingRights(@Input(LoanScopeFacts.servicingRights) servicingRights: ServicingRights) = servicingRights

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.documents)
    open fun recordDocuments(@Input(LoanScopeFacts.documents) documents: LoanDocuments) = documents

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.loanStates)
    open fun recordLoanStates(@Input(LoanScopeFacts.loanStates) loanStates: LoanStates) = loanStates

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.validationResults)
    open fun recordValidationResults(@Input(LoanScopeFacts.validationResults) validationResults: ValidationResults) = validationResults

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.eNote)
    open fun recordENote(@Input(LoanScopeFacts.eNote) eNote: ENote? = null) = eNote?.also {
        validateRequirements {
            if (existingENote != null) {
                requireThat((existingENote.eNote.checksum == it.eNote.checksum) orError "ENote with a different checksum already exists on chain for the specified scope; ENote modifications are not allowed!")
            }
            // TODO: Decide which fields should only be required if DART is listed as mortgagee of record/active custodian
            requireThat(
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
}
