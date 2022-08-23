package io.provenance.scope.loan.utility

import io.provenance.scope.util.toInstant
import java.time.Instant
import com.google.protobuf.Message as ProtobufMessage
import com.google.protobuf.Timestamp as ProtobufTimestamp
import java.time.LocalDate as JavaLocalDate
import java.util.UUID as JavaUUID
import tech.figure.util.v1beta1.Checksum as FigureTechChecksum
import tech.figure.util.v1beta1.Date as FigureTechDate
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

internal fun ProtobufTimestamp?.isValidForLoanState() =
    this !== null &&
        this != defaultInstanceForType && // TODO: This simply checks != epoch, may want to impose higher lower bound like closing/signing date
        toInstant() <= Instant.now() // "prevent servicers from accidentally recording loan tapes that are future dated"

internal fun FigureTechDate?.isValid() = isSet() && this!!.value.isNotBlank() && tryOrFalse { JavaLocalDate.parse(value) }

internal fun FigureTechDate?.isValidForSignedDate() = isSet() && this!!.value.isNotBlank() && falseIfError {
    JavaLocalDate.parse(value) <= JavaLocalDate.now()
}

internal fun FigureTechUUID?.isValid() = isSet() && this!!.value.isNotBlank() && tryOrFalse { JavaUUID.fromString(value) }

internal fun ContractEnforcementContext.checksumValidation(parentDescription: String = "Input", checksum: FigureTechChecksum?) {
    checksum.takeIf { it.isSet() }?.let { setChecksum ->
        requireThat(
            setChecksum.checksum.isNotBlank()  orError "$parentDescription must have a valid checksum string",
            setChecksum.algorithm.isNotBlank() orError "$parentDescription must specify a checksum algorithm",
        )
    } ?: raiseError("$parentDescription's checksum is not set")
}

internal fun ContractEnforcementContext.moneyValidation(parentDescription: String = "Input's money", money: FigureTechMoney?) {
    money.takeIf { it.isSet() }?.let { setMoney ->
        requireThat(
            setMoney.value.matches(Regex("^[-]?([0-9]+(?:[\\\\.][0-9]+)?|\\\\.[0-9]+)\$")) orError "$parentDescription must have a valid value",
            (setMoney.currency.length == 3 && setMoney.currency.all { character -> character.isLetter() })
                orError "$parentDescription must have a 3-letter ISO 4217 currency",
        )
    } ?: raiseError("$parentDescription is not set")
}
