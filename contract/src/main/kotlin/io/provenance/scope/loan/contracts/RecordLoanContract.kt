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
import io.provenance.scope.loan.utility.ContractRequirementType.VALID_INPUT
import io.provenance.scope.loan.utility.UnexpectedContractStateException
import io.provenance.scope.loan.utility.documentListInputValidation
import io.provenance.scope.loan.utility.eNoteValidation
import io.provenance.scope.loan.utility.isSet
import io.provenance.scope.loan.utility.isValid
import io.provenance.scope.loan.utility.orError
import io.provenance.scope.loan.utility.servicingRightsInputValidation
import io.provenance.scope.loan.utility.toLoan
import io.provenance.scope.loan.utility.validateRequirements
import tech.figure.asset.v1beta1.Asset
import tech.figure.loan.v1beta1.LoanDocuments
import tech.figure.servicing.v1beta1.LoanStateOuterClass.ServicingData
import tech.figure.servicing.v1beta1.ServicingRightsOuterClass.ServicingRights
import tech.figure.validation.v1beta1.LoanValidation

@Participants(roles = [PartyType.OWNER])
@ScopeSpecification(["tech.figure.loan"])
open class RecordLoanContract(
    @Record(LoanScopeFacts.asset) val existingAsset: Asset,
    @Record(LoanScopeFacts.eNote) val existingENote: ENote,
) : P8eContract() {

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.asset)
    open fun recordAsset(@Input(LoanScopeFacts.asset) newAsset: Asset) = newAsset.also {
        validateRequirements(VALID_INPUT) {
            val newLoan = newAsset.kvMap["loan"]?.toLoan()
                ?: throw UnexpectedContractStateException("No key \"loan\" was found in the input asset")
            if (existingAsset.isSet()) {
                val existingLoan = existingAsset.kvMap["loan"]?.toLoan()
                    ?: throw UnexpectedContractStateException("No key \"loan\" was found in the existing asset record")
                requireThat(
                    // Flag that the asset is an eNote
                    // existingLoan.isENote.isFalse()                               orError "Asset cannot be updated", // TODO: Determine how to do
                    // optional: make sure nothing important changed
                    // examples:
                    (existingAsset.id == newAsset.id)                            orError "Cannot change asset ID",
                    (existingAsset.type == newAsset.type)                        orError "Cannot change asset type",
                    // (existingLoan.originatorUuid == existingLoan.originatorUuid) orError "Cannot change loan originator UUID", // TODO: Remove?
                    (existingLoan.originatorName == existingLoan.originatorName) orError "Cannot change loan originator name",
                )
            } else {
                requireThat(
                    // other validation rules, such as:
                    newAsset.id.isValid()               orError "Asset ID is missing",
                    newAsset.type.isNotBlank()          orError "Asset type is missing",
                    // newLoan.originatorUuid.isValid()    orError "asset.kv.loan.originatorUuid is missing", // TODO: Remove?
                    newLoan.originatorName.isNotBlank() orError "asset.kv.loan.originatorName is missing",
                )
            }
        }
    }

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.servicingRights)
    open fun recordServicingRights(@Input(LoanScopeFacts.servicingRights) servicingRights: ServicingRights) =
        servicingRights.also(servicingRightsInputValidation)

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.documents)
    open fun recordDocuments(@Input(LoanScopeFacts.documents) documents: LoanDocuments) = documents.also(documentListInputValidation)

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.servicingData)
    open fun recordServicingData(@Input(LoanScopeFacts.servicingData) servicingData: ServicingData) = servicingData // TODO: Validate input

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.loanValidations)
    open fun recordValidationData(@Input(LoanScopeFacts.loanValidations) loanValidations: LoanValidation) = loanValidations // TODO: Validate input

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.eNote)
    open fun recordENote(@Input(LoanScopeFacts.eNote) eNote: ENote) = eNote.also {
        validateRequirements(VALID_INPUT) {
            if (existingENote.isSet()) {
                requireThat((existingENote.eNote.checksum == it.eNote.checksum) orError
                    "ENote with a different checksum already exists on chain for the specified scope; ENote modifications are not allowed!"
                )
            }
            eNoteValidation(eNote)
        }
    }
}
