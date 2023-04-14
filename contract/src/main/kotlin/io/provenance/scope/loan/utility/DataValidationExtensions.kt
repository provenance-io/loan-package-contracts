package io.provenance.scope.loan.utility

import io.provenance.scope.util.toInstant
import java.time.Instant
import com.google.protobuf.Message as ProtobufMessage
import com.google.protobuf.Timestamp as ProtobufTimestamp
import java.time.LocalDate as JavaLocalDate
import java.util.UUID as JavaUUID
import tech.figure.util.v1beta1.Address as FigureTechAddress
import tech.figure.util.v1beta1.Checksum as FigureTechChecksum
import tech.figure.util.v1beta1.Date as FigureTechDate
import tech.figure.util.v1beta1.DocumentMetadata as FigureTechDocumentMetadata
import tech.figure.util.v1beta1.ElectronicSignature as FigureTechElectronicSignature
import tech.figure.util.v1beta1.Money as FigureTechMoney
import tech.figure.util.v1beta1.UUID as FigureTechUUID

@Suppress("TooGenericExceptionCaught")
internal fun <T> tryOrFalse(fn: () -> T): Boolean =
    try {
        fn()
        true
    } catch (ignored: Exception) {
        false
    }

@Suppress("TooGenericExceptionCaught")
internal fun falseIfError(fn: () -> Boolean): Boolean =
    try {
        fn()
    } catch (ignored: Exception) {
        false
    }

internal fun ProtobufMessage?.isSet() = this !== null && this != defaultInstanceForType

internal fun ProtobufMessage?.isNotSet() = !isSet()

internal fun ProtobufTimestamp?.isValid() = this !== null

internal fun ProtobufTimestamp?.isValidAndNotInFuture() = this !== null && toInstant() <= Instant.now()

internal fun ProtobufTimestamp?.isValidFundingTime() = this !== null && this != defaultInstanceForType && toInstant() <= Instant.now()

internal fun ProtobufTimestamp?.isValidForLoanState() =
    this !== null &&
        this != defaultInstanceForType && // TODO: This simply checks != epoch, may want to impose higher lower bound like closing/signing date
        toInstant() <= Instant.now() // "prevent servicers from accidentally recording loan tapes that are future dated"

internal fun FigureTechDate?.isValid() = isSet() && this!!.value.isNotBlank() && tryOrFalse { JavaLocalDate.parse(value) }

internal fun FigureTechDate?.isValidForSignedDate() = isSet() && this!!.value.isNotBlank() && falseIfError {
    JavaLocalDate.parse(value) <= JavaLocalDate.now()
}

internal fun FigureTechUUID?.isValid() = isSet() && this!!.value.isNotBlank() && tryOrFalse { JavaUUID.fromString(value) }

internal fun FigureTechAddress?.isValid() = this !== null && street.isNotBlank() && city.isNotBlank() && state.isNotBlank() && zip.isNotBlank()

private val validProvenanceAddress = Regex("^(pb|tp)1.{38}.*")

internal fun String?.isValidProvenanceAddress() = this !== null && matches(validProvenanceAddress)

internal fun EnforcementContext.checksumValidation(
    parentDescription: String = "Input",
    checksum: FigureTechChecksum?
) =
    checksum.takeIf { it.isSet() }?.let { setChecksum ->
        requireThat(
            setChecksum.checksum.isNotBlank()  orError "$parentDescription must have a valid checksum string",
            setChecksum.algorithm.isNotBlank() orError "$parentDescription must specify a checksum algorithm",
        )
    } ?: raiseError("$parentDescription's checksum is not set")

private val validMoneyValue = Regex("^-?([0-9]+(?:[.][0-9]+)?|\\.[0-9]+)$")

internal fun EnforcementContext.moneyValidation(
    parentDescription: String = "Input's money",
    money: FigureTechMoney?
) =
    money.takeIf { it.isSet() }?.let { setMoney ->
        requireThat(
            setMoney.value.matches(validMoneyValue) orError "$parentDescription must have a valid value",
            (setMoney.currency.length == 3 && setMoney.currency.all { character -> character.isLetter() })
                orError "$parentDescription must have a 3-letter ISO 4217 currency",
        )
    } ?: raiseError("$parentDescription is not set")

/**
 * Performs validation to prevent a new document from changing specific fields of an existing document with the same checksum.
 */
internal fun EnforcementContext.documentModificationValidation(
    existingDocument: FigureTechDocumentMetadata,
    newDocument: FigureTechDocumentMetadata,
) =
    existingDocument.checksum.checksum.let { existingChecksum ->
        if (existingChecksum == newDocument.checksum.checksum) {
            val checksumSnippet = if (existingChecksum.isNotBlank()) {
                " with checksum $existingChecksum"
            } else {
                ""
            }
            requireThat(
                existingDocument.checksum.algorithm.let { existingAlgorithm ->
                    (
                        existingAlgorithm == newDocument.checksum.algorithm || existingAlgorithm.isNullOrBlank()
                        ) orError "Cannot change checksum algorithm of existing document$checksumSnippet"
                },
                (existingDocument.id == newDocument.id || existingDocument.id.value.isNullOrBlank())
                    orError "Cannot change ID of existing document$checksumSnippet",
                (existingDocument.uri == newDocument.uri || existingDocument.uri.isNullOrBlank())
                    orError "Cannot change URI of existing document$checksumSnippet",
                (existingDocument.contentType == newDocument.contentType || existingDocument.contentType.isNullOrBlank())
                    orError "Cannot change content type of existing document$checksumSnippet",
                (existingDocument.documentType == newDocument.documentType || existingDocument.documentType.isNullOrBlank())
                    orError "Cannot change document type of existing document$checksumSnippet",
            )
        }
    }

internal val documentValidation: ContractEnforcementContext.(FigureTechDocumentMetadata) -> Unit = { document ->
    document.takeIf { it.isSet() }?.also { setDocument ->
        val documentIdSnippet = if (setDocument.id.isSet()) {
            " with ID ${setDocument.id.value}"
        } else {
            ""
        }
        requireThat(
            setDocument.id.isValid()              orError "Document must have valid ID",
            setDocument.uri.isNotBlank()          orError "Document$documentIdSnippet is missing URI",
            setDocument.contentType.isNotBlank()  orError "Document$documentIdSnippet is missing content type",
            setDocument.documentType.isNotBlank() orError "Document$documentIdSnippet is missing document type",
            setDocument.fileName.isNotBlank()     orError "Document$documentIdSnippet is missing file name",
        )
        checksumValidation("Document$documentIdSnippet", setDocument.checksum)
        if (setDocument.hasSignature() && setDocument.signature.isSet()) {
            electronicSignatureValidation(setDocument.signature)
        }
    } ?: raiseError("Document is not set")
}

internal val electronicSignatureValidation: ContractEnforcementContext.(FigureTechElectronicSignature) -> Unit = { signature ->
    checksumValidation("Document's eSignature", signature.checksum)
    // TODO: What else?
}
