package com.thousandlabs.soulking.game

import com.thousandlabs.soulking.data.Prefs
import com.thousandlabs.soulking.data.Track

enum class AnswerMode(val label: String) {
    QCM("4 propositions"),
    ORAL("À l'oral"),
}

/** Ordre figé : la position est enregistrée dans les réglages. */
enum class GuessTarget(val label: String) {
    TITLE("Le titre"),
    /** Toujours le chanteur ou le groupe. */
    ARTIST("L'artiste"),
    BOTH("Titre et artiste"),
    /** L'animé ou le dessin animé d'où vient le morceau. */
    SHOW("L'animé"),
}

/** Ordre figé : la position est enregistrée dans les réglages. */
enum class StartMode(val label: String) {
    HOOK("Le moment le plus connu"),
    RANDOM("Au hasard"),
    BEGINNING("Depuis le début"),
}

data class GameConfig(
    val rounds: Int = 15,
    val clipSec: Int = 5,
    val answer: AnswerMode = AnswerMode.QCM,
    val target: GuessTarget = GuessTarget.BOTH,
    val start: StartMode = StartMode.HOOK,
    val includeLocal: Boolean = true,
    val sounds: Boolean = true,
    val vibrations: Boolean = true,
) {
    fun save() {
        Prefs.putInt("cfg_rounds", rounds)
        Prefs.putInt("cfg_clip", clipSec)
        Prefs.putInt("cfg_answer", answer.ordinal)
        Prefs.putInt("cfg_target", target.ordinal)
        Prefs.putInt("cfg_start", start.ordinal)
        Prefs.putBool("cfg_local", includeLocal)
        Prefs.putBool("cfg_sounds", sounds)
        Prefs.putBool("cfg_vibrations", vibrations)
    }

    companion object {
        val ROUND_CHOICES = listOf(10, 15, 20, 30)
        val CLIP_CHOICES = listOf(2, 5, 10, 15)

        fun load(): GameConfig = GameConfig(
            rounds = Prefs.getInt("cfg_rounds", 15).takeIf { it in ROUND_CHOICES } ?: 15,
            clipSec = Prefs.getInt("cfg_clip", 5).takeIf { it in CLIP_CHOICES } ?: 5,
            answer = AnswerMode.entries.getOrElse(Prefs.getInt("cfg_answer", 0)) { AnswerMode.QCM },
            target = GuessTarget.entries.getOrElse(Prefs.getInt("cfg_target", 2)) { GuessTarget.BOTH },
            start = StartMode.entries.getOrElse(Prefs.getInt("cfg_start", 0)) { StartMode.HOOK },
            includeLocal = Prefs.getBool("cfg_local", true),
            sounds = Prefs.getBool("cfg_sounds", true),
            vibrations = Prefs.getBool("cfg_vibrations", true),
        )
    }
}

data class PlayerState(
    val name: String,
    val score: Int = 0,
    val correct: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val fastestMs: Long? = null,
)

data class Choice(val label: String, val sub: String?, val correct: Boolean)

enum class Stage { LOADING, ERROR, COUNTDOWN, PLAYING, CLIP_OVER, ANSWERING, REVEAL }

/** Résultat d'une manche. */
data class Outcome(val winner: Int?, val points: Int, val timeMs: Long?)

data class GameUi(
    val stage: Stage = Stage.LOADING,
    val loadingText: String = "Préparation…",
    val error: String? = null,
    val sourceName: String = "",
    val answerMode: AnswerMode = AnswerMode.QCM,
    val target: GuessTarget = GuessTarget.BOTH,
    /** « l'animé » ou « le dessin animé », selon le thème. */
    val showWord: String = "l'animé",
    val players: List<PlayerState> = emptyList(),
    val roundIndex: Int = 0,
    val roundCount: Int = 0,
    val track: Track? = null,
    val choices: List<Choice> = emptyList(),
    /** Propositions déjà tentées à tort pendant la manche. */
    val eliminated: Set<Int> = emptySet(),
    /** Joueurs qui se sont trompés : ils ne peuvent plus buzzer pendant la manche. */
    val locked: Set<Int> = emptySet(),
    val answering: Int? = null,
    val replayUsed: Boolean = false,
    val countdown: Int = 3,
    val progress: Float = 0f,
    val outcome: Outcome? = null,
    val showAnswer: Boolean = false,
    /** Le moment le plus connu de ce morceau a été marqué. */
    val hookMarked: Boolean = false,
    /** Image de l'animé ou du dessin animé, si elle a été trouvée. */
    val showImage: String? = null,
    /** Le morceau en cours vient d'être écarté. */
    val banned: Boolean = false,
    /** Message bref affiché pendant la manche. */
    val flash: String? = null,
    /** Information sur la partie entière. */
    val notice: String? = null,
) {
    val solo: Boolean get() = players.size == 1
    val isLastRound: Boolean get() = roundIndex + 1 >= roundCount

    val prompt: String
        get() = when (target) {
            GuessTarget.TITLE -> "Trouvez le titre"
            GuessTarget.ARTIST -> "Trouvez l'artiste"
            GuessTarget.BOTH -> "Trouvez le titre et l'artiste"
            GuessTarget.SHOW -> "Trouvez $showWord"
        }
}
