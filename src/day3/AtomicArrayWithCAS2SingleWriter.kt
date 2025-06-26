@file:Suppress("DuplicatedCode", "UNCHECKED_CAST")

package day3

import day3.AtomicArrayWithCAS2SingleWriter.Status.*
import java.util.concurrent.atomic.*

// This implementation never stores `null` values.
class AtomicArrayWithCAS2SingleWriter<E : Any>(size: Int, initialValue: E) {
    private val array = AtomicReferenceArray<Any?>(size)

    init {
        // Fill array with the initial value.
        for (i in 0 until size) {
            array[i] = initialValue
        }
    }

    fun get(index: Int): E {
        while (true) {
            val curValue = array.get(index)
            when {
                curValue is AtomicArrayWithCAS2SingleWriter<*>.CAS2Descriptor -> {
                    when (curValue.status.get()) {
                       UNDECIDED, FAILED -> {
                           val prevValue = if (index == curValue.index1) {
                               curValue.expected1
                           } else {
                               curValue.expected2
                           }
                           return prevValue as E
                       }
                       SUCCESS -> {
                           val newValue = if (index == curValue.index1) {
                               curValue.update1
                           } else {
                               curValue.update2
                           }
                           return newValue as E
                       }
                    }
                }
                else -> return curValue as E
            }
        }
    }

    fun cas2(
        index1: Int, expected1: E, update1: E,
        index2: Int, expected2: E, update2: E
    ): Boolean {
        require(index1 != index2) { "The indices should be different" }
        val descriptor = CAS2Descriptor(
            index1 = index1, expected1 = expected1, update1 = update1,
            index2 = index2, expected2 = expected2, update2 = update2
        )
        descriptor.apply()
        return descriptor.status.get() === SUCCESS
    }

    inner class CAS2Descriptor(
        val index1: Int, val expected1: E, val update1: E,
        val index2: Int, val expected2: E, val update2: E
    ) {
        val status = AtomicReference(UNDECIDED)

        fun apply() {
            if (!tryInstallDescriptor()) {
                // if failed to install descriptor - rollback
                array.compareAndSet(index1, this, expected1)
                array.compareAndSet(index2, this, expected2)
                status.compareAndSet(UNDECIDED, FAILED)
                return
            }
            tryUpdateLogically()
            tryUpdatePhysically()
        }

        fun tryInstallDescriptor(): Boolean {
            return array.compareAndSet(index1, expected1, this)
                    && array.compareAndSet(index2, expected2, this)
        }

        private fun tryUpdateLogically() {
            // always succeeds because we own both cells
            status.compareAndSet(UNDECIDED, SUCCESS)
        }

        private fun tryUpdatePhysically() {
            val finalStatus = status.get()
            require(finalStatus !== UNDECIDED)

            val new1 = if (finalStatus == SUCCESS) update1 else expected1
            val new2 = if (finalStatus == SUCCESS) update2 else expected2
            array.compareAndSet(index1, this, new1)
            array.compareAndSet(index2, this, new2)
        }
    }

    enum class Status {
        UNDECIDED, SUCCESS, FAILED
    }
}