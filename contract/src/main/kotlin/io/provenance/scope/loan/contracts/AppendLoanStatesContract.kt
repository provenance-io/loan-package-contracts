package io.provenance.scope.loan.contracts

import io.provenance.scope.contract.annotations.Function
import io.provenance.scope.contract.annotations.Input
import io.provenance.scope.contract.annotations.Participants
import io.provenance.scope.contract.annotations.Record
import io.provenance.scope.contract.annotations.ScopeSpecification
import io.provenance.scope.contract.proto.Specifications.PartyType
import io.provenance.scope.contract.spec.P8eContract
import io.provenance.scope.loan.LoanScopeFacts
import io.provenance.scope.loan.utility.LoanPackageContract
import io.provenance.scope.loan.utility.isValid
import io.provenance.scope.loan.utility.orError
import io.provenance.scope.loan.utility.requireThat
import io.provenance.scope.loan.utility.validateRequirements
import tech.figure.servicing.v1beta1.LoanStateOuterClass.LoanStates

@Participants(roles = [PartyType.OWNER])
@ScopeSpecification(["tech.figure.loan"])
@LoanPackageContract("AppendLoanStatesContract")
open class AppendLoanStatesContract(
    @Record(LoanScopeFacts.loanStates) val existingLoanStates: LoanStates,
) : P8eContract() {

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.loanStates)
    open fun appendLoanStates(@Input(LoanScopeFacts.loanStates) newLoanStates: LoanStates): LoanStates {
        val newLoanStateList = LoanStates.newBuilder().mergeFrom(existingLoanStates)
        validateRequirements {
            for (state in newLoanStates.loanStateList) {
                requireThat(
                    state.effectiveTime.isValid()      orError "Invalid effective time",
                    state.servicerName.isNotBlank()    orError "Missing servicer name",
                    state.totalUnpaidPrinBal.isValid() orError "Invalid total unpaid principal balance",
                    state.accruedInterest.isValid()    orError "Invalid accrued interest",
                    state.dailyIntAmount.isValid()     orError "Invalid daily interest amount",
                )
                if (existingLoanStates.loanStateList.none { it.effectiveTime == state.effectiveTime }) {
                    newLoanStateList.addLoanState(state)
                }
            }
        }
        return newLoanStateList.build()
    }
}
