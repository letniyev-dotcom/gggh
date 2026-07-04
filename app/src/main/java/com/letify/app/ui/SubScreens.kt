package com.letify.app.ui

import androidx.compose.runtime.Composable
import com.letify.app.ui.screens.AddRoleScreen
import com.letify.app.ui.screens.AnketaDetailScreen
import com.letify.app.ui.screens.AppearanceScreen
import com.letify.app.ui.screens.EditQuestionScreen
import com.letify.app.ui.screens.ListsScreen
import com.letify.app.ui.screens.NotificationsScreen
import com.letify.app.ui.screens.ProfileEditScreen
import com.letify.app.ui.screens.RoleQuestionsScreen
import com.letify.app.ui.screens.RolesScreen
import com.letify.app.ui.screens.SearchScreen
import com.letify.app.ui.screens.StatsScreen
import com.letify.app.ui.state.AnketnicaState
import com.letify.app.ui.state.Question
import com.letify.app.ui.state.Role

/** All possible subscreen routes pushed onto the overlay stack. */
sealed class SubRoute {
    data class AnketaDetail(val anketaId: Int) : SubRoute()
    object Scenarios : SubRoute()
    data class RoleEditor(val roleId: String) : SubRoute()
    object AddRole : SubRoute()
    data class EditQuestion(val roleId: String, val questionId: String?) : SubRoute()
    object Notifications : SubRoute()
    object Appearance : SubRoute()
    object ProfileEdit : SubRoute()
    object Search : SubRoute()
    object Stats : SubRoute()
    object Lists : SubRoute()

    fun stateKey(): String = when (this) {
        is AnketaDetail -> "AnketaDetail:$anketaId"
        Scenarios -> "Scenarios"
        is RoleEditor -> "RoleEditor:$roleId"
        AddRole -> "AddRole"
        is EditQuestion -> "EditQuestion:$roleId:${questionId ?: "new"}"
        Notifications -> "Notifications"
        Appearance -> "Appearance"
        ProfileEdit -> "ProfileEdit"
        Search -> "Search"
        Stats -> "Stats"
        Lists -> "Lists"
    }
}

@Composable
fun RenderSubRoute(
    state: AnketnicaState,
    route: SubRoute,
    push: (SubRoute) -> Unit,
    dismiss: () -> Unit,
    openAnketa: (Int) -> Unit,
) {
    when (route) {
        is SubRoute.AnketaDetail -> {
            // Kept for completeness; detail is normally opened as the top overlay
            // via openAnketa, not pushed onto this stack.
            val a = state.anketa(route.anketaId)
            if (a == null) dismiss() else AnketaDetailScreen(state, a, dismiss)
        }
        SubRoute.Scenarios -> ScreenFrame(header = { SubHeader(title = "Сценарии", onBack = dismiss) }) { topPad ->
            RolesScreen(
                state,
                topPad,
                onRole = { push(SubRoute.RoleEditor(it)) },
                onAddRole = { push(SubRoute.AddRole) },
            )
        }
        is SubRoute.RoleEditor -> {
            val r: Role? = state.role(route.roleId)
            if (r == null) dismiss() else RoleQuestionsScreen(
                state, r,
                onBack = dismiss,
                onEditQuestion = { q -> push(SubRoute.EditQuestion(r.id, q?.id)) },
            )
        }
        is SubRoute.AddRole -> AddRoleScreen(state, onBack = dismiss)
        is SubRoute.EditQuestion -> {
            val r = state.role(route.roleId)
            if (r == null) {
                dismiss()
            } else {
                val q: Question? = route.questionId?.let { qid -> r.questions.firstOrNull { it.id == qid } }
                EditQuestionScreen(state, r, q, onBack = dismiss)
            }
        }
        SubRoute.Notifications -> NotificationsScreen(onBack = dismiss)
        SubRoute.Appearance -> AppearanceScreen(onBack = dismiss, state = state)
        SubRoute.ProfileEdit -> ProfileEditScreen(state, onBack = dismiss)
        SubRoute.Search -> SearchScreen(state, onBack = dismiss, onOpen = { openAnketa(it) })
        SubRoute.Stats -> ScreenFrame(header = { SubHeader(title = "Статистика", onBack = dismiss) }) { topPad ->
            StatsScreen(state, topPad)
        }
        SubRoute.Lists -> ListsScreen(state, onBack = dismiss, onOpen = { openAnketa(it) })
    }
}