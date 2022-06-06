package io.provenance.scope.loan.utility

import io.provenance.scope.util.toOffsetDateTime
import tech.figure.servicing.v1beta1.LoanStateOuterClass.LoanStateMetadata
import tech.figure.servicing.v1beta1.LoanStateOuterClass.ServicingData
import tech.figure.util.v1beta1.DocumentMetadata

internal fun updateServicingData(
    existingServicingData: ServicingData = ServicingData.getDefaultInstance(),
    newServicingData: ServicingData,
): ServicingData = existingServicingData.toBuilder().also { servicingDataBuilder ->
    validateRequirements(ContractRequirementType.VALID_INPUT) {
        // TODO: Validate any other top-level fields of newServicingData?
        // TODO: Which top-level fields of the servicing data record other than the loan states should be updatable? Does it vary between contracts?
        newServicingData.takeIf { it.isSet() }?.let {
            servicingDataBuilder.docMetaList.forEach { document ->
                documentValidation(document)
            }
            appendLoanStates(servicingDataBuilder, newServicingData.loanStateList)
            appendServicingDocuments(servicingDataBuilder, newServicingData.docMetaList)
        } ?: raiseError("Servicing data is not set")
    }
}.build()

internal fun ContractEnforcementContext.appendLoanStates(
    servicingDataBuilder: ServicingData.Builder,
    newLoanStates: Collection<LoanStateMetadata>,
) {
    /* Primitive types used for protobuf keys to avoid comparison interference from unknown fields */
    val existingStateChecksums = mutableMapOf<String, Boolean>()
    val existingStateIds = mutableMapOf<String, Boolean>()
    val existingStateTimes = mutableMapOf<Pair<Long, Int>, Boolean>()
    if (newLoanStates.isNotEmpty()) {
        servicingDataBuilder.loanStateList.forEach { loanState ->
            loanState?.checksum.takeIf { it.isValid() }?.checksum?.let { checksum ->
                existingStateChecksums[checksum] = true
            }
            loanState?.id.takeIf { it.isValid() }?.value?.let { id ->
                existingStateIds[id] = true
            }
            loanState?.effectiveTime.takeIf { it.isValid() }?.let { effectiveTime ->
                existingStateTimes[effectiveTime.seconds to effectiveTime.nanos] = true
            }
        }
    } else {
        raiseError("Must supply at least one loan state")
    }
    val incomingStateChecksums = mutableMapOf<String, Boolean>()
    val incomingStateIds = mutableMapOf<String, Boolean>()
    val incomingStateTimes = mutableMapOf<Pair<Long, Int>, Boolean>()
    for (state in newLoanStates) {
        /* Validate the input data */
        loanStateValidation(state)
        /* Validate each property constrained as unique against the existing data and incoming data */
        state.checksum?.checksum?.let { checksum ->
            requireThat(!existingStateChecksums.getOrDefault(checksum, false) orError "Loan state with checksum $checksum already exists")
            requireThat(
                !incomingStateChecksums.getOrDefault(checksum, false) orError "Loan state with checksum $checksum is provided more than once in input"
            )
            if (checksum.isNotBlank()) {
                incomingStateChecksums[checksum] = true
            }
        }
        state.id?.value?.let { id ->
            requireThat(!existingStateIds.getOrDefault(id, false) orError "Loan state with ID $id already exists")
            requireThat(
                !incomingStateIds.getOrDefault(id, false) orError "Loan state with ID $id is provided more than once in input"
            )
            if (id.isNotBlank()) {
                incomingStateIds[id] = true
            }
        }
        state.effectiveTime?.let { effectiveTime ->
            requireThat(
                !existingStateTimes.getOrDefault(effectiveTime.seconds to effectiveTime.nanos, false)
                    orError "Loan state with effective time ${effectiveTime.toOffsetDateTime()} already exists"
            )
            requireThat(
                !incomingStateTimes.getOrDefault(effectiveTime.seconds to effectiveTime.nanos, false)
                    orError "Loan state with effective time ${effectiveTime.toOffsetDateTime()} is provided more than once in input"
            )
            if (effectiveTime.isValid()) {
                incomingStateTimes[effectiveTime.seconds to effectiveTime.nanos] = true
            }
        }
        /* Append new data - if a violation was found, the eventual thrown exception prevents the changes from persisting */
        servicingDataBuilder.addLoanState(state)
    }
}

internal fun ContractEnforcementContext.appendServicingDocuments(
    servicingDataBuilder: ServicingData.Builder,
    newDocuments: Collection<DocumentMetadata>,
) {
    val existingDocumentMetadata = mutableMapOf<String, DocumentMetadata>()
    servicingDataBuilder.docMetaList.forEach { documentMetadata ->
        documentMetadata.checksum.takeIf { it.isSet() }?.checksum?.let { checksum ->
            existingDocumentMetadata[checksum] = documentMetadata
        }
    }
    val incomingDocumentChecksums = mutableMapOf<String, Boolean>()
    for (newDocument in newDocuments) {
        documentValidation(newDocument)
        newDocument.checksum.checksum?.let { newDocChecksum ->
            if (incomingDocumentChecksums[newDocChecksum] == true) {
                raiseError("Loan document with checksum $newDocChecksum is provided more than once in input")
            }
            existingDocumentMetadata[newDocChecksum]?.let { existingDocument ->
                documentModificationValidation(
                    existingDocument,
                    newDocument,
                )
            }
            /* Append new data - if a violation was found, the eventual thrown exception prevents the changes from persisting */
            servicingDataBuilder.addDocMeta(newDocument)
        }
    }
}
