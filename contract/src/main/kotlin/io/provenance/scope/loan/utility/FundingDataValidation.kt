package io.provenance.scope.loan.utility

import io.provenance.scope.util.toInstant
import tech.figure.loan.v1beta1.Funding
import tech.figure.util.v1beta1.ACH

internal fun Funding?.isSet() = this !== null && (status.isSet() || started.isSet() || completed.isSet() || disbursementsCount > 0)

internal val fundingValidation: ContractEnforcementContext.(Funding) -> Unit = { funding ->
    if (funding.isSet()) {
        funding.status.takeIf { it.isSet() }?.also { fundingStatus ->
            requireThat(
                (fundingStatus.status in listOf("UNFUNDED", "INITIATED", "FUNDED", "CANCELLED"))
                    orError "Funding status must be valid",
                fundingStatus.effectiveTime.isValidFundingTime()
                    orError "Funding status must have valid effective time",
            )
            when (fundingStatus.status) {
                "FUNDED" -> {
                    requireThat(
                        funding.started.isValidFundingTime()   orError "Completed funding's start time must be valid",
                        funding.completed.isValidFundingTime() orError "Completed funding's end time must be valid",
                    )
                    if (funding.started.isValidFundingTime() && funding.completed.isValidFundingTime()) {
                        requireThat(
                            (funding.completed.toInstant() >= funding.started.toInstant()) orError "Funding end time must be after start time",
                        )
                    }
                }
                "CANCELLED" -> {
                    requireThat(
                        (funding.completed.isNotSet() || funding.completed.isValidFundingTime()) orError "Cancelled funding's end time must be valid",
                    )
                }
                !in listOf("UNFUNDED", "UNKNOWN") -> {
                    requireThat(
                        (funding.started.isNotSet() || funding.started.isValidFundingTime()) orError "Funding start time must be valid",
                    )
                }
            }
        } ?: raiseError("Funding status must be set")
        if (funding.disbursementsCount > 0) {
            val incomingDisbursementIds = mutableMapOf<String, UInt>()
            funding.disbursementsList.requireThatEach { disbursement ->
                disbursement.takeIf { it.isSet() }?.also { setDisbursement ->
                    setDisbursement.id.takeIf { it.isSet() }?.value?.also { disbursementId ->
                        incomingDisbursementIds[disbursementId] = incomingDisbursementIds.getOrDefault(disbursementId, 0U) + 1U
                    }
                    requireThat(
                        setDisbursement.id.isValid() orError "Disbursement must have valid ID",
                        (setDisbursement.amount.value.toDoubleOrNull()?.let { it >= 0 } ?: true)
                            orError "Disbursement amount must not be negative",
                    )
                    setDisbursement.status.takeIf { it.isSet() }?.also { disbursementStatus ->
                        requireThat(
                            disbursementStatus.status.isNotBlank() orError "Disbursement status must not be empty",
                            (disbursementStatus.effectiveTime.isNotSet() || disbursementStatus.effectiveTime.isValidFundingTime())
                                orError "Disbursement status must have valid effective time",
                        )
                        when (disbursementStatus.status) {
                            in listOf("COMPLETE", "COMPLETED", "FUNDED") -> {
                                requireThat(
                                    (setDisbursement.started.isNotSet() || setDisbursement.started.isValidFundingTime())
                                        orError "Completed disbursement's start time must be valid",
                                    (setDisbursement.completed.isNotSet() || setDisbursement.completed.isValidFundingTime())
                                        orError "Completed disbursement's end time must be valid",
                                )
                                if (setDisbursement.started.isValidFundingTime() && setDisbursement.completed.isValidFundingTime()) {
                                    requireThat(
                                        (setDisbursement.completed.toInstant() >= setDisbursement.started.toInstant())
                                            orError "Disbursement end time must be after start time",
                                    )
                                }
                            }
                            "CANCELLED" -> {
                                requireThat(
                                    (setDisbursement.completed.isNotSet() || setDisbursement.completed.isValidFundingTime())
                                        orError "Cancelled disbursement's end time must be valid",
                                )
                            }
                            !in listOf("UNFUNDED", "UNKNOWN") -> {
                                requireThat(
                                    (setDisbursement.started.isNotSet() || setDisbursement.started.isValidFundingTime())
                                        orError "Disbursement start time must be valid",
                                )
                            }
                        }
                    } ?: raiseError("Disbursement status must be set")
                    moneyValidation("Disbursement amount", setDisbursement.amount)
                    setDisbursement.disburseAccount.takeIf { it.isSet() }?.also { disburseAccount ->
                        requireThat(
                            disburseAccount.accountOwnerId.isValid() orError "Disbursement account must have valid owner ID",
                            (disburseAccount.financial.isSet() xor disburseAccount.provenance.isSet())
                                orError "Disbursement account must have exactly one specific type",
                        )
                        disburseAccount.financial.takeIf { it.isSet() }?.also { financialAccount ->
                            requireThat(
                                financialAccount.id.isSet()
                                    orError "Disbursement bank account ID must be set",
                                (financialAccount.accountNumber.length in 4..17)
                                    orError "Disbursement bank account must have a valid account number",
                                (financialAccount.routingNumber.length == 9)
                                    orError "Disbursement bank account must have a valid routing number",
                            )
                            financialAccount.movementList.requireThatEach(listIndices = false) { moneyMovement ->
                                requireThat(
                                    (moneyMovement.ach.isSet() xor moneyMovement.wire.isSet()) orError
                                        "Disbursement bank account money movement must have exactly one specific type",
                                    (
                                        moneyMovement.ach.isNotSet() ||
                                            moneyMovement.ach.accountType !in listOf(
                                            ACH.AccountType.ACCOUNT_TYPE_UNKNOWN,
                                            ACH.AccountType.UNRECOGNIZED,
                                        )
                                        ) orError
                                        "Disbursement bank account for ACH must have a valid type",
                                    (moneyMovement.wire.isNotSet() || moneyMovement.wire.accountAddress.isValid())
                                        orError "Disbursement bank account for wires must have a valid address",
                                    (moneyMovement.wire.isNotSet() || moneyMovement.wire.wireInstructions.isNotBlank())
                                        orError "Disbursement bank account for wires must have valid wire instructions",
                                    (
                                        moneyMovement.wire.isNotSet() ||
                                            moneyMovement.wire.swiftInstructions.isNotSet() ||
                                            moneyMovement.wire.swiftInstructions.swiftId.isNotBlank()
                                        ) orError "Disbursement bank account for wires must have valid SWIFT bank account ID",
                                    (
                                        moneyMovement.wire.isNotSet() ||
                                            moneyMovement.wire.swiftInstructions.isNotSet() ||
                                            moneyMovement.wire.swiftInstructions.swiftBankAddress.isSet()
                                        ) orError "Disbursement bank account for wires must have valid SWIFT bank mailing address",
                                )
                            }
                        }
                        disburseAccount.provenance.takeIf { it.isSet() }?.also { provenanceAccount ->
                            requireThat(
                                provenanceAccount.address.isValidProvenanceAddress()
                                    orError "Disbursement account's Provenance address must be valid",
                            )
                        }
                    } ?: raiseError("Disbursement account is not set")
                } ?: raiseError("Disbursement is not set")
            }
            incomingDisbursementIds.entries.forEach { (iterationId, count) ->
                if (count > 1U) {
                    raiseError("Disbursement ID $iterationId is not unique ($count usages)")
                }
            }
        } else {
            raiseError("Must supply at least one disbursement entry")
        }
    } else {
        raiseError("Funding is not set")
    }
}
