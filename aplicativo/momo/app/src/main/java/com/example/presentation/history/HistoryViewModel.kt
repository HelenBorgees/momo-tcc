package com.example.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DiaperChange
import com.example.data.model.DiaperEvent
import com.example.data.model.ObservacaoCuidador
import com.example.data.model.ReportPeriod
import com.example.data.model.TipoEvento
import com.example.data.model.TrocaFralda
import com.example.data.repository.MomoRepository
import com.example.data.repository.MomoRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import com.example.data.model.HistoryPeriodPolicy

enum class HistoryTab(val rotulo: String) {
    CONFIRMED_CHANGES("Trocas de fralda"),
    SENSOR_EVENTS("Detecções do sensor")
}

enum class HistoryCategoryFilter(val label: String) {
    ALL("Todos"),
    URINA("Urina"),
    FEZES("Fezes"),
    DIARREIA("Diarreia"),
    NAO_INFORMADO("Não informado")
}

data class HistoryUiState(
    val selectedTab: HistoryTab = HistoryTab.CONFIRMED_CHANGES,
    val selectedPeriod: ReportPeriod? = ReportPeriod.TODAY,
    val selectedCategory: HistoryCategoryFilter = HistoryCategoryFilter.ALL,
    val confirmedChanges: List<TrocaFralda> = emptyList(),
    val filteredChanges: List<TrocaFralda> = emptyList(),
    val sensorEvents: List<DiaperEvent> = emptyList(),
    val filteredSensorEvents: List<DiaperEvent> = emptyList()
)

class HistoryViewModel(
    private val repository: MomoRepository = MomoRepositoryProvider.getRepository()
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(HistoryTab.CONFIRMED_CHANGES)
    private val _selectedPeriod = MutableStateFlow<ReportPeriod?>(ReportPeriod.TODAY)
    private val _selectedCategory = MutableStateFlow(HistoryCategoryFilter.ALL)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HistoryUiState> = MomoRepositoryProvider.modoAtual.flatMapLatest {
        val active = MomoRepositoryProvider.getRepository()
        combine(
        active.getChanges(),
        active.getEvents(),
        _selectedTab,
        _selectedPeriod,
        _selectedCategory
    ) { changes, events, tab, period, category ->
        val now = System.currentTimeMillis()

        // 1. Filtragem de trocas confirmadas pelo cuidador por data e categoria combinada
        val periodChanges = changes.filter { HistoryPeriodPolicy.includes(period, it.timestampMillis, now) }
        val filteredChanges = periodChanges.filter { change ->
            when (category) {
                HistoryCategoryFilter.ALL -> true
                HistoryCategoryFilter.URINA -> change.observacaoCuidador.hasUrine
                HistoryCategoryFilter.FEZES -> change.observacaoCuidador.hasFeces
                HistoryCategoryFilter.DIARREIA -> change.observacaoCuidador.hasDiarrhea
                HistoryCategoryFilter.NAO_INFORMADO -> change.observacaoCuidador == ObservacaoCuidador.NAO_INFORMADO
            }
        }

        // 2. Filtragem de eventos automáticos do sensor (aba secundária de telemetria científica)
        val periodEvents = events.filter { it.tipo in setOf(TipoEvento.URINA, TipoEvento.FEZES, TipoEvento.DIARREIA) && HistoryPeriodPolicy.includes(period, it.historyTimestamp, now) }.sortedByDescending { it.historyTimestamp }
        val filteredEvents = periodEvents.filter { event ->
            when (category) {
                HistoryCategoryFilter.ALL -> true
                HistoryCategoryFilter.URINA -> event.tipo == TipoEvento.URINA
                HistoryCategoryFilter.FEZES -> event.tipo == TipoEvento.FEZES
                HistoryCategoryFilter.DIARREIA -> event.tipo == TipoEvento.DIARREIA
                HistoryCategoryFilter.NAO_INFORMADO -> false
            }
        }

        HistoryUiState(
            selectedTab = tab,
            selectedPeriod = period,
            selectedCategory = category,
            confirmedChanges = changes,
            filteredChanges = filteredChanges,
            sensorEvents = events,
            filteredSensorEvents = filteredEvents
        )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun setTab(tab: HistoryTab) {
        _selectedTab.value = tab
    }

    fun setPeriod(period: ReportPeriod?) {
        _selectedPeriod.value = period
    }

    fun setCategory(category: HistoryCategoryFilter) {
        _selectedCategory.value = category
    }

    companion object {
        fun calculateCutoffMillis(period: ReportPeriod): Long =
            HistoryPeriodPolicy.start(period, System.currentTimeMillis())
    }
}
