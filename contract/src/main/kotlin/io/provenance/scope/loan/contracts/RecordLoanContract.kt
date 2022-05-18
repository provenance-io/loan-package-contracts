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
import io.provenance.scope.loan.utility.documentModificationValidation
import io.provenance.scope.loan.utility.documentValidation
import io.provenance.scope.loan.utility.eNoteValidation
import io.provenance.scope.loan.utility.isSet
import io.provenance.scope.loan.utility.isValid
import io.provenance.scope.loan.utility.orError
import io.provenance.scope.loan.utility.raiseError
import io.provenance.scope.loan.utility.servicingRightsInputValidation
import io.provenance.scope.loan.utility.toFigureTechLoan
import io.provenance.scope.loan.utility.toMISMOLoan
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
            if (existingAsset.isSet()) {
                requireThat(
                    // Flag that the asset is an eNote
                    // existingLoan.isENote.isFalse()     orError "Asset cannot be updated", // TODO: Determine how to do
                    // optional: make sure nothing important changed
                    // examples:
                    (existingAsset.id == newAsset.id)     orError "Cannot change asset ID",
                    (existingAsset.type == newAsset.type) orError "Cannot change asset type",
                )
            } else {
                requireThat(
                    // other validation rules, such as:
                    newAsset.id.isValid()      orError "Asset is missing valid ID",
                    newAsset.type.isNotBlank() orError "Asset is missing type",
                )
            }
            if (newAsset.containsKv("loan") xor newAsset.containsKv("mismoLoan")) {
                newAsset.kvMap["loan"]?.let { newLoanValue ->
                    newLoanValue.tryUnpackingAs<FigureTechLoan>("input asset's \"loan\"") { newLoan ->
                        if (existingAsset.isSet()) {
                            existingAsset.kvMap["loan"]?.toFigureTechLoan()?.let { existingLoan ->
                                requireThat(
                                    (existingLoan.id == newLoan.id)                          orError "Cannot change loan ID",
                                    (existingLoan.originatorName == newLoan.originatorName) orError "Cannot change loan originator name",
                                )
                            } ?: raiseError("The input asset had key \"loan\" but the existing asset did not")
                        } else {
                            requireThat(
                                newLoan.id.isValid()                orError "Loan is missing valid ID",
                                newLoan.originatorName.isNotBlank() orError "Loan is missing originator name",
                            )
                        }
                    }
                }
                newAsset.kvMap["mismoLoan"]?.let { newLoanValue ->
                    newLoanValue.tryUnpackingAs<MISMOLoanMetadata>("input asset's \"mismoLoan\"") { newLoan ->
                        if (existingAsset.isSet()) {
                            existingAsset.kvMap["mismoLoan"]?.toMISMOLoan()?.let { existingLoan ->
                                documentModificationValidation(existingLoan.document, newLoan.document)
                                requireThat(
                                    (existingLoan.uli == newLoan.uli) orError "Cannot change loan ULI",
                                )
                            } ?: raiseError("The input asset had key \"mismoLoan\" but the existing asset did not")
                        } else {
                            // TODO: Investigate wrapping protoc validate.rules call into ContractViolation somehow instead
                            requireThat(
                                (newLoan.uli.length in 23..45) orError "Loan ULI is invalid", // TODO: Any other requirements for ULI?
                            )
                        }
                    }
                }
            } else {
                raiseError("Exactly one of \"loan\" or \"mismoLoan\" must be a key in the input asset")
            }
        }
    }

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.servicingRights)
    open fun recordServicingRights(@Input(LoanScopeFacts.servicingRights) servicingRights: ServicingRights) =
        servicingRights.also(servicingRightsInputValidation)

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.documents)
    open fun recordDocuments(@Input(LoanScopeFacts.documents) documents: LoanDocuments) = documents.also { input ->
        validateRequirements(VALID_INPUT) {
            requireThat(
                input.documentList.isNotEmpty() orError "Must supply at least one document" // TODO: Verify desired; not thrown for optional input
            )
            input.documentList.forEach { document ->
                documentValidation(document)
            }
        }
    }

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.servicingData)
    open fun recordServicingData(@Input(LoanScopeFacts.servicingData) servicingData: ServicingData) =
        updateServicingData(newServicingData = servicingData) // TODO: Add existing record to constructor, annotated as optional?

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
