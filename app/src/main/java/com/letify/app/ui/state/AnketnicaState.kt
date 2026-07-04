package com.letify.app.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.letify.app.ui.theme.ThemeMode

enum class QType { TEXT, VOICE, QUIZ }

// Anketa review outcome. HOLD = «Доп. рассмотрение» (additional review) — a
// candidate parked for a second look; shown as a BLUE status dot. Like ACC/REJ
// it counts as "processed" (status != NEW), so such anketas leave «Новые анкеты».
enum class AnketaStatus { NEW, ACC, REJ, HOLD }
enum class Tab { Anketki, Roles, Profile }

/**
 * Screen-transition animation the user picks in Оформление → Анимация.
 * Consumed 1:1 by the Letify motion engine (OverlayHost / RoundedSlideOverlay).
 *  - [Push]  — «Сдвиг»: both screens move as one strip at the same speed.
 *  - [Cover] — «Наплыв»: the screen underneath shifts a little while the new
 *              one slides over it with rounded leading corners.
 */
enum class TransitionStyle(val key: String) { Push("push"), Cover("cover") }

/** A question in a role's application form. Editable, so backed by state. */
class Question(
    val id: String,
    type: QType,
    text: String,
    options: List<String> = listOf("", ""),
    correct: Int = 0,
) {
    var type by mutableStateOf(type)
    var text by mutableStateOf(text)
    val options = mutableStateListOf<String>().also { it.addAll(options) }
    var correct by mutableStateOf(correct)
}

/** A candidate's answer to one question (read-only demo data). */
class Answer(
    val q: String,
    val type: QType,
    val a: String? = null,
    val dur: String? = null,
    val options: List<String> = emptyList(),
    val correct: Int = -1,
    val chosen: Int = -1,
)

class Anketa(
    val id: Int,
    val name: String,
    val user: String,
    val role: String,
    status: AnketaStatus,
    val time: String,
    val agox: Int,
    val fields: List<Pair<String, String>>,
    val answers: List<Answer>,
) {
    var status by mutableStateOf(status)
}

class Role(
    val id: String,
    name: String,
    color: Long,
    val icon: String,
    questions: List<Question>,
) {
    var name by mutableStateOf(name)
    var colorValue by mutableStateOf(color)
    val questions = mutableStateListOf<Question>().also { it.addAll(questions) }
    val color: Color get() = Color(colorValue)
}

data class AccentOption(val key: String, val color: Color, val name: String)

val ACCENTS = listOf(
    AccentOption("violet", Color(0xFFB084F5), "Violet"),
    AccentOption("blue", Color(0xFF5AA7FF), "Blue"),
    AccentOption("cyan", Color(0xFF45C8E8), "Cyan"),
    AccentOption("teal", Color(0xFF3DBFA8), "Teal"),
    AccentOption("green", Color(0xFF4ECB71), "Green"),
    AccentOption("lime", Color(0xFFB7D858), "Lime"),
    AccentOption("yellow", Color(0xFFEFCF4A), "Yellow"),
    AccentOption("orange", Color(0xFFFF8C5A), "Orange"),
    AccentOption("coral", Color(0xFFF77D6D), "Coral"),
    AccentOption("pink", Color(0xFFFF6B9D), "Pink"),
    AccentOption("magenta", Color(0xFFE57AC9), "Magenta"),
)

val AVATAR_GRADIENTS = listOf(
    listOf(Color(0xFFB084F5), Color(0xFF6E86F2)),
    listOf(Color(0xFF4ECB71), Color(0xFF3DBFA8)),
    listOf(Color(0xFFFF8C5A), Color(0xFFF26A5C)),
    listOf(Color(0xFF5AA7FF), Color(0xFF45C8E8)),
    listOf(Color(0xFFFF6B9D), Color(0xFFE57AC9)),
    listOf(Color(0xFFEFCF4A), Color(0xFFFF8C5A)),
)

/** Question type metadata (tile colour + icon + labels), matches prototype. */
data class QTypeMeta(val label: String, val short: String, val desc: String, val color: Color, val icon: String)

val QTYPE_META: Map<QType, QTypeMeta> = mapOf(
    QType.TEXT to QTypeMeta("Текстовой ответ", "Текстовой", "Свободный текст", Color(0xFF3FA8F5), "document-text-bold-duotone"),
    QType.VOICE to QTypeMeta("Голосовой ответ", "Голосовой", "Кандидат записывает аудио", Color(0xFFFF6E97), "music-note-2-bold-duotone"),
    QType.QUIZ to QTypeMeta("Викторина", "Викторина", "Выбор варианта, есть верный", Color(0xFFFFA63D), "list-check-bold-duotone"),
)

class AnketnicaState {
    var themeMode by mutableStateOf(ThemeMode.Dark)
    var currentTab by mutableStateOf(Tab.Anketki)
    var accent by mutableStateOf(ACCENTS[0].color)
    var accentName by mutableStateOf(ACCENTS[0].name)
    var toastMsg by mutableStateOf<String?>(null)

    // ── Motion preferences (read by the Letify motion engine) ──────────────
    // transitionStyle drives OverlayHost/RoundedSlideOverlay push-vs-cover;
    // swipeBackEnabled gates the follow-finger left-edge swipe-back gesture.
    var transitionStyle by mutableStateOf(TransitionStyle.Push)
    var swipeBackEnabled by mutableStateOf(true)

    val roles = mutableStateListOf<Role>()
    val ankety = mutableStateListOf<Anketa>()

    val newCount: Int get() = ankety.count { it.status == AnketaStatus.NEW }
    fun role(id: String): Role? = roles.firstOrNull { it.id == id }
    fun anketa(id: Int): Anketa? = ankety.firstOrNull { it.id == id }
    fun avatarGradient(id: Int): List<Color> = AVATAR_GRADIENTS[(id - 1).mod(AVATAR_GRADIENTS.size)]
    fun toast(msg: String) { toastMsg = msg }
}

private fun seed(s: AnketnicaState) {
    s.roles.addAll(
        listOf(
            Role("admin", "Админ", 0xFFF26A5C, "shield-user-bold-duotone", listOf(
                Question("q1", QType.TEXT, "Почему вы хотите стать админом?"),
                Question("q2", QType.TEXT, "Опишите ваш опыт модерации сообществ."),
                Question("q3", QType.QUIZ, "Что делать при спаме от нового участника?",
                    listOf("Сразу забанить", "Предупредить и удалить спам", "Игнорировать"), 1),
            )),
            Role("mod", "Модератор", 0xFF5AA7FF, "star-bold-duotone", listOf(
                Question("q1", QType.TEXT, "Сколько времени готовы уделять в день?"),
                Question("q2", QType.VOICE, "Расскажите голосом о себе."),
                Question("q3", QType.QUIZ, "Участник нарушил правила впервые. Действия?",
                    listOf("Мут на неделю", "Устное предупреждение", "Бан"), 1),
            )),
            Role("idea", "Идейный вдохновитель", 0xFFB084F5, "magic-stick-3-bold-duotone", listOf(
                Question("q1", QType.TEXT, "Какую идею вы хотите привнести в проект?"),
                Question("q2", QType.VOICE, "Питч вашей идеи голосом."),
            )),
        )
    )
    s.ankety.addAll(listOf(
        Anketa(1, "Артём Волков", "@artvolk", "mod", AnketaStatus.NEW, "12:40", 0,
            listOf("Возраст" to "19", "Город" to "Москва", "Опыт" to "2 года"),
            listOf(
                Answer("Сколько времени готовы уделять в день?", QType.TEXT, a = "2–3 часа каждый день, вечером свободен."),
                Answer("Расскажите голосом о себе.", QType.VOICE, dur = "0:47"),
                Answer("Участник нарушил правила впервые. Действия?", QType.QUIZ,
                    options = listOf("Мут на неделю", "Устное предупреждение", "Бан"), correct = 1, chosen = 1),
            )),
        Anketa(2, "Лиза Кот", "@lizakot", "idea", AnketaStatus.NEW, "11:05", 1,
            listOf("Возраст" to "23", "Город" to "СПб", "Портфолио" to "есть"),
            listOf(
                Answer("Какую идею вы хотите привнести?", QType.TEXT, a = "Еженедельные тематические стримы и система бейджей за активность."),
                Answer("Питч идеи голосом.", QType.VOICE, dur = "1:12"),
            )),
        Anketa(3, "Максим Орлов", "@maxorl", "admin", AnketaStatus.NEW, "Вчера", 1,
            listOf("Возраст" to "27", "Город" to "Казань", "Опыт" to "5 лет"),
            listOf(
                Answer("Почему хотите стать админом?", QType.TEXT, a = "Хочу помогать развивать комьюнити и наводить порядок."),
                Answer("Опыт модерации?", QType.TEXT, a = "Админил Discord на 12k и TG-чат на 5k."),
                Answer("Что делать при спаме?", QType.QUIZ,
                    options = listOf("Сразу забанить", "Предупредить и удалить спам", "Игнорировать"), correct = 1, chosen = 2),
            )),
        Anketa(4, "Соня Ким", "@sonyakim", "mod", AnketaStatus.ACC, "Пн", 3,
            listOf("Возраст" to "21", "Город" to "Новосиб", "Опыт" to "1 год"),
            listOf(
                Answer("Сколько времени готовы уделять?", QType.TEXT, a = "Пару часов вечером, стабильно."),
                Answer("Расскажите голосом о себе.", QType.VOICE, dur = "0:32"),
            )),
        Anketa(5, "Гоша Пуп", "@goshapup", "admin", AnketaStatus.REJ, "Пн", 3,
            listOf("Возраст" to "16", "Город" to "—", "Опыт" to "нет"),
            listOf(
                Answer("Почему админом?", QType.TEXT, a = "хочу банить всех"),
                Answer("Опыт?", QType.TEXT, a = "нет опыта"),
                Answer("Что делать при спаме?", QType.QUIZ,
                    options = listOf("Сразу забанить", "Предупредить и удалить спам", "Игнорировать"), correct = 1, chosen = 0),
            )),
        Anketa(6, "Даша Лунь", "@dashal", "idea", AnketaStatus.NEW, "2 июл", 2,
            listOf("Возраст" to "25", "Город" to "Минск", "Портфолио" to "есть"),
            listOf(
                Answer("Какую идею?", QType.TEXT, a = "Игровые ивенты и коллабы с другими комьюнити."),
                Answer("Питч голосом.", QType.VOICE, dur = "0:55"),
            )),
    ))
}

@Composable
fun rememberAnketnicaState(): AnketnicaState = remember { AnketnicaState().also { seed(it) } }

/**
 * App-wide handle to [AnketnicaState]. The Letify motion engine
 * (LetifyOverlay.kt) reads `LocalAppState.current.transitionStyle` and
 * `.swipeBackEnabled`, so the shell MUST provide this above the content.
 */
val LocalAppState = compositionLocalOf<AnketnicaState> { error("AnketnicaState not provided") }