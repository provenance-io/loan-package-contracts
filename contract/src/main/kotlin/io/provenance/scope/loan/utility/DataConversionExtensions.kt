package io.provenance.scope.loan.utility

import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.Message
import tech.figure.loan.v1beta1.MISMOLoanMetadata
import com.google.protobuf.Any as ProtobufAny
import tech.figure.loan.v1beta1.Loan as FigureTechLoan

context(ContractEnforcementContext)
internal inline fun <reified T : Message> ProtobufAny.tryUnpackingAs(inputDescription: String = "input", body: (T) -> Any) {
    val expectedType = T::class.java
    var unpackedResult: T? = null
    try {
        unpackedResult = unpack(expectedType)
    } catch (suppressed: InvalidProtocolBufferException) {
        listOf( // List of probable incorrect types to try parsing the input as, to potentially better inform the caller
            FigureTechLoan::class.java,
            MISMOLoanMetadata::class.java,
        ).filterNot { maybeActualType ->
            expectedType == maybeActualType
        }.firstOrNull { maybeActualType ->
            tryOrFalse {
                unpack(maybeActualType)
            }
        }.let { discoveredActualType ->
            if (discoveredActualType === null) {
                "Could not unpack the $inputDescription as $expectedType"
            } else {
                "Expected $inputDescription to be a $expectedType but was actually a $discoveredActualType"
            }
        }.let { violationMessage ->
            raiseError(violationMessage)
        }
    }
    if (unpackedResult !== null) {
        body(unpackedResult)
    }
}

internal inline fun <reified T : Message> ProtobufAny.unpackAs(): T =
    T::class.java.let { clazz ->
        try {
            unpack(clazz)
        } catch (exception: InvalidProtocolBufferException) {
            throw UnexpectedContractStateException("Could not unpack as $clazz", exception)
        }
    }

internal fun ProtobufAny.toFigureTechLoan() = unpackAs<FigureTechLoan>()

internal fun ProtobufAny.toMISMOLoan() = unpackAs<MISMOLoanMetadata>()
