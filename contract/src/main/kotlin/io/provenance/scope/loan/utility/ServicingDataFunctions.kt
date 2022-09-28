package io.provenance.scope.loan.utility

import io.provenance.scope.util.toOffsetDateTime
import tech.figure.servicing.v1beta1.LoanStateOuterClass.LoanStateMetadata
import tech.figure.servicing.v1beta1.LoanStateOuterClass.ServicingData
import tech.figure.util.v1beta1.DocumentMetadata

/**
 * Updates the [existingServicingData] record with [newServicingData] which only has a field set if it is allowed to update the field
 * in the calling contract.
 */
internal fun ContractEnforcementContext.updateServicingData(
    existingServicingData: ServicingData = ServicingData.getDefaultInstance(),
    newServicingData: ServicingData,
    expectLoanStates: Boolean = false,
): ServicingData = existingServicingData.toBuilder().also { newServicingDataBuilder ->
    newServicingData.takeIf { data -> data.isSet() }?.also { setNewServicingData ->
        appendLoanStates(newServicingDataBuilder, setNewServicingData.loanStateList, expectLoanStates)
        appendServicingDocuments(newServicingDataBuilder, setNewServicingData.docMetaList)
        // At the moment, we won't perform any basic data validation of the top-level fields that aren't the loan state or document lists
        setNewServicingData.loanId.takeIf { it.isSet() }?.let { newLoanId ->
            newServicingDataBuilder.loanId = newLoanId
        }
        setNewServicingData.assetType.takeIf { it.isSet() }?.let { newAssetType ->
            newServicingDataBuilder.assetType = newAssetType
        }
        setNewServicingData.currentBorrowerInfo.takeIf { it.isSet() }?.let { newCurrentBorrowerInfo ->
            newServicingDataBuilder.currentBorrowerInfo = newCurrentBorrowerInfo
        }
        setNewServicingData.originalNoteAmount.takeIf { it.isSet() }?.let { newOriginalNoteAmount ->
            newServicingDataBuilder.originalNoteAmount = newOriginalNoteAmount
        }
    } ?: raiseError("Servicing data is not set")
}.build()

internal fun ContractEnforcementContext.appendLoanStates(
    servicingDataBuilder: ServicingData.Builder,
    newLoanStates: Collection<LoanStateMetadata>,
    expectLoanStates: Boolean,
) {
    /* Primitive types used for protobuf keys to avoid comparison interference from unknown fields */
    val existingStateChecksums = mutableMapOf<String, Boolean>()
    val existingStateIds = mutableMapOf<String, Boolean>()
    val existingStateTimes = mutableMapOf<Pair<Long, Int>, Boolean>()
    if (newLoanStates.isNotEmpty()) {
        servicingDataBuilder.loanStateList.forEach { loanState ->
            loanState?.checksum?.checksum?.takeIf { it.isNotBlank() }?.let { checksum ->
                existingStateChecksums[checksum] = true
            }
            loanState?.id.takeIf { it.isValid() }?.value?.let { id ->
                existingStateIds[id] = true
            }
            loanState?.effectiveTime.takeIf { it.isValid() }?.let { effectiveTime ->
                existingStateTimes[effectiveTime.seconds to effectiveTime.nanos] = true
            }
        }
    } else if (expectLoanStates) {
        raiseError("Must supply at least one loan state")
    }
    val incomingStateChecksums = mutableMapOf<String, Boolean>()
    val incomingStateIds = mutableMapOf<String, Boolean>()
    val incomingStateTimes = mutableMapOf<Pair<Long, Int>, Boolean>()
    for (state in newLoanStates) {
        /* Validate the input data */
        loanStateValidation(state)
        /* Validate each property constrained as unique against the existing data and incoming data */
        state.checksum.checksum.takeIf { it.isNotBlank() }?.let { checksum ->
            requireThat(!existingStateChecksums.getOrDefault(checksum, false) orError "Loan state with checksum $checksum already exists")
            requireThat(
                !incomingStateChecksums.getOrDefault(checksum, false) orError "Loan state with checksum $checksum is provided more than once in input"
            )
            incomingStateChecksums[checksum] = true
        }
        state.id.value.takeIf { it.isNotBlank() }?.let { id ->
            requireThat(!existingStateIds.getOrDefault(id, false) orError "Loan state with ID $id already exists")
            requireThat(
                !incomingStateIds.getOrDefault(id, false) orError "Loan state with ID $id is provided more than once in input"
            )
            incomingStateIds[id] = true
        }
        state.effectiveTime.takeIf { it.isValid() }?.let { effectiveTime ->
            requireThat(
                !existingStateTimes.getOrDefault(effectiveTime.seconds to effectiveTime.nanos, false)
                    orError "Loan state with effective time ${effectiveTime.toOffsetDateTime()} already exists"
            )
            requireThat(
                !incomingStateTimes.getOrDefault(effectiveTime.seconds to effectiveTime.nanos, false)
                    orError "Loan state with effective time ${effectiveTime.toOffsetDateTime()} is provided more than once in input"
            )
            incomingStateTimes[effectiveTime.seconds to effectiveTime.nanos] = true
        }
        /* Append new data - if a violation was found, the eventual thrown exception prevents the changes from persisting */
        servicingDataBuilder.addLoanState(state)
    }
}

internal fun ContractEnforcementContext.appendServicingDocuments(
    servicingDataBuilder: ServicingData.Builder,
    newDocuments: Collection<DocumentMetadata>,
) {
    val existingDocumentMetadata = servicingDataBuilder.docMetaList.toChecksumMap()
    val incomingDocumentChecksums = mutableMapOf<String, Boolean>()
    for (newDocument in newDocuments) {
        documentValidation(newDocument)
        newDocument.checksum.checksum.takeIf { it.isNotBlank() }?.let { newDocChecksum ->
            if (incomingDocumentChecksums[newDocChecksum] == true) {
                raiseError("Loan document with checksum $newDocChecksum is provided more than once in input")
            }
            existingDocumentMetadata[newDocChecksum]?.let { existingDocument ->
                documentModificationValidation(
                    existingDocument,
                    newDocument,
                )
            }
            incomingDocumentChecksums[newDocChecksum] = true
            /* Append new data - if a violation was found, the eventual thrown exception prevents the changes from persisting */
            servicingDataBuilder.addDocMeta(newDocument)
        }
    }
}
