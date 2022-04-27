package io.provenance.scope.loan.utility

import com.google.protobuf.Message as ProtobufMessage
import com.google.protobuf.Timestamp as ProtobufTimestamp
import java.util.UUID as JavaUUID
import tech.figure.util.v1beta1.Checksum as FigureTechChecksum
import tech.figure.util.v1beta1.Date as FigureTechDate
import tech.figure.util.v1beta1.Money as FigureTechMoney
import tech.figure.util.v1beta1.UUID as FigureTechUUID

@Suppress("TooGenericExceptionCaught")
private fun tryOrFalse(fn: () -> Any): Boolean =
    try {
        fn()
        true
    } catch (e: Exception) {
        false
    }

internal fun ProtobufMessage?.isSet() = this !== null && this != this.defaultInstanceForType

internal fun ProtobufTimestamp?.isValid() = isSet()

internal fun FigureTechDate?.isValid() = isSet() && this!!.value.isNotBlank()

internal fun FigureTechChecksum?.isValid() = isSet() && this!!.checksum.isNotBlank()

internal fun FigureTechUUID?.isValid() = isSet() && this!!.value.isNotBlank() && tryOrFalse {
    JavaUUID.fromString(this.value)
}

internal fun FigureTechMoney?.isValid() = isSet()
