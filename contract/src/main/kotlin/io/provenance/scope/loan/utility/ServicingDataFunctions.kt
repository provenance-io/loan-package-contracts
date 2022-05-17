package io.provenance.scope.loan.utility

import io.provenance.scope.util.toOffsetDateTime
import tech.figure.servicing.v1beta1.LoanStateOuterClass.LoanStateMetadata
import tech.figure.servicing.v1beta1.LoanStateOuterClass.ServicingData

/* TODO: Adjust signatures and/or use Kotlin's new context receivers to make this file more digestable */

internal fun updateServicingData(
    existingServicingData: ServicingData = ServicingData.getDefaultInstance(),
    newServicingData: ServicingData,
): ServicingData = existingServicingData.toBuilder().also { servicingDataBuilder ->
    validateRequirements(ContractRequirementType.VALID_INPUT) {
        // TODO: Validate any other top-level fields of newServicingData?
        // TODO: Which top-level fields of the servicing data record other than the loan states should be updatable? Does it vary between contracts?
        servicingDataBuilder.docMetaList.forEach { document ->
            documentValidation(document)
        }
        appendLoanStates(servicingDataBuilder, newServicingData.loanStateList)
    }
}.build()

internal val appendLoanStates: ContractEnforcementContext.(
    ServicingData.Builder,
    Collection<LoanStateMetadata>,
) -> Unit = { servicingDataBuilder, newLoanStates ->
    requireThat(
        newLoanStates.isNotEmpty() orError "Must supply at least one loan state" // TODO: Verify not thrown for empty/optional input
    )
    /* Primitive types used for protobuf keys to avoid comparison interference from unknown fields */
    val existingStateChecksums = mutableMapOf<String, Boolean>()
    val existingStateIds = mutableMapOf<String, Boolean>()
    val existingStateTimes = mutableMapOf<Pair<Long, Int>, Boolean>()
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
                !incomingStateChecksums.getOrDefault(checksum, false) orError "Loan state with checksum $checksum already provided in input"
            )
            if (checksum.isNotBlank()) {
                incomingStateChecksums[checksum] = true
            }
        }
        state.id?.value?.let { id ->
            requireThat(!existingStateIds.getOrDefault(id, false) orError "Loan state with ID $id already exists")
            requireThat(
                !incomingStateIds.getOrDefault(id, false) orError "Loan state with ID $id already provided in input"
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
                    orError "Loan state with effective time ${effectiveTime.toOffsetDateTime()} already provided in input"
            )
            if (effectiveTime.isValid()) {
                incomingStateTimes[effectiveTime.seconds to effectiveTime.nanos] = true
            }
        }
        /* Append new data - if a violation was found, the eventual thrown exception prevents the changes from persisting */
        servicingDataBuilder.addLoanState(state)
    }
}
