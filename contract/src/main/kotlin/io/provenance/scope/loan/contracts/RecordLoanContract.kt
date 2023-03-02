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
import io.provenance.scope.loan.LoanScopeProperties.assetLoanKey
import io.provenance.scope.loan.LoanScopeProperties.assetMismoKey
import io.provenance.scope.loan.utility.ContractRequirementType.VALID_INPUT
import io.provenance.scope.loan.utility.documentValidation
import io.provenance.scope.loan.utility.eNoteInputValidation
import io.provenance.scope.loan.utility.fundingValidation
import io.provenance.scope.loan.utility.isSet
import io.provenance.scope.loan.utility.isValid
import io.provenance.scope.loan.utility.loanDocumentInputValidation
import io.provenance.scope.loan.utility.loanValidationInputValidation
import io.provenance.scope.loan.utility.orError
import io.provenance.scope.loan.utility.servicingRightsInputValidation
import io.provenance.scope.loan.utility.toFigureTechLoan
import io.provenance.scope.loan.utility.tryUnpackingAs
import io.provenance.scope.loan.utility.uliValidation
import io.provenance.scope.loan.utility.updateServicingData
import io.provenance.scope.loan.utility.validateRequirements
import tech.figure.asset.v1beta1.Asset
import tech.figure.loan.v1beta1.LoanDocuments
import tech.figure.loan.v1beta1.MISMOLoanMetadata
import tech.figure.servicing.v1beta1.LoanStateOuterClass.ServicingData
import tech.figure.servicing.v1beta1.ServicingRightsOuterClass.ServicingRights
import tech.figure.loan.v1beta1.Loan as FigureTechLoan
import tech.figure.validation.v1beta1.LoanValidation as LoanValidation_v1beta1
import tech.figure.validation.v1beta2.LoanValidation as LoanValidation_v1beta2

@Participants(roles = [PartyType.OWNER])
@ScopeSpecification(["tech.figure.loan"])
open class RecordLoanContract(
    @Record(name = LoanScopeFacts.asset, optional = true) val existingAsset: Asset?,
    @Record(name = LoanScopeFacts.eNote, optional = true) val existingENote: ENote?,
    @Record(name = LoanScopeFacts.servicingData, optional = true) val existingServicingData: ServicingData?,
    @Record(name = LoanScopeFacts.servicingRights, optional = true) val existingServicingRights: ServicingRights?,
) : P8eContract() {

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.asset)
    open fun recordAsset(@Input(LoanScopeFacts.asset) newAsset: Asset) = newAsset.also {
        validateRequirements(VALID_INPUT) {
            if (existingAsset.isSet()) {
                requireThat(
                    (existingAsset!!.id == newAsset.id)   orError "Cannot change asset ID",
                    (existingAsset.type == newAsset.type) orError "Cannot change asset type",
                )
            } else {
                requireThat(
                    newAsset.id.isValid()      orError "Asset must have valid ID",
                    newAsset.type.isNotBlank() orError "Asset is missing type",
                )
            }
            if (newAsset.containsKv(assetLoanKey)) {
                newAsset.kvMap[assetLoanKey]!!.let { newLoanValue ->
                    newLoanValue.tryUnpackingAs<FigureTechLoan, Unit>("input asset's \"${assetLoanKey}\"") { newLoan ->
                        if (existingAsset.isSet()) {
                            existingAsset!!.kvMap[assetLoanKey]?.toFigureTechLoan()?.also { existingLoan ->
                                requireThat(
                                    (existingLoan.id == newLoan.id)                         orError "Cannot change loan ID",
                                    (existingLoan.originatorName == newLoan.originatorName) orError "Cannot change loan originator name",
                                )
                            }
                        } else {
                            requireThat(
                                newLoan.id.isValid()                orError "Loan must have valid ID",
                                newLoan.originatorUuid.isValid()    orError "Loan must have valid originator ID",
                                newLoan.originatorName.isNotBlank() orError "Loan is missing originator name",
                            )
                            uliValidation(newLoan.uli)
                            newLoan.funding.takeIf { it.isSet() }?.let { newLoanFunding ->
                                fundingValidation(newLoanFunding)
                            }
                        }
                    }
                }
            } else {
                raiseError("\"${assetLoanKey}\" must be a key in the input asset")
            }
            newAsset.kvMap[assetMismoKey]?.let { newMismoLoanValue ->
                newMismoLoanValue.tryUnpackingAs<MISMOLoanMetadata, Unit>("input asset's \"${assetMismoKey}\"") { newLoan ->
                    documentValidation(newLoan.document)
                }
            }
        }
    }

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.servicingRights)
    @SkipIfRecordExists(LoanScopeFacts.servicingRights)
    open fun recordServicingRights(@Input(LoanScopeFacts.servicingRights) servicingRights: ServicingRights) =
        servicingRights.also(servicingRightsInputValidation)

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.documents)
    open fun recordDocuments(@Input(LoanScopeFacts.documents) documents: LoanDocuments) = documents.also(loanDocumentInputValidation)

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.servicingData)
    open fun recordServicingData(@Input(LoanScopeFacts.servicingData) servicingData: ServicingData) =
        validateRequirements(VALID_INPUT) {
            updateServicingData(
                existingServicingData = existingServicingData ?: ServicingData.getDefaultInstance(),
                newServicingData = servicingData,
            )
        }

    @Suppress("deprecation")
    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.loanValidations)
    /**
     * Overwrites the value of the deprecated validation record.
     *
     * This function exists to provide backwards compatibility for scopes which used a validation record before the newer one was introduced.
     */
    open fun recordOldValidationData(@Input(LoanScopeFacts.loanValidations) loanValidations: LoanValidation_v1beta1) = loanValidations

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.loanValidationMetadata)
    open fun recordValidationData(@Input(LoanScopeFacts.loanValidationMetadata) loanValidations: LoanValidation_v1beta2) = loanValidations.also(
        loanValidationInputValidation
    )

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.eNote)
    @SkipIfRecordExists(LoanScopeFacts.eNote)
    open fun recordENote(@Input(LoanScopeFacts.eNote) eNote: ENote) = eNote.also(eNoteInputValidation)
}
