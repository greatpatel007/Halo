package com.halo.feature.showroom

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.toRoute
import com.halo.feature.showroom.catalog.ShowroomCatalog
import com.halo.feature.showroom.navigation.ShowroomPageRoute
import com.halo.feature.showroom.ui.ShowroomScaffold
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ShowroomPageViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {
    val pageId: String = savedStateHandle.toRoute<ShowroomPageRoute>().pageId
}

@Composable
fun ShowroomPageScreen(
    onBack: () -> Unit,
    viewModel: ShowroomPageViewModel = hiltViewModel(),
) {
    val page = ShowroomCatalog.find(viewModel.pageId)
    if (page == null) {
        ShowroomScaffold(title = "Not found", summary = "No showroom page \"${viewModel.pageId}\".", onBack = onBack) {}
    } else {
        ShowroomScaffold(title = page.title, summary = page.summary, onBack = onBack) {
            page.content()
        }
    }
}
