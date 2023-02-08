package io.provenance.scope.loan

import io.dartinc.registry.v1beta1.ENote
import io.provenance.scope.contract.annotations.Record
import io.provenance.scope.contract.annotations.ScopeSpecificationDefinition
import io.provenance.scope.contract.proto.Specifications
import io.provenance.scope.contract.spec.P8eScopeSpecification
import tech.figure.asset.v1beta1.Asset
import tech.figure.loan.v1beta1.LoanDocuments
import tech.figure.servicing.v1beta1.LoanStateOuterClass.ServicingData
import tech.figure.servicing.v1beta1.ServicingRightsOuterClass.ServicingRights
import tech.figure.validation.v1beta2.LoanValidation

/**
 * Denotes the string literals used in [Record] annotations for the [LoanScopeSpecification] and its contracts.
 */
object LoanScopeFacts {
    const val asset = "asset"
    const val documents = "documents"
    const val servicingRights = "servicingRights"
    const val servicingData = "servicingData"
    @Deprecated(
        message = "This label is for a record no longer supported by the latest validation contracts.",
        replaceWith = ReplaceWith("LoanScopeFacts.loanValidationMetadata", "io.provenance.scope.loan.LoanScopeFacts"),
        level = DeprecationLevel.WARNING,
    )
    const val loanValidations = "loanValidations"
    const val loanValidationMetadata = "loanValidation"
    const val eNote = "eNote"
}

/**
 * Denotes string literals used to access specific properties within a loan scope's records, such as a key for a [protobuf Any]
 * [com.google.protobuf.Any] value.
 */
object LoanScopeProperties {
    /**
     * Denotes the key that an [Asset][tech.figure.asset.v1beta1.Asset]'s `kvMap` should use to store a [Loan][tech.figure.loan.v1beta1.Loan].
     */
    const val assetLoanKey = "loan"
    /**
     * Denotes the key that an [Asset][tech.figure.asset.v1beta1.Asset]'s `kvMap` should use to store a [MISMOLoan]
     * [tech.figure.loan.v1beta1.MISMOLoanMetadata].
     */
    const val assetMismoKey = "mismoLoan"
    /**
     * Denotes the key that a [LoanDocuments][tech.figure.loan.v1beta1.LoanDocuments] instance's `metadataKvMap` should use to store a
     * [DocumentRecordingGuidance][io.dartinc.registry.v1beta1.DocumentRecordingGuidance] pertaining to servicing documents.
     */
    const val servicingDocumentsKey = "servicingDocuments"
}

/**
 * Denotes the string literals used in [io.provenance.scope.contract.annotations.Input] annotations for
 * [LoanScopeSpecification] contract functions where the input type differs from the output type.
 */
object LoanScopeInputs {
    const val validationRequest = "newRequest"
    const val validationResponse = "resultSubmission"
    const val eNoteUpdate = "newENote"
    const val eNoteControllerUpdate = "newController"
    const val newLoanStates = "newLoanStates"
}

/**
 * Denotes the [Record]s that are part of a [LoanScopeSpecification] for the loan package.
 */
data class LoanPackage(
    /** The loan asset. */
    @Record(LoanScopeFacts.asset) var asset: Asset,
    /** The servicing rights to the loan. Defaults to the lender. */
    @Record(LoanScopeFacts.servicingRights) var servicingRights: ServicingRights,
    /** A list of metadata for documents, including their URIs in an encrypted object store. */
    @Record(LoanScopeFacts.documents) var documents: LoanDocuments,
    /** Servicing data for the loan, including a list of metadata on loan states. */
    @Record(LoanScopeFacts.servicingData) var servicingData: ServicingData,
    /** A list of third-party validation iterations. */
    @Record(LoanScopeFacts.loanValidationMetadata) var loanValidations: LoanValidation,
    /** The eNote for the loan. */
    @Record(LoanScopeFacts.eNote) var eNote: ENote,
)

/**
 * The scope specification definition for a [LoanPackage].
 */
@ScopeSpecificationDefinition(
    uuid = "c370d852-0f3b-4f70-85e6-25983ac07c0f",
    name = "tech.figure.loan",
    description = "An extensible loan scope",
    partiesInvolved = [Specifications.PartyType.OWNER],
)
open class LoanScopeSpecification : P8eScopeSpecification()
