package day1

import java.util.concurrent.atomic.AtomicReference

class MSQueue<E> : Queue<E> {
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
        val headNode = head.get()
        val nextNode = headNode.next.get() ?: return null
        head.compareAndSet(headNode, nextNode)
        nextNode.element = null
        return nextNode.element
    }

    // FOR TEST PURPOSE, DO NOT CHANGE IT.
    override fun validate() {
        check(tail.get().next.get() == null) {
            "`tail.next` must be `null`"
        }
        check(head.get().element == null) {
            "`head.element` must be `null`"
        }
    }

    private class Node<E>(
        var element: E?
    ) {
        val next = AtomicReference<Node<E>?>(null)
    }
}
