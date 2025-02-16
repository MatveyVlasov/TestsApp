package com.app.testik.presentation.dialog.testinfo.model

import com.app.testik.domain.model.CategoryType

data class TestInfoDialogUIState(
    val id: String = "",
    val category: CategoryType = CategoryType.NOT_SELECTED,
    val image: String = "",
    val title: String = "",
    val author: String = "",
    val authorName: String = "",
    val description: String = "",
    val questionsNum: Int = 0,
    val pointsMax: Int = 0,
    val timeLimit: Long = 0L,
    val isTimeLimitQuestion: Boolean = false,
    val isDemo: Boolean = false,
    val isOpen: Boolean = false,
    val isPasswordEnabled: Boolean = false,
    val password: String = ""
)