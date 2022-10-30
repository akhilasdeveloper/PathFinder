package com.akhilasdeveloper.pathfinder.views

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.GenerateMaze
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel
@Inject constructor(
    private val findPath: FindPath,
    private val generateMaze: GenerateMaze
): ViewModel() {

    private val _dataStateInfoDialogChange: MutableLiveData<Int> = MutableLiveData()
    val dataStateInfoDialogChange: LiveData<Int>
        get() = _dataStateInfoDialogChange

    fun setInfoDialog(state: Int) {
        _dataStateInfoDialogChange.value = state
    }
}