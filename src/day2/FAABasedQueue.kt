package day2

import day1.*
import java.util.concurrent.atomic.*

class FAABasedQueue<E> : Queue<E> {
    private val infiniteArray = InfiniteArray()
    private val enqIdx = AtomicLong(0)
    private val deqIdx = AtomicLong(0)

    override fun enqueue(element: E) {
        while (true) {
            // фиксируем хвост перед вычислением i потому что иначе
            // хвост может убежать вперед и мы не сможем от него найти
            // ex. index = 1, tail не зафиксировали и он убежал до 5
            // начиная с 5 сегмента мы не сможем найти сегмент 1 - все плохо
            val currentTail = infiniteArray.tail.get()
            requireNotNull(currentTail)
            val i = enqIdx.getAndAdd(1)
            val index = i.toInt() % SEGMENT_SIZE
            val segment = infiniteArray.findOrCreateSegment(currentTail, i / SEGMENT_SIZE)
            infiniteArray.moveTailForward(segment)
            if (segment.compareAndSet(index, null, element)) {
                return
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun dequeue(): E? {
        while (true) {
            val currentHead = infiniteArray.head.get()
            if (!shouldTryToDequeue()) return null
            val i = deqIdx.getAndAdd(1)
            val index = i.toInt() % SEGMENT_SIZE
            val segment = infiniteArray.findOrCreateSegment(currentHead, i / SEGMENT_SIZE)
            infiniteArray.moveHeadForward(segment)
            if (segment.compareAndSet(index, null, POISONED)) {
                continue
            }
            return segment.getAndSet(index, null) as E
        }
    }

    private fun shouldTryToDequeue(): Boolean {
        return deqIdx.get() < enqIdx.get()
    }
}

private class InfiniteArray {
    val head: AtomicReference<Segment>
    val tail: AtomicReference<Segment>

    init {
        val segment = Segment(0)
        head = AtomicReference(segment)
        tail = AtomicReference(segment)
    }

    fun findOrCreateSegment(startSegment: Segment, segmentId: Long): Segment {
        require(startSegment.id <= segmentId)
        var current = startSegment
        while (true) {
            val next = current.next.get()
            if (next != null && current.id < segmentId) {
                current = next
                continue
            }

            // either next is null, or current.id == segmentId
            if (current.id == segmentId) return current
            val newSegment = Segment(current.id + 1)

            if (current.next.compareAndSet(null, newSegment)) {
                return newSegment
            }
        }
    }

    fun moveHeadForward(segment: Segment) {
        if (segment.id > head.get().id) {
            head.compareAndSet(segment, segment.next.get())
        }
    }

    // returns true if there is some free space
    fun moveTailForward(segment: Segment) {
        if (segment.id > tail.get().id) {
            tail.compareAndSet(segment, segment.next.get())
        }
    }
}

// TODO: Use me to construct a linked list of segments.
private class Segment(val id: Long) : AtomicReferenceArray<Any?>(SEGMENT_SIZE) {
    val next = AtomicReference<Segment?>(null)
}

private val POISONED = Any()

// DO NOT CHANGE THIS CONSTANT
private const val SEGMENT_SIZE = 2
