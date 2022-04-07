package io.provenance.contracts

import io.p8e.annotations.Input
import io.p8e.annotations.Participants
import io.p8e.annotations.ScopeSpecification
import io.provenance.LoanScopeFacts
import io.p8e.proto.ContractSpecs.PartyType.OWNER
import io.p8e.spec.P8eContract

@Participants(OWNER)
@ScopeSpecification(["tech.figure.loan"])
open class AppendLoanStatesContract(
    @Record(LoanScopeFacts.loanStates) val existingLoanStates: tech.figure.servicing.v1beta1.LoanStates,
) : P8eContract() {

    @Function(OWNER)
    @Record(LoanScopeFacts.loanStates)
    open fun appendLoanStates(@Input(LoanScopeFacts.loanStates) newLoanStates: tech.figure.servicing.v1beta1.LoanStates) {
        var newLoanStateList = LoanStates.newBuilder().mergeFrom(existingLoanStates)
        for (state in newLoanStates.loanStateList) {
            require(state.effectiveTime.isNotBlank() && state.effectiveTime < Instant.now()) { "Invalid effective time" }
            require(state.servicerName.isNotBlank()) { "Missing servicer name" }
            require(state.totalUnpaidPrinBal.isValid()) { "Invalid total unpaid principle balance" }
            require(state.accruedInterest.isValid()) { "Invalid accrued interest" }
            require(state.dailyIntAmount.isValid()) { "Invalid daily interest amount" }
            if (!existingLoanStates.loanStateList.any({ it.effectiveTime == state.effectiveTime })) {
                newLoanStateList.addLoanState(state)
            }
        }
        return newLoanStateList.build()
    }

}