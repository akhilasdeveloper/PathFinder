package com.akhilasdeveloper.pathfinder.algorithms

import com.akhilasdeveloper.pathfinder.models.Square

class HeapMin {
    private val heapMin: MutableList<Int> = mutableListOf()
    private var data: List<Square>? = null

    private fun getLeftChildIndex(parentIndex: Int) = 2 * parentIndex + 1
    private fun getRightChildIndex(parentIndex: Int) = 2 * parentIndex + 2
    private fun getParentIndex(childIndex: Int) = (childIndex - 1) / 2

    private fun hasLeftChild(index: Int) = getLeftChildIndex(index) < heapMin.size
    private fun hasRightChild(index: Int) = getRightChildIndex(index) < heapMin.size
    private fun hasParent(index: Int) = getParentIndex(index) >= 0

    private fun leftChild(index: Int) = heapMin[getLeftChildIndex(index)]
    private fun rightChild(index: Int) = heapMin[getRightChildIndex(index)]
    private fun parent(index: Int) = heapMin[getParentIndex(index)]

    fun pull(data: List<Square>?):Int?{
        this.data = data
        if (heapMin.size == 0) return null
        val item = heapMin[0]
        heapMin[0] = heapMin[heapMin.size - 1]
        heapMin.removeAt(heapMin.size - 1)
        heapifyDown()
        return item
    }

    fun push(item: Int, data: List<Square>?){
        this.data = data
        heapMin.add(item)
        heapifyUp()
    }

    private fun heapifyUp() {
        data?.let {data->
            var index = heapMin.size-1
            while (hasParent(index) && data[parent(index)].distance > data[heapMin[index]].distance){
                val item = parent(index)
                heapMin[getParentIndex(index)] = heapMin[index]
                heapMin[index] = item
                index = getParentIndex(index)
            }
        }
    }

    private fun heapifyDown() {
        data?.let{data->
            var index = 0
            while (hasLeftChild(index)){
                var smallerChildIndex = getLeftChildIndex(index)
                if (hasRightChild(index) && data[rightChild(index)].distance < data[leftChild(index)].distance){
                    smallerChildIndex = getRightChildIndex(index)
                }

                if (data[heapMin[index]].distance < data[heapMin[smallerChildIndex]].distance)
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