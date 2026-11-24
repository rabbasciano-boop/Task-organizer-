package com.example.taskorganizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable
internal fun PageColumn(
    safeBottom: Boolean = false,
    safeTop: Boolean = false,
    keyboardPadding: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)
                .then(if (safeTop) Modifier.statusBarsPadding() else Modifier)
                .then(if (safeBottom) Modifier.navigationBarsPadding() else Modifier)
                .then(if (keyboardPadding) Modifier.imePadding() else Modifier)
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
internal fun LazyPageColumn(safeBottom: Boolean = false, safeTop: Boolean = false, content: LazyListScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)
                .then(if (safeTop) Modifier.statusBarsPadding() else Modifier)
                .then(if (safeBottom) Modifier.navigationBarsPadding() else Modifier)
                .imePadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}
