@file:Suppress("FoldInitializerAndIfToElvis", "DuplicatedCode")

package day2

import java.util.concurrent.atomic.*

class MSQueueWithLinearTimeRemove<E> : QueueWithRemove<E> {
    private val head: AtomicReference<Node>
    private val tail: AtomicReference<Node>

    init {
        val dummy = Node(null)
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    override fun enqueue(element: E) {
        val newNode = Node(element)
        while (true) {
            val currentTail = tail.get()
            val success = if (currentTail.next.compareAndSet(null, newNode)) {
                // success, update tail
                tail.compareAndSet(currentTail, newNode)
                true
            } else {
                // fail, fix the tail and repeat
                tail.compareAndSet(currentTail, currentTail.next.get())
                false
            }
            if (currentTail.extractedOrRemoved) {
                currentTail.remove()
            }
            if (success) return
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

    override fun remove(element: E): Boolean {
        // Traverse the linked list, searching the specified
        // element. Try to remove the corresponding node if found.
        // DO NOT CHANGE THIS CODE.
        var node = head.get()
        while (true) {
            val next = node.next.get()
            if (next == null) return false
            node = next
            if (node.element == element && node.remove()) return true
        }
    }

    /**
     * This is an internal function for tests.
     * DO NOT CHANGE THIS CODE.
     */
    override fun validate() {
        check(tail.get().next.get() == null) {
            "tail.next must be null"
        }
        var node = head.get()
        // Traverse the linked list
        while (true) {
            if (node !== head.get() && node !== tail.get()) {
                check(!node.extractedOrRemoved) {
                    "Removed node with element ${node.element} found in the middle of this queue"
                }
            }
            node = node.next.get() ?: break
        }
    }

    // TODO: Node is an inner class for accessing `head` in `remove()`
    private inner class Node(
        var element: E?
    ) {
        val next = AtomicReference<Node?>(null)

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

        fun findPrevious(): Node? {
            var prev = head.get()
            if (prev === this) return null // don't remove the head
            while (true) {
                val next = prev.next.get()
                if (next == null) return null
                if (next === this) return prev
                prev = next
            }
        }

        /**
         * Removes this node from the queue structure.
         * Returns `true` if this node was successfully
         * removed, or `false` if it has already been
         * removed by [remove] or extracted by [dequeue].
         */
        fun remove(): Boolean {
            val removed = markExtractedOrRemoved()
            removePhysically()
            return removed
        }

        fun removePhysically() {
            // physically remove the node
            val currentPrev = findPrevious() ?: return // if there is no prev it's already removed
            val currentNext = next.get()
            if (currentNext !== null) { // don't remove the tail.
                currentPrev.next.set(currentNext)
                if (currentNext.extractedOrRemoved) {
                    currentNext.remove()
                }
            }
        }
    }
}
