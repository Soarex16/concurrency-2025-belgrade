package day1

import java.util.concurrent.atomic.*
import kotlin.coroutines.*

// Never stores `null`-s for simplicity.
class RendezvousChannel<E : Any> {
    private val head: AtomicReference<Node>
    private val tail: AtomicReference<Node>

    init {
        val dummy = Node(null, null)
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    suspend fun send(element: E) {
        while (true) {
            // Is this queue empty or contain other senders?
            if (isEmptyOrContainsSenders()) {
                val success = suspendCoroutine<Boolean> { continuation ->
                    val node = Node(element, continuation as Continuation<Any?>)
                    if (!tryAddNode(tail.get(), node)) {
                        // Fail and retry.
                        continuation.resume(false)
                    }
                }
                // Finish on success and retry on failure.
                if (success) return
            } else {
                // The queue contains receivers, try to extract the first one.
                val firstReceiver = tryExtractNode(head.get(), expectingReceiver = true) ?: continue
                firstReceiver.continuation!!.resume(element)
                return
            }
        }
    }

    suspend fun receive(): E {
        while (true) {
            // Is this queue empty or contain other receivers?
            if (isEmptyOrContainsReceivers()) {
                val element = suspendCoroutine<E?> { continuation ->
                    val node = Node(RECEIVER, continuation as Continuation<Any?>)
                    if (!tryAddNode(tail.get(), node)) {
                        // Fail and retry.
                        continuation.resume(null)
                    }
                }
                // Should we retry?
                if (element == null) continue
                // Return the element
                return element
            } else {
                // The queue contains senders, try to extract the first one.
                val firstSender = tryExtractNode(head.get(), expectingReceiver = false) ?: continue
                firstSender.continuation!!.resume(true)
                return firstSender.element as E
            }
        }
    }

    private fun isEmptyOrContainsReceivers(): Boolean {
        return isEmptyOrMatches { it === RECEIVER }
    }

    private fun isEmptyOrContainsSenders(): Boolean {
        return isEmptyOrMatches { it !== RECEIVER }
    }

    private fun isEmptyOrMatches(condition: (Any?) -> Boolean): Boolean {
        val next = firstOrNull() ?: return true
        return condition(next.element)
    }

    private fun firstOrNull(): Node? { // this operation is not atomic!
        return head.get().next.get()
    }

    private fun tryAddNode(curTail: Node, newNode: Node): Boolean {
        val curTailNext = curTail.next.get()
        if (curTailNext != null && tail.compareAndSet(curTail, curTailNext)) {
            return false
        }

        // at this point `tail` points to a real `curTail`
        val isEmpty = firstOrNull() === null
        if (!isEmpty) {
            // if we are trying to add an element with a type different from the type of all elements in the queue,
            // then it means that another operation has completed between the current operation, fail
            when {
                curTail.element === RECEIVER && newNode.element !== RECEIVER -> return false
                curTail.element !== RECEIVER && newNode.element === RECEIVER -> return false
            }
        }

        return if (curTail.next.compareAndSet(null, newNode)) {
            tail.compareAndSet(curTail, newNode)
            true
        } else {
            tail.compareAndSet(curTail, curTail.next.get())
            false
        }
    }

    private fun tryExtractNode(curHead: Node, expectingReceiver: Boolean): Node? {
        val currentHeadNext = curHead.next.get() ?: return null
        when {
            expectingReceiver && currentHeadNext.element !== RECEIVER -> return null
            !expectingReceiver && currentHeadNext.element === RECEIVER -> return null
        }
        if (head.compareAndSet(curHead, currentHeadNext)) {
            return currentHeadNext
        }
        return null
    }

    class Node(
        // Sending element in case of suspended `send()` or
        // RECEIVER in case of suspended `receive()`.
        val element: Any?,
        // Suspended `send` of `receive` request.
        val continuation: Continuation<Any?>?
    ) {
        val next = AtomicReference<Node?>(null)
    }
}

val RECEIVER = "RECEIVER"