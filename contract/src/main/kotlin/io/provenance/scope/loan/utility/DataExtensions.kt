package io.provenance.scope.loan.utility

import com.google.protobuf.Message
import com.google.protobuf.Timestamp
import io.provenance.scope.util.toInstant
import tech.figure.util.v1beta1.Checksum
import tech.figure.util.v1beta1.Date
import tech.figure.util.v1beta1.Money
import tech.figure.util.v1beta1.UUID
import java.time.Instant

internal fun Message?.isSet() = this !== null && this != this.defaultInstanceForType

internal fun Timestamp?.isValid() = isSet() && this!!.toInstant() < Instant.now()

internal fun Date?.isValid() = isSet() && this!!.value.isNotBlank()

internal fun Checksum?.isValid() = isSet() && this!!.checksum.isNotBlank()

internal fun UUID?.isValid() = isSet() && this!!.value.isNotBlank()

internal fun Money?.isValid() = isSet() && this!!.amount > 0
