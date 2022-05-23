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
import io.provenance.scope.loan.LoanScopeProperties.assetLoanKey
import io.provenance.scope.loan.LoanScopeProperties.assetMismoKey
import io.provenance.scope.loan.utility.ContractRequirementType.VALID_INPUT
import io.provenance.scope.loan.utility.eNoteInputValidation
import io.provenance.scope.loan.utility.isValid
import io.provenance.scope.loan.utility.loanDocumentInputValidation
import io.provenance.scope.loan.utility.loanValidationInputValidation
import io.provenance.scope.loan.utility.orError
import io.provenance.scope.loan.utility.raiseError
import io.provenance.scope.loan.utility.servicingRightsInputValidation
import io.provenance.scope.loan.utility.tryUnpackingAs
import io.provenance.scope.loan.utility.updateServicingData
import io.provenance.scope.loan.utility.validateRequirements
import tech.figure.asset.v1beta1.Asset
import tech.figure.loan.v1beta1.LoanDocuments
import tech.figure.loan.v1beta1.MISMOLoanMetadata
import tech.figure.servicing.v1beta1.LoanStateOuterClass.ServicingData
import tech.figure.servicing.v1beta1.ServicingRightsOuterClass.ServicingRights
import tech.figure.validation.v1beta1.LoanValidation
import tech.figure.loan.v1beta1.Loan as FigureTechLoan

/**
 * A contract designed to be a temporary workaround to pending support in `p8e-scope-sdk` for optional constructor record values.
 */
@Participants(roles = [PartyType.OWNER])
@ScopeSpecification(["tech.figure.loan"])
class InitializeLoanScope : P8eContract() {

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.asset)
    fun recordAsset(@Input(LoanScopeFacts.asset) asset: Asset) = asset.also {
        // TODO: Not DRYing this body since the asset record is only modified by RecordLoanContract and this
        validateRequirements(VALID_INPUT) {
            requireThat(
                asset.id.isValid()      orError "Asset must have valid ID",
                asset.type.isNotBlank() orError "Asset is missing type",
            )
            if (asset.containsKv(assetLoanKey) xor asset.containsKv(assetMismoKey)) {
                asset.kvMap[assetLoanKey]?.tryUnpackingAs<FigureTechLoan>("input asset's \"${assetLoanKey}\"") { newLoan ->
                    requireThat(
                        newLoan.id.isValid()                orError "Loan must have valid ID",
                        newLoan.originatorName.isNotBlank() orError "Loan is missing originator name",
                    )
                }
                asset.kvMap[assetMismoKey]?.tryUnpackingAs<MISMOLoanMetadata>("input asset's \"${assetMismoKey}\"") { newLoan ->
                    // TODO: Investigate wrapping protoc validate.rules call into ContractViolation somehow instead
                    requireThat(
                        (newLoan.uli.length in 23..45) orError "Loan ULI is invalid", // TODO: Any other requirements for ULI?
                    )
                }
            } else {
                raiseError("Exactly one of \"$assetLoanKey\" or \"$assetMismoKey\" must be a key in the input asset")
            }
        }
    }

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.documents)
    fun recordDocuments(@Input(LoanScopeFacts.documents) documents: LoanDocuments) = documents.also(loanDocumentInputValidation)

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.servicingData)
    fun recordServicingData(@Input(LoanScopeFacts.servicingData) servicingData: ServicingData) = updateServicingData(newServicingData = servicingData)

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.servicingRights)
    fun recordServicingRights(@Input(LoanScopeFacts.servicingRights) servicingRights: ServicingRights) = servicingRights.also(
        servicingRightsInputValidation
    )

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.loanValidations)
    fun recordValidationData(@Input(LoanScopeFacts.loanValidations) loanValidations: LoanValidation) = loanValidations.also(
        loanValidationInputValidation
    )

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.eNote)
    fun recordENote(@Input(LoanScopeFacts.eNote) eNote: ENote) = eNote.also(eNoteInputValidation)
}
