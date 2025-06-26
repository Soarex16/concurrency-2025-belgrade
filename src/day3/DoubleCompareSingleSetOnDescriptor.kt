@file:Suppress("DuplicatedCode", "UNCHECKED_CAST")

package day3

import day3.DoubleCompareSingleSetOnDescriptor.Status.*
import java.util.concurrent.atomic.*

// This implementation never stores `null` values.
class DoubleCompareSingleSetOnDescriptor<E : Any>(initialValue: E) : DoubleCompareSingleSet<E> {
    private val a = AtomicReference<Any>()
    private val b = AtomicReference<E>()

    init {
        a.set(initialValue)
        b.set(initialValue)
    }

    override fun getA(): E {
        while (true) {
            val curA = a.get()
            when {
                curA is DoubleCompareSingleSetOnDescriptor<*>.DcssDescriptor -> curA.doApply()
                else -> return curA as E
            }
        }
    }

    override fun dcss(expectedA: E, updateA: E, expectedB: E): Boolean {
        val descriptor = DcssDescriptor(expectedA, updateA, expectedB)
        descriptor.apply()
        return descriptor.status.get() == SUCCESS
    }

    private inner class DcssDescriptor(
        val expectedA: E, val updateA: E, val expectedB: E
    ) {
        val status = AtomicReference(UNDECIDED)

        fun apply() {
            // try install descriptor
            while (true) {
                val curA = a.get()
                when {
                    curA is DoubleCompareSingleSetOnDescriptor<*>.DcssDescriptor -> {
                        // descriptor is already installed (by us or by another one) -> help apply
                        curA.doApply()
                    }
                    // curA === expectedA -> try to install
                    curA === expectedA -> {
                        // if we failed to install -> someone another installed, retry
                        if (!a.compareAndSet(expectedA, this)) continue
                        // we successfully installed the descriptor, handle states
                        doApply()
                        return
                    }
                    // curA !== expectedA -> fail
                    else -> {
                        status.compareAndSet(UNDECIDED, FAILED)
                        return
                    }
                }
            }
        }

        fun doApply() {
            while (true) {
                val curStatus = status.get()
                when (curStatus) {
                    UNDECIDED -> {
                        val success = tryUpdateLogically()
                        if (success) {
                            tryUpdatePhysically()
                            return
                        } else {
                            status.compareAndSet(UNDECIDED, FAILED)
                        }
                    }
                    SUCCESS -> {
                        tryUpdatePhysically()
                        return
                    }
                    FAILED -> {
                        rollback()
                        return
                    }
                }
            }
        }

        private fun rollback() {
            a.compareAndSet(this, expectedA)
        }

        private fun tryUpdateLogically(): Boolean {
            if (b.get() != expectedB) return false
            return status.compareAndSet(UNDECIDED, SUCCESS)
        }

        private fun tryUpdatePhysically() {
            a.compareAndSet(this, updateA)
        }
    }

    override fun setB(value: E) {
        b.set(value)
    }

    override fun getB(): E {
        return b.get()
    }

    enum class Status {
        UNDECIDED, SUCCESS, FAILED
    }
}