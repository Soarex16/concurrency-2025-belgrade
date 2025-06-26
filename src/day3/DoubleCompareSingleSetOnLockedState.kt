@file:Suppress("DuplicatedCode", "UNCHECKED_CAST")

package day3

import java.util.concurrent.atomic.*

// This implementation never stores `null` values.
class DoubleCompareSingleSetOnLockedState<E : Any>(initialValue: E) : DoubleCompareSingleSet<E> {
    private val a = AtomicReference<Any>()
    private val b = AtomicReference<E>()

    init {
        a.set(initialValue)
        b.set(initialValue)
    }

    override fun getA(): E {
        while (true) {
            val curA = a.get() // read state
            if (curA !== LOCKED) return curA as E // handle possible cases (LOCKED or value)
        }
    }

    override fun dcss(
        expectedA: E, updateA: E, expectedB: E
    ): Boolean {
        while (true) {
            val curA = a.get() // read state
            when {
                curA === LOCKED -> continue
                curA === expectedA -> {
                    if (!a.compareAndSet(expectedA, LOCKED)) continue
                    val success = b.get() === expectedB
                    if (success) {
                        a.set(updateA)
                    } else {
                        a.set(expectedA)
                    }
                    return success
                }
                else -> return false
            }
        }
    }

    override fun setB(value: E) {
        b.set(value)
    }

    override fun getB(): E {
        return b.get()
    }
}

// TODO: Store me in `a` to indicate that the reference is "locked".
// TODO: Other operations should wait in an active loop until the
// TODO: value changes.
private val LOCKED = "Locked"
