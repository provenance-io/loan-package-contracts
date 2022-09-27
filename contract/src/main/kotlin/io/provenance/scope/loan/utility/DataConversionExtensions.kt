package io.provenance.scope.loan.utility

import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.Message
import tech.figure.loan.v1beta1.MISMOLoanMetadata
import tech.figure.util.v1beta1.DocumentMetadata
import com.google.protobuf.Any as ProtobufAny
import tech.figure.loan.v1beta1.Loan as FigureTechLoan

internal fun Iterable<DocumentMetadata>.toChecksumMap(): Map<String, DocumentMetadata> = mutableMapOf<String, DocumentMetadata>().also { map ->
    forEach { document ->
        document.checksum.checksum.takeIf { it.isNotBlank() }?.let { checksum ->
            map[checksum] = document
        }
    }
}

context(ContractEnforcementContext)
internal inline fun <reified M : Message, S> ProtobufAny.tryUnpackingAs(inputDescription: String = "input", body: (M) -> S): S? {
    val expectedType = M::class.java
    var unpackedResult: M? = null
    try {
        unpackedResult = unpack(expectedType)
    } catch (suppressed: InvalidProtocolBufferException) {
        /**
         * If the parse fails, we want to yield a [ContractViolation], but we can also try to re-parse the receiver as a different type to potentially
         * better inform the caller of the input violation.
         * However, we must be careful not to supply too many protobufs to try, and especially not provide simple ones like a
         * [tech.figure.util.v1beta1.UUID] that can be formed by a non-unique subset of the receiver's fields, to avoid _mis_informing the caller.
         */
        listOf(
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
    return if (unpackedResult !== null) {
        body(unpackedResult)
    } else {
        null
    }
}

internal inline fun <reified T : Message> ProtobufAny.unpackOrNull(): T? =
    T::class.java.let { clazz ->
        try {
            unpack(clazz)
        } catch (suppressed: InvalidProtocolBufferException) {
            null
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
