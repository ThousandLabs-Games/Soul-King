package com.thousandlabs.soulking.game

import android.app.Application
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.thousandlabs.soulking.data.Deezer
import com.thousandlabs.soulking.data.LocalMusic
import com.thousandlabs.soulking.data.PoolBuilder
import com.thousandlabs.soulking.data.Prefs
import com.thousandlabs.soulking.data.Source
import com.thousandlabs.soulking.data.Track
import com.thousandlabs.soulking.data.norm
import com.thousandlabs.soulking.data.showLike
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

enum class Screen { HOME, LOCAL, SEARCH, SETUP, GAME, PODIUM, HOWTO, ABOUT }

data class SearchUi(
    val query: String = "",
    val loading: Boolean = false,
    val artists: List<Deezer.ArtistInfo> = emptyList(),
    val playlists: List<Deezer.PlaylistInfo> = emptyList(),
    val error: String? = null,
)

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx get() = getApplication<Application>()

    // ------------------------------------------------------------------ navigation

    private val _screen = MutableStateFlow(Screen.HOME)
    val screen: StateFlow<Screen> = _screen.asStateFlow()
    private val history = ArrayDeque<Screen>()

    fun open(s: Screen) {
        if (_screen.value == s) return
        history.addLast(_screen.value)
        _screen.value = s
    }

    fun back() {
        if (_screen.value == Screen.GAME) {
            quitGame(); return
        }
        _screen.value = history.removeLastOrNull() ?: Screen.HOME
    }

    fun home() {
        history.clear()
        _screen.value = Screen.HOME
    }

    // ------------------------------------------------------------------ musique du téléphone

    private val _local = MutableStateFlow<List<Track>?>(null)
    val local: StateFlow<List<Track>?> = _local.asStateFlow()
    private var localJob: Job? = null

    fun hasAudioPermission(): Boolean = LocalMusic.granted(ctx)

    fun loadLocal(force: Boolean = false) {
        if (localJob?.isActive == true) return
        if (_local.value != null && !force) return
        localJob = viewModelScope.launch {
            _local.value = withContext(Dispatchers.IO) { runCatching { LocalMusic.load(ctx) }.getOrDefault(emptyList()) }
        }
    }

    fun localMatches(source: Source?): Int {
        val list = _local.value ?: return 0
        return if (source is Source.Theme) list.count { PoolBuilder.matchesLocal(source.def, it) } else 0
    }

    // ------------------------------------------------------------------ recherche

    private val _search = MutableStateFlow(SearchUi())
    val search: StateFlow<SearchUi> = _search.asStateFlow()
    private var searchJob: Job? = null

    fun search(q: String) {
        val query = q.trim()
        if (query.isEmpty()) return
        open(Screen.SEARCH)
        _search.value = SearchUi(query = query, loading = true)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                val (artists, playlists) = withContext(Dispatchers.IO) {
                    Deezer.searchArtists(query, 4) to Deezer.searchPlaylists(query, 10)
                }
                _search.value = SearchUi(query, false, artists, playlists.filter { it.count >= 8 }.take(8))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _search.value = SearchUi(query, false, error = "Recherche impossible. Vérifiez votre connexion Internet.")
            }
        }
    }

    // ------------------------------------------------------------------ préparation de partie

    private val _source = MutableStateFlow<Source?>(null)
    val source: StateFlow<Source?> = _source.asStateFlow()

    private val _config = MutableStateFlow(GameConfig.load())
    val config: StateFlow<GameConfig> = _config.asStateFlow()

    private val _playerNames = MutableStateFlow(Prefs.playerNames())
    val playerNames: StateFlow<List<String>> = _playerNames.asStateFlow()

    fun choose(s: Source) {
        _source.value = s
        if (s is Source.Theme && _config.value.includeLocal && hasAudioPermission()) loadLocal()
        open(Screen.SETUP)
    }

    fun updateConfig(f: (GameConfig) -> GameConfig) {
        val c = f(_config.value)
        _config.value = c
        c.save()
    }

    fun setPlayerName(i: Int, name: String) {
        _playerNames.update { list -> list.toMutableList().also { if (i in it.indices) it[i] = name.take(16) } }
    }

    fun addPlayer() {
        _playerNames.update { if (it.size >= 4) it else it + "Joueur ${it.size + 1}" }
    }

    fun removePlayer(i: Int) {
        _playerNames.update { if (it.size <= 1 || i !in it.indices) it else it.toMutableList().also { l -> l.removeAt(i) } }
    }

    // ------------------------------------------------------------------ partie

    private val _game = MutableStateFlow(GameUi())
    val game: StateFlow<GameUi> = _game.asStateFlow()

    private var gameCfg = GameConfig()
    private var pool: List<Track> = emptyList()
    private var rounds: MutableList<Track> = mutableListOf()
    private val spares = ArrayDeque<Track>()

    private var gameJob: Job? = null
    private var roundJob: Job? = null
    private var tickJob: Job? = null

    private var clipStart = 0L
    private var clipMs = 5_000L
    private var clipOverAt = 0L
    private var buzzTime = 0L
    private var wasOver = false
    private var refreshed = false

    private var _player: ExoPlayer? = null
    private val player: ExoPlayer
        get() = _player ?: ExoPlayer.Builder(ctx)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                true,
            )
            .build()
            .also {
                it.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) = onAudioError()
                })
                _player = it
            }

    fun startGame() {
        val src = _source.value ?: return
        val cfg = _config.value
        gameCfg = cfg
        Sfx.enabled = cfg.sounds
        val names = _playerNames.value.mapIndexed { i, n -> n.trim().ifBlank { "Joueur ${i + 1}" } }
        _playerNames.value = names
        Prefs.savePlayerNames(names)

        cancelJobs()
        stopAudio()
        _game.value = GameUi(
            stage = Stage.LOADING,
            sourceName = src.name,
            answerMode = cfg.answer,
            target = cfg.target,
            players = names.map { PlayerState(it) },
        )
        when (_screen.value) {
            Screen.GAME -> Unit
            Screen.PODIUM -> {
                history.clear(); history.addLast(Screen.HOME); history.addLast(Screen.SETUP)
                _screen.value = Screen.GAME
            }
            else -> open(Screen.GAME)
        }

        gameJob = viewModelScope.launch {
            try {
                val useLocal = src is Source.Local || (src is Source.Theme && cfg.includeLocal && hasAudioPermission())
                val locals = if (!useLocal) emptyList() else _local.value
                    ?: withContext(Dispatchers.IO) { LocalMusic.load(ctx) }.also { _local.value = it }

                val built = PoolBuilder.build(src, locals, useLocal, cfg.rounds) { msg ->
                    _game.update { it.copy(loadingText = msg) }
                }
                if (built.size < 4) {
                    fail(
                        if (src is Source.Local) "Il faut au moins 4 morceaux pour lancer une partie."
                        else "Pas assez de morceaux trouvés. Vérifiez votre connexion Internet ou essayez une autre recherche."
                    )
                    return@launch
                }
                pool = built

                // les morceaux jamais joués d'abord
                val recent = Prefs.recent()
                val (fresh, old) = built.partition { it.key !in recent }
                val ordered = fresh.shuffled() + old.shuffled()
                val count = minOf(cfg.rounds, ordered.size)
                rounds = ordered.take(count).toMutableList()
                spares.clear()
                spares.addAll(ordered.drop(count))

                // choix de ce qu'on devine, selon ce que contient vraiment la partie
                val themeDef = (src as? Source.Theme)?.def
                val distinctArtists = built.map { norm(it.artist) }.distinct().size
                val distinctShows = built.mapNotNull { it.anime?.let(::norm) }.distinct().size
                val showsKnown = built.count { it.anime != null } * 2 >= built.size
                val wanted = when {
                    themeDef?.whoOnly == true -> GuessTarget.SHOW
                    cfg.target == GuessTarget.SHOW && !(src.showLike && showsKnown) -> GuessTarget.ARTIST
                    else -> cfg.target
                }
                val qcm = cfg.answer == AnswerMode.QCM
                val target = when {
                    // en 4 propositions, il faut au moins 4 réponses différentes
                    qcm && wanted == GuessTarget.SHOW && distinctShows < 4 -> GuessTarget.TITLE
                    qcm && (wanted == GuessTarget.ARTIST || wanted == GuessTarget.BOTH) && distinctArtists < 4 -> GuessTarget.TITLE
                    else -> wanted
                }
                val showWord = themeDef?.whoWord ?: "l'animé"
                _game.update {
                    it.copy(
                        roundCount = count,
                        target = target,
                        showWord = showWord,
                        notice = if (count < cfg.rounds) "Seulement $count morceaux disponibles : la partie compte $count manches." else null,
                    )
                }
                startRound(0)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail("Impossible de préparer la partie. Vérifiez votre connexion Internet.")
            }
        }
    }

    private fun fail(message: String) {
        _game.update { it.copy(stage = Stage.ERROR, error = message) }
    }

    private fun startRound(i: Int) {
        roundJob?.cancel()
        tickJob?.cancel()
        val track = rounds.getOrNull(i) ?: return finish()
        clipMs = gameCfg.clipSec * 1000L
        clipStart = computeStart(track)
        refreshed = false
        wasOver = false
        _game.update {
            it.copy(
                stage = Stage.COUNTDOWN,
                roundIndex = i,
                track = track,
                choices = makeChoices(track, it.target),
                eliminated = emptySet(),
                locked = emptySet(),
                answering = null,
                replayUsed = false,
                countdown = 3,
                progress = 0f,
                outcome = null,
                showAnswer = false,
                hookMarked = Prefs.hook(track.key) != null,
            )
        }
        prepare(track)
        roundJob = viewModelScope.launch {
            for (n in 3 downTo 1) {
                _game.update { it.copy(countdown = n) }
                Sfx.tick()
                delay(750)
            }
            Sfx.go()
            _game.update { it.copy(stage = Stage.PLAYING, flash = null) }
            player.play()
            startTicker()
        }
    }

    private fun prepare(track: Track) {
        val p = player
        p.stop()
        p.setMediaItem(MediaItem.fromUri(track.playUri))
        p.prepare()
        p.seekTo(clipStart)
        p.playWhenReady = false
    }

    private fun computeStart(t: Track): Long {
        val maxStart = if (t.isLocal) (t.durationMs - clipMs - 500L).coerceAtLeast(0L) else (30_000L - clipMs - 500L).coerceAtLeast(0L)
        return when (gameCfg.start) {
            StartMode.BEGINNING -> 0L
            StartMode.HOOK -> {
                // 1. le passage que vous avez marqué ; 2. en ligne, l'extrait choisi par Deezer (souvent le refrain) ;
                // 3. vos fichiers : estimation selon la durée (version TV d'un opening ou morceau complet)
                val marked = Prefs.hook(t.key)
                when {
                    marked != null -> marked
                    !t.isLocal -> 0L
                    t.durationMs <= 120_000L -> (t.durationMs * 0.42).toLong()
                    else -> (t.durationMs * 0.27).toLong()
                }.coerceIn(0L, maxStart)
            }
            StartMode.RANDOM -> if (t.isLocal) {
                val lo = (t.durationMs * 0.12).toLong()
                val hi = (t.durationMs * 0.70).toLong() - clipMs
                if (hi <= lo) 0L else Random.nextLong(lo, hi)
            } else {
                if (maxStart <= 0L) 0L else Random.nextLong(0L, maxStart)
            }
        }
    }

    /** Pendant la révélation : retient le passage en cours comme le moment le plus connu du morceau. */
    fun markHook() {
        val g = _game.value
        val t = g.track ?: return
        if (g.stage != Stage.REVEAL) return
        // on recule un peu : le temps de réagir en entendant le passage
        val pos = (player.currentPosition - 1_500L).coerceAtLeast(0L)
        Prefs.setHook(t.key, pos)
        _game.update { it.copy(hookMarked = true) }
    }

    fun clearHook() {
        val t = _game.value.track ?: return
        Prefs.clearHook(t.key)
        _game.update { it.copy(hookMarked = false) }
    }

    private fun startTicker() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive) {
                if (_game.value.stage != Stage.PLAYING) break
                val p = player
                val elapsed = (p.currentPosition - clipStart).coerceAtLeast(0L)
                _game.update { it.copy(progress = (elapsed.toFloat() / clipMs).coerceIn(0f, 1f)) }
                if (elapsed >= clipMs || p.playbackState == Player.STATE_ENDED) {
                    p.pause()
                    clipOverAt = SystemClock.elapsedRealtime()
                    wasOver = true
                    _game.update { it.copy(stage = Stage.CLIP_OVER, progress = 1f) }
                    break
                }
                delay(40)
            }
        }
    }

    /** Temps de réaction : durée d'écoute, plus le temps de réflexion une fois l'extrait fini. */
    private fun reactionTime(): Long =
        if (_game.value.stage == Stage.CLIP_OVER) clipMs + (SystemClock.elapsedRealtime() - clipOverAt)
        else (player.currentPosition - clipStart).coerceIn(0L, clipMs)

    private fun makeChoices(track: Track, target: GuessTarget): List<Choice> {
        if (gameCfg.answer != AnswerMode.QCM) return emptyList()
        fun label(t: Track) = when (target) {
            GuessTarget.ARTIST -> t.artist
            GuessTarget.SHOW -> t.who
            GuessTarget.TITLE, GuessTarget.BOTH -> t.title
        }
        // en « titre et artiste », le chanteur s'affiche sous chaque titre
        fun sub(t: Track) = if (target == GuessTarget.BOTH) t.artist else null
        val seen = hashSetOf(norm(label(track)))
        val wrong = pool.asSequence()
            .filter { it.key != track.key }
            .shuffled()
            .filter { seen.add(norm(label(it))) }
            .take(3)
            .map { Choice(label(it), sub(it), false) }
            .toList()
        return (wrong + Choice(label(track), sub(track), true)).shuffled()
    }

    fun buzz(p: Int) {
        val g = _game.value
        if (g.stage != Stage.PLAYING && g.stage != Stage.CLIP_OVER) return
        if (p in g.locked || p !in g.players.indices) return
        buzzTime = reactionTime()
        tickJob?.cancel()
        player.pause()
        Sfx.buzz()
        vibrate(45)
        _game.update { it.copy(stage = Stage.ANSWERING, answering = p, showAnswer = false, flash = null) }
    }

    fun pick(index: Int) {
        val g = _game.value
        val choice = g.choices.getOrNull(index) ?: return
        if (index in g.eliminated) return
        val p: Int
        val time: Long
        when (g.stage) {
            Stage.ANSWERING -> {
                p = g.answering ?: return
                time = buzzTime
            }
            Stage.PLAYING, Stage.CLIP_OVER -> {
                if (!g.solo) return
                p = 0
                time = reactionTime()
                tickJob?.cancel()
                player.pause()
            }
            else -> return
        }
        if (choice.correct) award(p, time) else miss(p, index)
    }

    fun oralVerdict(ok: Boolean) {
        val g = _game.value
        if (g.stage != Stage.ANSWERING) return
        val p = g.answering ?: return
        if (ok) award(p, buzzTime) else miss(p, null)
    }

    fun toggleAnswer() {
        _game.update { it.copy(showAnswer = !it.showAnswer) }
    }

    private fun award(p: Int, time: Long) {
        val g = _game.value
        // 100 points pour la bonne réponse, jusqu'à 100 de bonus pour la rapidité
        val ratio = time.toFloat() / (clipMs * 2f)
        var pts = 100 + (100 * (1f - ratio)).toInt().coerceIn(0, 100)
        if (g.replayUsed) pts /= 2
        val players = g.players.mapIndexed { i, s ->
            if (i == p) {
                val streak = s.streak + 1
                s.copy(
                    score = s.score + pts,
                    correct = s.correct + 1,
                    streak = streak,
                    bestStreak = maxOf(s.bestStreak, streak),
                    fastestMs = minOf(s.fastestMs ?: Long.MAX_VALUE, time),
                )
            } else s.copy(streak = 0)
        }
        Sfx.success()
        vibrate(30)
        reveal(players, Outcome(p, pts, time))
    }

    private fun miss(p: Int, choiceIndex: Int?) {
        val g = _game.value
        Sfx.error()
        vibrate(220)
        val players = g.players.mapIndexed { i, s ->
            if (i == p) s.copy(score = if (g.solo) s.score else s.score - PENALTY, streak = 0) else s
        }
        val locked = g.locked + p
        val eliminated = if (choiceIndex != null) g.eliminated + choiceIndex else g.eliminated
        val remaining = g.players.indices.count { it !in locked }
        val choicesLeft = g.choices.isEmpty() || g.choices.indices.count { it !in eliminated } > 1
        if (g.solo || remaining == 0 || !choicesLeft) {
            reveal(players.map { it.copy(streak = 0) }, Outcome(null, 0, null))
            return
        }
        val resume = if (wasOver) Stage.CLIP_OVER else Stage.PLAYING
        _game.update {
            it.copy(
                players = players,
                locked = locked,
                eliminated = eliminated,
                answering = null,
                stage = resume,
                flash = "${g.players[p].name} se trompe : −$PENALTY",
            )
        }
        if (resume == Stage.PLAYING) {
            player.play()
            startTicker()
        }
    }

    fun replay() {
        val g = _game.value
        if (g.stage != Stage.CLIP_OVER || g.replayUsed) return
        wasOver = false
        player.seekTo(clipStart)
        player.play()
        _game.update { it.copy(stage = Stage.PLAYING, replayUsed = true, progress = 0f, flash = null) }
        startTicker()
    }

    fun giveUp() {
        val g = _game.value
        if (g.stage != Stage.PLAYING && g.stage != Stage.CLIP_OVER) return
        reveal(g.players.map { it.copy(streak = 0) }, Outcome(null, 0, null))
    }

    private fun reveal(players: List<PlayerState>, outcome: Outcome) {
        tickJob?.cancel()
        _game.value.track?.let { Prefs.addRecent(it.key) }
        _game.update { it.copy(stage = Stage.REVEAL, players = players, outcome = outcome, answering = null, flash = null) }
        // le morceau continue pendant la révélation
        player.play()
    }

    fun next() {
        val g = _game.value
        if (g.stage != Stage.REVEAL) return
        val n = g.roundIndex + 1
        if (n < rounds.size) startRound(n) else finish()
    }

    private fun finish() {
        cancelJobs()
        stopAudio()
        history.clear()
        history.addLast(Screen.HOME)
        _screen.value = Screen.PODIUM
        Sfx.fanfare()
    }

    fun quitGame() {
        cancelJobs()
        stopAudio()
        _screen.value = history.removeLastOrNull() ?: Screen.HOME
    }

    fun retryLoading() = startGame()

    private fun cancelJobs() {
        gameJob?.cancel()
        roundJob?.cancel()
        tickJob?.cancel()
    }

    private fun stopAudio() {
        _player?.let { runCatching { it.stop(); it.clearMediaItems() } }
    }

    /** Extrait illisible : on tente un lien neuf, sinon on remplace le morceau. */
    private fun onAudioError() {
        val g = _game.value
        val track = g.track ?: return
        if (g.stage != Stage.COUNTDOWN && g.stage != Stage.PLAYING && g.stage != Stage.CLIP_OVER) return
        val id = track.deezerId
        if (!refreshed && id != null) {
            refreshed = true
            viewModelScope.launch {
                val fresh = withContext(Dispatchers.IO) { Deezer.track(id) }
                val url = fresh?.previewUrl
                if (url == null) {
                    skipBroken(); return@launch
                }
                val p = player
                p.setMediaItem(MediaItem.fromUri(url))
                p.prepare()
                p.seekTo(clipStart)
                if (_game.value.stage == Stage.PLAYING) p.play()
            }
        } else skipBroken()
    }

    private fun skipBroken() {
        val g = _game.value
        val replacement = spares.removeFirstOrNull()
        if (replacement != null && g.roundIndex in rounds.indices) {
            rounds[g.roundIndex] = replacement
            startRound(g.roundIndex)
            _game.update { it.copy(flash = "Extrait indisponible : morceau remplacé") }
        } else {
            reveal(g.players, Outcome(null, 0, null))
        }
    }

    private fun vibrate(ms: Long) {
        if (!gameCfg.vibrations) return
        runCatching {
            ctx.getSystemService(Vibrator::class.java)
                ?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    override fun onCleared() {
        cancelJobs()
        _player?.release()
        _player = null
        Sfx.release()
    }

    companion object {
        const val PENALTY = 50
    }
}
