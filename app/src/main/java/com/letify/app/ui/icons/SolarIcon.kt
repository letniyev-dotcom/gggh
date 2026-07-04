package com.letify.app.ui.icons

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import com.caverock.androidsvg.SVG
import android.graphics.Bitmap
import android.graphics.Canvas
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Renders a Solar (Iconify) SVG icon from `assets/icons/<name>.svg`.
 *
 * Why two render paths?
 *
 * 1. **Synchronous (preferred).** [SolarIconLoader.prewarmAll] eagerly
 *    decodes every SVG into an [ImageBitmap] on a background thread at
 *    process start. Once the bitmap lands in [SolarIconLoader.bitmaps],
 *    SolarIcon paints with the standard, synchronous [Image] — no
 *    `AsyncImagePainter` state machine in the middle, no async decode
 *    hitch when the user opens the app or switches tabs. The bitmap is
 *    rasterised at a generous max-dimension and rendered with
 *    `ContentScale.Fit`, so it remains crisp at every call site size
 *    (22dp navbar, 72dp hero, etc.).
 *
 * 2. **Asynchronous fallback.** During the brief first-launch window
 *    *before* prewarm has populated the cache we drop back to
 *    [AsyncImage] so the slot doesn't show through empty. This path
 *    self-heals — once the prewarm finishes and writes to the snapshot
 *    map, the icon recomposes onto the synchronous branch.
 *
 * The navbar's 5 icons are decoded first by the prewarm worker and
 * the main thread briefly waits on a latch in MainActivity.onCreate
 * (see [SolarIconLoader.awaitNavbarReady]) so they're guaranteed to
 * paint instantly on the first frame.
 */
@Composable
fun SolarIcon(
    name: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: Dp = 24.dp,
) {
    val bitmap = SolarIconLoader.bitmaps[name]
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier.size(size),
            contentScale = ContentScale.Fit,
            colorFilter = if (tint != Color.Unspecified) ColorFilter.tint(tint) else null,
        )
        return
    }

    val context = LocalContext.current
    val loader = remember(context.applicationContext) {
        SolarIconLoader.get(context.applicationContext)
    }
    val request = remember(name) {
        ImageRequest.Builder(context)
            .data("file:///android_asset/icons/$name.svg")
            .crossfade(false)
            .memoryCacheKey("solar:$name")
            .build()
    }
    AsyncImage(
        model = request,
        imageLoader = loader,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        colorFilter = if (tint != Color.Unspecified) ColorFilter.tint(tint) else null,
        modifier = modifier.size(size),
    )
}

object SolarIconLoader {
    /**
     * Icon names used by the navbar. The prewarm worker decodes these
     * first and counts down [navbarReady]; MainActivity.onCreate waits
     * on that latch (up to a brief timeout) before calling setContent
     * so the first frame of the app already has every tab glyph in
     * cache. Keep in sync with Navbar.tabIcon — each decode is <10ms
     * so we list them by hand here rather than coupling the icons
     * module to the navbar module.
     */
    private val NAVBAR_ICONS = arrayOf(
        "checklist-minimalistic-bold-duotone",
        "user-id-bold-duotone",
    )

    /**
     * Icons that must be ready by the time the user opens the habit
     * icon picker. Decoded immediately after the navbar icons, before
     * the rest of the alphabetical sweep, so the 6×6 grid always paints
     * with every cell filled — never the half-empty grid the user saw
     * before. Keep in sync with HabitIconCatalog in AddHabitScreen.kt.
     */
    private val HABIT_CATALOG_ICONS = arrayOf(
        "bottle-bold-duotone", "cup-paper-bold-duotone", "cup-hot-bold-duotone",
        "tea-cup-bold-duotone", "wineglass-bold-duotone", "waterdrop-bold-duotone",
        "plate-bold-duotone", "donut-bold-duotone", "chef-hat-bold-duotone",
        "pill-bold-duotone", "leaf-bold-duotone", "scale-bold-duotone",
        "dumbbell-bold-duotone", "running-bold-duotone", "walking-bold-duotone",
        "bicycling-bold-duotone", "swimming-bold-duotone", "stretching-bold-duotone",
        "heart-bold-duotone", "heart-pulse-bold-duotone", "meditation-bold-duotone",
        "moon-stars-bold-duotone", "bed-bold-duotone", "smile-circle-bold-duotone",
        "sun-bold-duotone", "sunrise-bold-duotone", "sunset-bold-duotone",
        "flame-bold-duotone", "alarm-bold-duotone", "stopwatch-bold-duotone",
        "book-bookmark-bold-duotone", "notebook-bold-duotone", "pen-bold-duotone",
        "clipboard-check-bold-duotone", "star-bold-duotone", "stars-bold-duotone",
    )

    /**
     * Max edge in pixels for the rasterised SVG. 256px keeps every icon
     * crisp at the largest in-app call site (the 72dp Telegram glyph on
     * the bindings screen ≈ 216px on a 3x device) while keeping decode
     * memory tiny — 85 icons × 256² × 4B ≈ 22MB worst case, but most
     * Solar SVGs decode to far less because they're white on transparent.
     */
    private const val RASTER_PX = 256

    @Volatile private var instance: ImageLoader? = null
    @Volatile private var prewarmStarted = false

    /**
     * Synchronous, snapshot-aware bitmap cache. SolarIcon reads this
     * directly during composition; writes happen on a worker thread
     * (snapshot state is thread-safe for writes).
     */
    val bitmaps: SnapshotStateMap<String, ImageBitmap> = mutableStateMapOf()

    fun get(appContext: Context): ImageLoader =
        instance ?: synchronized(this) {
            instance ?: ImageLoader.Builder(appContext)
                .components { add(SvgDecoder.Factory()) }
                .memoryCache {
                    MemoryCache.Builder(appContext)
                        .maxSizePercent(0.15)
                        .build()
                }
                .diskCache(null as DiskCache?)
                .build()
                .also { instance = it }
        }

    /** Latch released when [prewarmAll] has decoded all 5 navbar icons. */
    private val navbarReady = CountDownLatch(NAVBAR_ICONS.size)

    /**
     * Block the calling (main) thread until every navbar glyph is in
     * [bitmaps] — or until [timeoutMs] elapses, whichever comes first.
     *
     * History / why this is now cheap: an earlier build decoded the icons
     * with `runBlocking { coilLoader.execute(...) }` on the worker. Coil's
     * `execute` launches its request job on `Dispatchers.Main.immediate`, so
     * the decode could only make progress while the MAIN thread was free —
     * but main was sitting right here, blocked on this latch. The two
     * deadlocked, so this await ALWAYS burned its full timeout: a guaranteed
     * ~800ms freeze on every cold launch, after which the icons still faded
     * in late once the looper resumed. That was the "лаги при первом запуске"
     * symptom.
     *
     * The fix: [decodeInto] now rasterises each SVG with AndroidSVG directly
     * on the worker thread — no coroutines, no Coil, no Main dispatch — so
     * the navbar glyphs are ready in a few milliseconds and this latch
     * releases almost immediately. The bounded timeout stays purely as a
     * safety net so a pathological decode stall can never strand the splash.
     */
    fun awaitNavbarReady(timeoutMs: Long = 800L) {
        if (NAVBAR_ICONS.all { bitmaps.containsKey(it) }) return
        runCatching { navbarReady.await(timeoutMs, TimeUnit.MILLISECONDS) }
    }

    /**
     * Decode every SVG in `assets/icons/` on a background thread and put
     * the resulting bitmaps into [bitmaps]. Navbar icons are decoded
     * **first** so [awaitNavbarReady] can release the main thread as
     * early as possible. Safe to call multiple times — the second call
     * is a no-op.
     */
    fun prewarmAll(appContext: Context) {
        if (prewarmStarted) return
        synchronized(this) {
            if (prewarmStarted) return
            prewarmStarted = true
        }
        Thread(
            {
                // Decode navbar glyphs first so main can unblock ASAP.
                for (name in NAVBAR_ICONS) {
                    if (!bitmaps.containsKey(name)) {
                        decodeInto(appContext, "$name.svg")
                    }
                    navbarReady.countDown()
                }
                // Then the habit-picker catalog so the 6×6 grid is fully
                // populated by the time the user can possibly tap into
                // "Иконка" — even a fast tester won't reach it before
                // ~36 decodes (<1s in practice).
                for (name in HABIT_CATALOG_ICONS) {
                    if (!bitmaps.containsKey(name)) {
                        decodeInto(appContext, "$name.svg")
                    }
                }
                // Now the rest. Sorted for determinism — easier to
                // reproduce ordering bugs in profiles.
                val assetManager = appContext.assets
                val all = runCatching { assetManager.list("icons") }
                    .getOrNull()
                    ?.filter { it.endsWith(".svg") }
                    ?.sorted()
                    ?: return@Thread
                val priority = HashSet<String>().apply {
                    NAVBAR_ICONS.forEach { add("$it.svg") }
                    HABIT_CATALOG_ICONS.forEach { add("$it.svg") }
                }
                for (file in all) {
                    if (file in priority) continue
                    val key = file.removeSuffix(".svg")
                    if (bitmaps.containsKey(key)) continue
                    decodeInto(appContext, file)
                }
            },
            "solar-icon-prewarm",
        ).apply {
            isDaemon = true
            // One step below default — keeps the UI thread free of
            // scheduler contention during the heavy first-frame burst,
            // but high enough that the 5 navbar decodes still finish
            // in well under our 800ms main-thread wait budget.
            priority = Thread.NORM_PRIORITY - 1
        }.start()
    }

    /**
     * Decode any names in [names] that aren't yet in [bitmaps], using
     * the calling thread. Must NOT be called from the main thread —
     * `runBlocking` inside [decodeInto] would deadlock there. The
     * habit picker uses this from a coroutine on Dispatchers.IO as
     * a safety net in case prewarm hasn't reached the catalog yet
     * (very unlikely after the priority bump, but free insurance).
     */
    fun ensureLoadedBlocking(appContext: Context, names: List<String>) {
        for (n in names) {
            if (!bitmaps.containsKey(n)) {
                decodeInto(appContext, "$n.svg")
            }
        }
    }

    /**
     * Rasterise a single `assets/icons/<file>` SVG into [bitmaps] on the
     * CALLING thread, synchronously, using AndroidSVG directly.
     *
     * This deliberately bypasses Coil: `coil-svg` is itself only a thin
     * wrapper around `com.caverock.androidsvg`, so the pixels are identical,
     * but a direct render has no coroutine machinery and — crucially — never
     * dispatches onto `Dispatchers.Main.immediate`. That's what makes the
     * prewarm worker fully self-contained: it can finish the 5 navbar glyphs
     * in a few ms even while [awaitNavbarReady] is parking the main thread,
     * instead of deadlocking against it (see [awaitNavbarReady]).
     *
     * The result is a software ARGB_8888 [Bitmap]; software (not hardware)
     * because these bitmaps live in our own [SnapshotStateMap] and may be
     * drawn from any thread, and hardware bitmaps don't survive cross-thread
     * upload. Solar SVGs paint with `fill="currentColor"` (AndroidSVG resolves
     * that to black) on a transparent canvas — every in-app call site that
     * needs a colour tints the result via [ColorFilter] at draw time.
     */
    private fun decodeInto(appContext: Context, file: String) {
        val key = file.removeSuffix(".svg")
        if (bitmaps.containsKey(key)) return
        val bitmap = runCatching {
            appContext.assets.open("icons/$file").use { input ->
                val svg = SVG.getFromInputStream(input)
                // Scale the icon's viewBox to fill the raster square. Solar
                // icons are square (viewBox 0 0 24 24), so this preserves
                // aspect ratio exactly — same as Coil's Size(RASTER,RASTER).
                svg.setDocumentWidth(RASTER_PX.toFloat())
                svg.setDocumentHeight(RASTER_PX.toFloat())
                val bmp = Bitmap.createBitmap(
                    RASTER_PX, RASTER_PX, Bitmap.Config.ARGB_8888,
                )
                svg.renderToCanvas(Canvas(bmp))
                bmp
            }
        }.getOrNull() ?: return
        bitmaps[key] = bitmap.asImageBitmap()
    }
}
