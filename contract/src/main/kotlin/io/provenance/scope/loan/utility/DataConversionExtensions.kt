package io.provenance.scope.loan.utility

import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.Any as ProtobufAny
import tech.figure.loan.v1beta1.Loan as FigureTechLoan

internal inline fun <reified T : com.google.protobuf.Message> ProtobufAny.tryUnpackingAs(): T =
    T::class.java.let { clazz ->
        try {
            unpack(clazz)
        } catch (exception: InvalidProtocolBufferException) {
            throw UnexpectedContractStateException("Could not unpack as $clazz", exception)
        }
    }

internal fun ProtobufAny.toLoan() = tryUnpackingAs<FigureTechLoan>()
