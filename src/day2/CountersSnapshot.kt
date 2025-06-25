package day2

import java.util.concurrent.atomic.*

class CountersSnapshot {
    val counter1 = AtomicLong(0)
    val counter2 = AtomicLong(0)
    val counter3 = AtomicLong(0)

    fun incrementCounter1() = counter1.getAndIncrement()
    fun incrementCounter2() = counter2.getAndIncrement()
    fun incrementCounter3() = counter3.getAndIncrement()

    fun countersSnapshot(): Triple<Long, Long, Long> {
        while (true) {
            val currentCounter1 = counter1.get()
            val currentCounter2 = counter2.get()
            if (currentCounter1 != counter1.get()) continue
            // counter 1 is still valid, proceed

            val currentCounter3 = counter3.get()

            if (currentCounter1 != counter1.get()) continue
            // counter 1 is still valid, proceed
            if (currentCounter2 != counter2.get()) continue
            // counter 2 is still valid, proceed
            return Triple(currentCounter1, currentCounter2, currentCounter3)
        }
    }
}