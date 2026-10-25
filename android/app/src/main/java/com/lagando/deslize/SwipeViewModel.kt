package com.lagando.deslize

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.lagando.deslize.data.AppDatabase
import com.lagando.deslize.data.MediaRepository
import com.lagando.deslize.model.MediaItem
import com.lagando.deslize.model.MediaStatus
import com.lagando.deslize.model.UndoAction
import kotlinx.coroutines.launch

/**
 * ViewModel para gerenciar o estado da tela de Swipe e desacoplar a lógica de negócio do Fragment.
 * Preserva itens, índice atual, filtros e histórico de Undo através de rotações de tela.
 */
class SwipeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(
        application,
        AppDatabase.getDatabase(application).mediaDao()
    )

    private val _mediaItems = MutableLiveData<MutableList<MediaItem>>(mutableListOf())
    val mediaItems: LiveData<MutableList<MediaItem>> = _mediaItems

    private val _currentIndex = MutableLiveData(0)
    val currentIndex: LiveData<Int> = _currentIndex

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRandomMode = MutableLiveData(false)
    val isRandomMode: LiveData<Boolean> = _isRandomMode

    private val _isSortBySizeMode = MutableLiveData(false)
    val isSortBySizeMode: LiveData<Boolean> = _isSortBySizeMode

    private val _lastAction = MutableLiveData<UndoAction?>(null)
    val lastAction: LiveData<UndoAction?> = _lastAction

    private val _selectedBucket = MutableLiveData<String?>(null)
    val selectedBucket: LiveData<String?> = _selectedBucket

    private val _availableBuckets = MutableLiveData<List<String>>(emptyList())
    val availableBuckets: LiveData<List<String>> = _availableBuckets

    private var originalUnsortedList = listOf<MediaItem>()

    fun loadMedia(forceRefresh: Boolean = false) {
        // Se já carregou mídias e não é um refresh explícito, mantém o estado (ex: rotação de tela)
        if (!forceRefresh && _mediaItems.value?.isNotEmpty() == true) {
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val items = repository.getUnreviewedMedia(_selectedBucket.value).toMutableList()
                applyCurrentSort(items)
                originalUnsortedList = items.toList()
                _mediaItems.value = items
                _currentIndex.value = 0
                _lastAction.value = null

                // Atualiza lista de pastas disponíveis
                _availableBuckets.value = repository.getAvailableBuckets()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setBucketFilter(bucket: String?) {
        if (_selectedBucket.value == bucket) return
        _selectedBucket.value = bucket
        loadMedia(forceRefresh = true)
    }

    fun toggleRandomMode() {
        val newRandomState = !(_isRandomMode.value ?: false)
        _isRandomMode.value = newRandomState

        if (newRandomState) {
            _isSortBySizeMode.value = false
        }

        val currentList = _mediaItems.value ?: return
        val currentIdx = _currentIndex.value ?: 0
        if (currentIdx >= currentList.size) return

        val currentItem = currentList[currentIdx]
        val remaining = currentList.subList(currentIdx, currentList.size).toMutableList()

        if (newRandomState) {
            remaining.shuffle()
            // Garante que, se houver mais de 1 item, a foto visível mude imediatamente
            if (remaining.size > 1 && remaining[0].id == currentItem.id) {
                val swapIdx = kotlin.random.Random.nextInt(1, remaining.size)
                val temp = remaining[0]
                remaining[0] = remaining[swapIdx]
                remaining[swapIdx] = temp
            }
        } else {
            // Restaura ordem por data (mais recente primeiro)
            remaining.sortByDescending { it.dateAdded }
        }

        val newList = currentList.subList(0, currentIdx).toMutableList()
        newList.addAll(remaining)
        _mediaItems.value = newList
    }

    fun toggleSortBySizeMode() {
        val newSortState = !(_isSortBySizeMode.value ?: false)
        _isSortBySizeMode.value = newSortState

        if (newSortState) {
            _isRandomMode.value = false
        }

        val currentList = _mediaItems.value ?: return
        val currentIdx = _currentIndex.value ?: 0
        if (currentIdx >= currentList.size) return

        val remaining = currentList.subList(currentIdx, currentList.size).toMutableList()

        if (newSortState) {
            // Maior tamanho primeiro
            remaining.sortByDescending { it.size }
        } else {
            // Mais recente primeiro
            remaining.sortByDescending { it.dateAdded }
        }

        val newList = currentList.subList(0, currentIdx).toMutableList()
        newList.addAll(remaining)
        _mediaItems.value = newList
    }

    fun markCurrentAsDeleted(onCompleted: () -> Unit) {
        val list = _mediaItems.value ?: return
        val index = _currentIndex.value ?: 0
        if (index >= list.size) return

        val item = list[index]
        viewModelScope.launch {
            repository.markAsTrashed(item)
            _lastAction.value = UndoAction(item, MediaStatus.TRASHED)
            list.removeAt(index)
            _mediaItems.value = list
            onCompleted()
        }
    }

    fun markCurrentAsKept(onCompleted: () -> Unit) {
        val list = _mediaItems.value ?: return
        val index = _currentIndex.value ?: 0
        if (index >= list.size) return

        val item = list[index]
        viewModelScope.launch {
            repository.markAsKept(item)
            _lastAction.value = UndoAction(item, MediaStatus.KEPT)
            list.removeAt(index)
            _mediaItems.value = list
            onCompleted()
        }
    }

    fun performUndo(onSuccess: (MediaItem, String) -> Unit) {
        val action = _lastAction.value ?: return
        viewModelScope.launch {
            repository.deleteReviewedRecord(action.item.id)
            val list = _mediaItems.value ?: mutableListOf()
            val index = _currentIndex.value ?: 0
            list.add(index, action.item)
            _mediaItems.value = list
            _lastAction.value = null
            onSuccess(action.item, action.status)
        }
    }

    fun skipCurrent(onCompleted: () -> Unit) {
        val list = _mediaItems.value ?: return
        val index = _currentIndex.value ?: 0
        if (index >= list.size) return

        // Move o item para o final da fila
        val item = list.removeAt(index)
        list.add(item)
        _mediaItems.value = list
        onCompleted()
    }

    private fun applyCurrentSort(items: MutableList<MediaItem>) {
        when {
            _isSortBySizeMode.value == true -> items.sortByDescending { it.size }
            _isRandomMode.value == true -> items.shuffle()
            else -> items.sortByDescending { it.dateAdded }
        }
    }
}
