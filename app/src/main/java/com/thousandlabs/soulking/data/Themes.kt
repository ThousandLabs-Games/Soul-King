package com.thousandlabs.soulking.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Sailing
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.ui.graphics.vector.ImageVector

/** Morceau précis choisi à la main : vérifié avant d'entrer dans la partie. */
data class Curated(val title: String, val artist: String, val anime: String? = null)

data class ThemeDef(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    /** Les morceaux les plus connus de chacun de ces artistes. */
    val artists: List<String> = emptyList(),
    /** Recherches de playlists publiques. */
    val playlists: List<String> = emptyList(),
    /** Morceaux précis. */
    val curated: List<Curated> = emptyList(),
    /** Tous les morceaux du thème appartiennent à cet animé. */
    val forceAnime: String? = null,
    /** Morceaux du téléphone qui vont avec : un animé précis… */
    val localAnime: String? = null,
    /** … ou tous les openings du téléphone. */
    val localAnyAnime: Boolean = false,
)

object Themes {

    private val rapUs = ThemeDef(
        id = "rap_us",
        title = "Rap US",
        subtitle = "Eminem, 50 Cent, Kendrick, Drake…",
        icon = Icons.Outlined.Mic,
        artists = listOf(
            "Eminem", "50 Cent", "Dr. Dre", "Snoop Dogg", "Kendrick Lamar", "Drake", "Kanye West", "JAY-Z",
            "Travis Scott", "Lil Wayne", "The Notorious B.I.G.", "2Pac", "J. Cole", "Nicki Minaj",
            "Post Malone", "21 Savage", "Future", "A\$AP Rocky",
        ),
    )

    private val drill = ThemeDef(
        id = "drill",
        title = "Drill",
        subtitle = "UK et US : Pop Smoke, Central Cee…",
        icon = Icons.Outlined.GraphicEq,
        artists = listOf(
            "Pop Smoke", "Central Cee", "Digga D", "Headie One", "Fivio Foreign", "Chief Keef", "King Von",
            "Unknown T", "Russ Millions", "Sheff G", "Kay Flock", "Lil Durk", "Dutchavelli",
        ),
        playlists = listOf("drill hits"),
    )

    private val rnb = ThemeDef(
        id = "rnb_us",
        title = "R&B US",
        subtitle = "Usher, Rihanna, The Weeknd…",
        icon = Icons.Outlined.NightsStay,
        artists = listOf(
            "Usher", "Rihanna", "Chris Brown", "The Weeknd", "Beyoncé", "Ne-Yo", "Alicia Keys", "Bruno Mars",
            "SZA", "Frank Ocean", "Akon", "Mariah Carey", "Destiny's Child", "Khalid", "Bryson Tiller",
            "Summer Walker", "Jhené Aiko", "Brent Faiyaz",
        ),
    )

    private val classics = ThemeDef(
        id = "classics",
        title = "Grands classiques",
        subtitle = "Les tubes que tout le monde connaît",
        icon = Icons.Outlined.AutoAwesome,
        artists = listOf(
            "Michael Jackson", "Queen", "ABBA", "The Beatles", "Bob Marley & The Wailers", "Whitney Houston",
            "Elton John", "Bee Gees", "Prince", "Madonna", "Stevie Wonder", "Earth, Wind & Fire", "a-ha",
            "Toto", "Survivor", "Bon Jovi", "Lionel Richie", "Tina Turner", "Gloria Gaynor", "Eagles",
        ),
    )

    private val worldHits = ThemeDef(
        id = "world_hits",
        title = "Hits mondiaux",
        subtitle = "7 Years, Shape of You, Rolling in the Deep…",
        icon = Icons.Outlined.Public,
        artists = listOf(
            "Lukas Graham", "Ed Sheeran", "Adele", "Justin Bieber", "Dua Lipa", "Coldplay", "Imagine Dragons",
            "Maroon 5", "Taylor Swift", "Billie Eilish", "Sia", "Shakira", "Katy Perry", "Lady Gaga",
            "OneRepublic", "Avicii", "Calvin Harris", "Harry Styles", "Sam Smith", "Pharrell Williams",
        ),
    )

    private val nowHits = ThemeDef(
        id = "now_hits",
        title = "Hits du moment",
        subtitle = "Ce qui tourne en ce moment dans le monde",
        icon = Icons.AutoMirrored.Outlined.TrendingUp,
        playlists = listOf("Top Worldwide", "Global Top 50", "Top Hits"),
    )

    private val animeOpenings = ThemeDef(
        id = "anime",
        title = "Openings cultes",
        subtitle = "Naruto, Attack on Titan, Dragon Ball…",
        icon = Icons.Outlined.LiveTv,
        curated = listOf(
            Curated("We Are!", "Hiroshi Kitadani", "One Piece"),
            Curated("Blue Bird", "Ikimonogakari", "Naruto Shippuden"),
            Curated("Silhouette", "KANA-BOON", "Naruto Shippuden"),
            Curated("Sign", "FLOW", "Naruto Shippuden"),
            Curated("GO!!!", "FLOW", "Naruto"),
            Curated("Haruka Kanata", "ASIAN KUNG-FU GENERATION", "Naruto"),
            Curated("Guren no Yumiya", "Linked Horizon", "L'Attaque des Titans"),
            Curated("Shinzou wo Sasageyo", "Linked Horizon", "L'Attaque des Titans"),
            Curated("The Rumbling", "SiM", "L'Attaque des Titans"),
            Curated("Unravel", "TK from Ling tosite sigure", "Tokyo Ghoul"),
            Curated("Again", "YUI", "Fullmetal Alchemist: Brotherhood"),
            Curated("Cha-La Head-Cha-La", "Hironobu Kageyama", "Dragon Ball Z"),
            Curated("Dragon Soul", "Takayoshi Tanimoto", "Dragon Ball Kai"),
            Curated("Limit Break x Survivor", "Kazuya Yoshii", "Dragon Ball Super"),
            Curated("The World", "Nightmare", "Death Note"),
            Curated("Kaikai Kitan", "Eve", "Jujutsu Kaisen"),
            Curated("Specialz", "King Gnu", "Jujutsu Kaisen"),
            Curated("Gurenge", "LiSA", "Demon Slayer"),
            Curated("Idol", "YOASOBI", "Oshi no Ko"),
            Curated("Kick Back", "Kenshi Yonezu", "Chainsaw Man"),
            Curated("Peace Sign", "Kenshi Yonezu", "My Hero Academia"),
            Curated("Odd Future", "UVERworld", "My Hero Academia"),
            Curated("Hikaru Nara", "Goose house", "Your Lie in April"),
            Curated("Tank!", "SEATBELTS", "Cowboy Bebop"),
            Curated("A Cruel Angel's Thesis", "Yoko Takahashi", "Neon Genesis Evangelion"),
            Curated("Colors", "FLOW", "Code Geass"),
            Curated("Departure!", "Masatoshi Ono", "Hunter x Hunter"),
            Curated("Mixed Nuts", "Official HIGE DANdism", "Spy x Family"),
            Curated("Bling-Bang-Bang-Born", "Creepy Nuts", "Mashle"),
            Curated("Haruka Mirai", "Kankaku Piero", "Black Clover"),
            Curated("Black Catcher", "Vickeblanka", "Black Clover"),
            Curated("Inferno", "Mrs. GREEN APPLE", "Fire Force"),
            Curated("Hacking to the Gate", "Kanako Itou", "Steins;Gate"),
            Curated("Crossing Field", "LiSA", "Sword Art Online"),
            Curated("Asterisk", "ORANGE RANGE", "Bleach"),
            Curated("Ichirin no Hana", "HIGH and MIGHTY COLOR", "Bleach"),
            Curated("Rolling Star", "YUI", "Bleach"),
            Curated("Bloody Stream", "Coda", "JoJo's Bizarre Adventure"),
            Curated("Great Days", "Karen Aoki", "JoJo's Bizarre Adventure"),
            Curated("Re:Re:", "ASIAN KUNG-FU GENERATION", "Erased"),
        ),
        localAnyAnime = true,
    )

    private val onePiece = ThemeDef(
        id = "one_piece",
        title = "Spécial One Piece",
        subtitle = "Les openings, de We Are! à The Peak",
        icon = Icons.Outlined.Sailing,
        curated = listOf(
            Curated("We Are!", "Hiroshi Kitadani"),
            Curated("Believe", "Folder5"),
            Curated("Hikari e", "The Babystars"),
            Curated("Bon Voyage!", "Bon-Bon Blanco"),
            Curated("Kokoro no Chizu", "BOYSTYLE"),
            Curated("Brand New World", "D-51"),
            Curated("Kaze wo Sagashite", "Mari Yaguchi"),
            Curated("Jungle P", "5050"),
            Curated("Share the World", "TVXQ"),
            Curated("One Day", "The ROOTLESS"),
            Curated("Fight Together", "Namie Amuro"),
            Curated("We Go!", "Hiroshi Kitadani"),
            Curated("Hands Up!", "Kota Shinzato"),
            Curated("Wake Up!", "AAA"),
            Curated("Hard Knock Days", "GENERATIONS"),
            Curated("We Can!", "Kishidan"),
            Curated("Hope", "Namie Amuro"),
            Curated("Super Powers", "V6"),
            Curated("Over the Top", "Hiroshi Kitadani"),
            Curated("Dreamin' On", "Da-iCE"),
            Curated("Paint", "I Don't Like Mondays."),
            Curated("The Peak", "SEKAI NO OWARI"),
        ),
        forceAnime = "One Piece",
        localAnime = "One Piece",
    )

    private val cartoons = ThemeDef(
        id = "cartoons",
        title = "Génériques de dessins animés",
        subtitle = "Les génériques qui ont marqué l'enfance",
        icon = Icons.Outlined.Tv,
        playlists = listOf(
            "génériques dessins animés",
            "génériques dessins animés années 2000",
            "génériques dessins animés années 90",
            "cartoon theme songs",
        ),
    )

    private val rapFr = ThemeDef(
        id = "rap_fr",
        title = "Rap français",
        subtitle = "Booba, PNL, Ninho, Nekfeu…",
        icon = Icons.Outlined.RecordVoiceOver,
        artists = listOf(
            "Booba", "PNL", "Ninho", "Jul", "SCH", "Damso", "Nekfeu", "Orelsan", "Niska", "Kaaris", "Lacrim",
            "Gazo", "Tiakola", "Werenoi", "Hamza", "IAM", "Sexion d'Assaut", "MC Solaar", "Lomepal", "Alonzo",
        ),
    )

    val all: List<ThemeDef> = listOf(rapUs, drill, rnb, classics, worldHits, nowHits, animeOpenings, onePiece, cartoons, rapFr)
}
