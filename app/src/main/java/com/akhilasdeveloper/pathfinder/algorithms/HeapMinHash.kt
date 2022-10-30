package com.akhilasdeveloper.pathfinder.algorithms

import com.akhilasdeveloper.pathfinder.models.Square
import com.akhilasdeveloper.pathfinder.models.nodes

class HeapMinHash<T> {
    private val heapMin: MutableList<T> = mutableListOf()
    private var data: HashMap<T, Square> = hashMapOf()

    private fun getLeftChildIndex(parentIndex: Int) = 2 * parentIndex + 1
    private fun getRightChildIndex(parentIndex: Int) = 2 * parentIndex + 2
    private fun getParentIndex(childIndex: Int) = (childIndex - 1) / 2

    private fun hasLeftChild(index: Int) = getLeftChildIndex(index) < heapMin.size
    private fun hasRightChild(index: Int) = getRightChildIndex(index) < heapMin.size
    private fun hasParent(index: Int) = getParentIndex(index) >= 0

    private fun leftChild(index: Int) = heapMin[getLeftChildIndex(index)]
    private fun rightChild(index: Int) = heapMin[getRightChildIndex(index)]
    private fun parent(index: Int) = heapMin[getParentIndex(index)]

    fun clear(){
        heapMin.clear()
        data.clear()
    }

    fun pull(data: HashMap<T, Square>?): T? {
        this.data = data ?: hashMapOf()
        if (heapMin.size == 0) return null
        val item = heapMin[0]
        heapMin[0] = heapMin[heapMin.size - 1]
        heapMin.removeAt(heapMin.size - 1)
        heapDown()
        return item
    }

    fun push(item: T, data: HashMap<T, Square>?) {
        this.data = data ?: hashMapOf()
        heapMin.add(item)
        heapUp()
    }

    private fun heapUp() {
        if (data.isNotEmpty()) {
            var index = heapMin.size - 1
            while (hasParent(index) &&
                getData(parent(index)).distance >
                getData(heapMin[index]).distance
            ) {
                val item = parent(index)
                heapMin[getParentIndex(index)] = heapMin[index]
                heapMin[index] = item
                index = getParentIndex(index)
            }
        }
    }

    private fun getData(index: T) = data.getOrPut(index) { nodes() }

    private fun heapDown() {
        if (data.isNotEmpty()) {
            var index = 0
            while (hasLeftChild(index)) {
                var smallerChildIndex = getLeftChildIndex(index)
                if (hasRightChild(index) && getData(rightChild(index)).distance < getData(leftChild(
                        index
                    )).distance
                ) {
                    smallerChildIndex = getRightChildIndex(index)
                }

                if (getData(heapMin[index]).distance < getData(heapMin[smallerChildIndex]).distance)
                    break
                else {
                    val item = heapMin[index]
                    heapMin[index] = heapMin[smallerChildIndex]
                    heapMin[smallerChildIndex] = item
                }

                index = smallerChildIndex
            }
        }
    }
}