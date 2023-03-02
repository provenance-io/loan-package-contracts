package io.provenance.scope.loan.utility

import tech.figure.loan.v1beta1.Funding
import tech.figure.util.v1beta1.ACH

internal fun Funding?.isSet() = this !== null && (status.isSet() || started.isSet() || completed.isSet() || disbursementsCount > 0)

internal val fundingValidation: ContractEnforcementContext.(Funding) -> Unit = { funding ->
    if (funding.isSet()) {
        funding.status.takeIf { it.isSet() }?.also { fundingStatus ->
            requireThat(
                fundingStatus.status.isNotBlank()                   orError "Funding status must not be blank",
                fundingStatus.effectiveTime.isValidForFundingTime() orError "Funding status must have valid effective time",
            )
        } ?: raiseError("Funding status must be set")
        requireThat(
            funding.started.isValidForFundingTime()   orError "Funding start time must be valid",
            funding.completed.isValidForFundingTime() orError "Funding end time must be valid",
        )
        if (funding.disbursementsCount > 0) {
            val incomingDisbursementIds = mutableMapOf<String, UInt>()
            funding.disbursementsList.requireThatEach { disbursement ->
                disbursement.takeIf { it.isSet() }?.also { setDisbursement ->
                    setDisbursement.id.takeIf { it.isSet() }?.value?.also { disbursementId ->
                        incomingDisbursementIds[disbursementId] = incomingDisbursementIds.getOrDefault(disbursementId, 0U) + 1U
                    }
                    requireThat(
                        setDisbursement.id.isValid()                      orError "Disbursement must have valid ID",
                        setDisbursement.started.isValidForFundingTime()   orError "Disbursement start time must be valid",
                        setDisbursement.completed.isValidForFundingTime() orError "Disbursement end time must be valid",
                        (setDisbursement.amount.value.toDoubleOrNull()?.let { it >= 0 } ?: true)
                            orError "Disbursement amount must not be negative",
                    )
                    setDisbursement.status.takeIf { it.isSet() }?.also { disbursementStatus ->
                        requireThat(
                            (disbursementStatus.status in listOf("UNFUNDED", "INITIATED", "COMPLETED", "CANCELLED")) // TODO: Confirm if desired
                                orError "Disbursement status must be valid",
                            disbursementStatus.effectiveTime.isValidForFundingTime() orError "Disbursement must have valid effective time",
                        )
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
                                    (moneyMovement.wire.isNotSet() || moneyMovement.wire.accountAddress.isSet())
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
                                provenanceAccount.address.matches(Regex("^(pb|tp)1.{38}.*"))
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
