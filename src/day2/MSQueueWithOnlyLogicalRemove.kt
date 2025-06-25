package day2

import java.util.concurrent.atomic.*

class MSQueueWithOnlyLogicalRemove<E> : QueueWithRemove<E> {
    private val head: AtomicReference<Node<E>>
    private val tail: AtomicReference<Node<E>>

    init {
        val dummy = Node<E>(null)
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    override fun enqueue(element: E) {
        val newNode = Node(element)
        while (true) {
            val currentTail = tail.get()
            if (currentTail.next.compareAndSet(null, newNode)) {
                // success, update tail
                tail.compareAndSet(currentTail, newNode)
                return
            } else {
                // fail, fix the tail and repeat
                tail.compareAndSet(currentTail, currentTail.next.get()) // TODO: is it correct?
            }
        }
    }

    override fun dequeue(): E? {
        while (true) {
            val currentHead = head.get()
            val currentHeadNext = currentHead.next.get() ?: return null
            if (head.compareAndSet(currentHead, currentHeadNext)){
                if (currentHeadNext.markExtractedOrRemoved()) {
                    val value = currentHeadNext.element
                    currentHeadNext.element = null
                    return value
                }
            }
        }
    }

    /**
     * This is an internal function for tests.
     * DO NOT CHANGE THIS CODE.
     */
    override fun validate() {
        // In this version, we allow storing
        // removed elements in the linked list.
        check(tail.get().next.get() == null) {
            "`tail.next` must be `null`"
        }
    }

    override fun remove(element: E): Boolean {
        val currentHead = head.get()
        var currentHeadNext = currentHead.next.get()
        if (currentHeadNext == null) return false
        while (currentHeadNext != null) {
            if (currentHeadNext.element == element) {
                if (currentHeadNext.markExtractedOrRemoved()) {
                    currentHeadNext.element = null
                    return true
                }
            }
            currentHeadNext = currentHeadNext.next.get()
        }
        return false
    }

    private class Node<E>(
        var element: E?
    ) {
        val next = AtomicReference<Node<E>?>(null)

        /**
         * TODO: Both [dequeue] and [remove] should mark
         * TODO: nodes as "extracted or removed".
         */
        private val _extractedOrRemoved = AtomicBoolean(false)
        val extractedOrRemoved
            get() =
                _extractedOrRemoved.get()

        fun markExtractedOrRemoved(): Boolean =
            _extractedOrRemoved.compareAndSet(false, true)

        /**
         * Removes this node from the queue structure.
         * Returns `true` if this node was successfully
         * removed, or `false` if it has already been
         * removed by [remove] or extracted by [dequeue].
         */
        fun remove(): Boolean {
            return markExtractedOrRemoved()
        }
    }
}