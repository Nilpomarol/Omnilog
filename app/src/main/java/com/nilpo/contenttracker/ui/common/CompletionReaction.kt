package com.nilpo.contenttracker.ui.common

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.Objective
import com.nilpo.contenttracker.core.model.ObjectiveMetric
import com.nilpo.contenttracker.core.model.ObjectiveProgress
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.pace
import com.nilpo.contenttracker.core.objectives.ObjectiveCalculator
import com.nilpo.contenttracker.ui.home.GoalRing
import com.nilpo.contenttracker.ui.profile.ProfilePreferences
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.SerifFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.cos
import kotlin.math.sin

/**
 * An objective one write moved: its progress once the write landed and its count from just before.
 * [reached] is true only for the write that carried it over its target, and is switched off when
 * that goal has already been celebrated (see [CelebratedObjectives]).
 */
data class ObjectiveStep(
    val progress: ObjectiveProgress,
    val previousValue: Int,
    val reached: Boolean = previousValue < progress.objective.targetValue && progress.isComplete,
)

/**
 * Compares the active objectives across one write. Measured on the library as it was before and as
 * it is once the write has landed, so every metric is covered — page and episode goals move through
 * progress entries the repository derives, which no prediction from the request could see.
 *
 * Reached goals come first, then title counts over unit counts, then a type-specific goal over an
 * any-media one: "books this year" says more about a finished book than "titles this year".
 */
fun objectiveSteps(
    before: List<TrackedMedia>,
    after: List<TrackedMedia>,
    objectives: List<Objective>,
    today: LocalDate = LocalDate.now(),
): List<ObjectiveStep> {
    val active = objectives.filter { it.archivedAtEpochMillis == null }
    val calculator = ObjectiveCalculator(today)
    return calculator.calculate(after, active)
        .zip(calculator.calculate(before, active))
        .filter { (now, then) -> now.currentValue > then.currentValue }
        .map { (now, then) -> ObjectiveStep(now, then.currentValue) }
        .sortedWith(
            compareByDescending<ObjectiveStep> { it.reached }
                .thenByDescending { it.progress.objective.metric == ObjectiveMetric.CompletedTitles }
                .thenByDescending { it.progress.objective.mediaType != null },
        )
}

/**
 * [steps] with the goals already celebrated demoted to plain progress, then reordered so a goal still
 * waiting for its moment leads. Without this, a celebrated goal that sorts first would hide another
 * goal the same write has just reached.
 */
fun List<ObjectiveStep>.withoutCelebrated(isCelebrated: (Objective) -> Boolean): List<ObjectiveStep> =
    map { if (it.reached && isCelebrated(it.progress.objective)) it.copy(reached = false) else it }
        .sortedByDescending { it.reached }

/** What finishing a title meant, for the completion page. */
data class CompletionReaction(
    val title: String,
    val coverUrl: String?,
    /** Whole days from start to finish; null when the session never recorded a start. */
    val daysTaken: Long?,
    val visitNumber: Int,
    /** Every objective this completion moved, most meaningful first. */
    val steps: List<ObjectiveStep>,
) {
    /** The one objective the page shows. */
    val objective: ObjectiveStep? get() = steps.firstOrNull()
}

fun completionReaction(
    media: TrackedMedia,
    session: TrackingSession,
    startedAt: LocalDate?,
    finishedAt: LocalDate,
    steps: List<ObjectiveStep>,
): CompletionReaction = CompletionReaction(
    title = media.item.title,
    coverUrl = media.item.coverUrl,
    daysTaken = startedAt?.let { ChronoUnit.DAYS.between(it, finishedAt) }?.takeIf { it >= 0 },
    visitNumber = media.visitNumber(session),
    steps = steps,
)

/**
 * Remembers which goals have had their moment, so editing a date back and forth or re-logging an
 * entry cannot celebrate the same goal twice. Shares its record with the Profile screen's catch-up
 * banner, so a goal celebrated here is not announced again there, nor the other way round. Undoing
 * the write that reached a goal forgets it again.
 */
object CelebratedObjectives {
    fun contains(context: Context, objective: Objective): Boolean = objective.id.toString() in read(context)

    fun add(context: Context, objective: Objective) = write(context, read(context) + objective.id.toString())

    fun remove(context: Context, objective: Objective) = write(context, read(context) - objective.id.toString())

    private fun read(context: Context): Set<String> =
        ProfilePreferences.from(context).getStringSet(ProfilePreferences.CELEBRATED_OBJECTIVES_KEY, emptySet()).orEmpty()

    private fun write(context: Context, ids: Set<String>) {
        ProfilePreferences.from(context).edit().putStringSet(ProfilePreferences.CELEBRATED_OBJECTIVES_KEY, ids).apply()
    }
}

/** The shared rhythm of a celebration page, read by whatever the page shows. */
private class CelebrationBeats(
    val entrance: Float,
    val stamp: Float,
    val ring: Float,
    val facts: Float,
    val advanced: Boolean,
    /** The goal's own beat, 0 until it lands and settling at 1 after. */
    val finale: Float,
    /** Light running along the full bar just before the finale lands. */
    val sweep: Float,
    /** Rays and an outline leaving the objective as it lands, then gone. */
    val burst: Float,
    /** The objective panel's bounce as it lands. */
    val pop: Float,
)

/**
 * The top tier of feedback, deliberately in the way: a translucent page settles over the whole app,
 * its hero takes a stamp with a confirm haptic, then the facts settle in. With [withFinale] a second
 * beat follows once the facts have played — a goal reached on top of the hero's own moment.
 *
 * It is a layer rather than a dialog so the library stays faintly visible behind it. Once everything
 * has played, back or a tap anywhere continues; the page fades away before [onContinue] or [onUndo]
 * runs. [onUndo] is null when the write behind the page cannot be undone.
 */
@Composable
private fun CelebrationPage(
    key: Any,
    withFinale: Boolean,
    onContinue: () -> Unit,
    onUndo: (() -> Unit)?,
    modifier: Modifier,
    hero: @Composable BoxScope.(CelebrationBeats) -> Unit,
    content: @Composable ColumnScope.(CelebrationBeats) -> Unit,
) {
    val accent = OmnilogTheme.accents.Completed
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    // Previews render one frame, so they start from the settled state instead of the first frame.
    val settled = LocalInspectionMode.current
    val start = if (settled) 1f else 0f
    val scrim = remember(key) { Animatable(start) }
    val entrance = remember(key) { Animatable(start) }
    val stamp = remember(key) { Animatable(start) }
    val ring = remember(key) { Animatable(start) }
    val finale = remember(key) { Animatable(if (settled && withFinale) 1f else 0f) }
    val sweep = remember(key) { Animatable(start) }
    val burst = remember(key) { Animatable(start) }
    val pop = remember(key) { Animatable(1f) }
    var revealed by remember(key) { mutableStateOf(settled) }
    var advanced by remember(key) { mutableStateOf(settled) }
    // Back and a stray tap only count once the whole moment has played, so a back press meant for the
    // closing sheet cannot skip past the page (and its undo) before it is even read.
    var dismissible by remember(key) { mutableStateOf(settled) }
    var leaving by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) {
        if (settled) return@LaunchedEffect
        launch { scrim.animateTo(1f, tween(durationMillis = 280)) }
        launch { entrance.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)) }
        delay(360)
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        launch { stamp.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow)) }
        launch { ring.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing)) }
        delay(160)
        revealed = true
        delay(420)
        advanced = true
        if (withFinale) {
            // A second act for the goal, once its bar has filled and its count has rolled over the
            // target: light runs along the full bar, then the goal lands with its own double pulse.
            delay(850)
            sweep.animateTo(1f, tween(durationMillis = 420, easing = FastOutSlowInEasing))
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            launch { finale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)) }
            launch { burst.animateTo(1f, tween(durationMillis = 750, easing = FastOutSlowInEasing)) }
            launch {
                pop.animateTo(1.09f, tween(durationMillis = 130, easing = FastOutSlowInEasing))
                pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow))
            }
            delay(140)
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            delay(700)
        } else {
            delay(1_000)
        }
        dismissible = true
    }
    fun leave(then: () -> Unit) {
        if (leaving) return
        leaving = true
        scope.launch {
            launch { entrance.animateTo(0f, tween(durationMillis = 200)) }
            scrim.animateTo(0f, tween(durationMillis = 240))
            then()
        }
    }
    BackHandler { if (dismissible) leave(onContinue) }
    val facts by animateFloatAsState(if (revealed) 1f else 0f, tween(400), label = "facts")
    val beats = CelebrationBeats(
        entrance = entrance.value,
        stamp = stamp.value,
        ring = ring.value,
        facts = facts,
        advanced = advanced,
        finale = finale.value,
        sweep = sweep.value,
        burst = burst.value,
        pop = pop.value,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = scrim.value }
            .background(OmnilogTheme.colors.appBackground.copy(alpha = 0.94f))
            // Swallows every touch so nothing behind the page reacts while it is up.
            .clickable(interactionSource = null, indication = null) { if (dismissible) leave(onContinue) }
            .systemBarsPadding()
            .padding(horizontal = 28.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .wrapContentHeight(Alignment.CenterVertically)
                .widthIn(max = 420.dp)
                .graphicsLayer {
                    scaleX = 0.9f + 0.1f * entrance.value
                    scaleY = 0.9f + 0.1f * entrance.value
                    translationY = (1f - entrance.value) * 56.dp.toPx()
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) { hero(beats) }
            Spacer(Modifier.height(36.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = facts
                        translationY = (1f - facts) * 12.dp.toPx()
                    }
                    .semantics(mergeDescendants = true) {},
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) { content(beats) }
        }

        Column(
            modifier = Modifier
                .padding(top = 16.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .graphicsLayer { alpha = facts },
        ) {
            Button(
                onClick = { leave(onContinue) },
                enabled = revealed,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = contentColorOn(accent)),
            ) {
                Text(stringResource(R.string.quick_progress_next), fontWeight = FontWeight.Bold)
            }
            if (onUndo != null) {
                TextButton(onClick = { leave(onUndo) }, enabled = revealed, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.deletion_undo_action),
                        color = OmnilogTheme.colors.appMuted,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/** Finishing a title: the cover takes the stamp; a goal it reached gets the second beat. */
@Composable
fun CompletionCelebration(
    reaction: CompletionReaction,
    onContinue: () -> Unit,
    onUndo: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val accent = OmnilogTheme.accents.Completed
    CelebrationPage(
        key = reaction,
        withFinale = reaction.objective?.reached == true,
        onContinue = onContinue,
        onUndo = onUndo,
        modifier = modifier,
        hero = { beats ->
            Ripple(beats.ring, accent, RoundedCornerShape(16.dp), Modifier.size(CoverWidth, CoverHeight))
            Box(
                Modifier
                    .size(CoverWidth, CoverHeight)
                    // Steps back as the goal lands, so the eye goes where the news is.
                    .graphicsLayer {
                        val recede = beats.finale.coerceIn(0f, 1f)
                        scaleX = 1f - 0.08f * recede
                        scaleY = 1f - 0.08f * recede
                        alpha = 1f - 0.2f * recede
                        // Fades each draw instead of an offscreen layer, which would crop the stamp
                        // that sits past the cover's corner.
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    },
            ) {
                MetadataCoverImage(reaction.coverUrl, Modifier.fillMaxSize(), RoundedCornerShape(14.dp))
                CheckStamp(
                    progress = beats.stamp,
                    size = 54.dp,
                    modifier = Modifier.align(Alignment.BottomEnd).offset(x = 16.dp, y = 16.dp),
                )
            }
        },
    ) { beats ->
        Eyebrow(stringResource(R.string.completion_reaction_label))
        Headline(reaction.title)
        val facts = listOfNotNull(
            reaction.daysTaken?.let { days ->
                if (days == 0L) {
                    stringResource(R.string.completion_reaction_same_day)
                } else {
                    pluralStringResource(R.plurals.completion_reaction_days, days.toInt(), days)
                }
            },
            reaction.visitNumber.takeIf { it > 1 }?.let { stringResource(R.string.completion_reaction_visit, it) },
        )
        if (facts.isNotEmpty()) Facts(facts.joinToString(" · "))
        reaction.objective?.let { step -> ObjectivePanel(step, beats) }
    }
}

/**
 * A goal reached by logging progress rather than by finishing something: the goal's own ring fills
 * and closes where a cover would stand, then takes the stamp.
 */
@Composable
fun ObjectiveCelebration(
    step: ObjectiveStep,
    onContinue: () -> Unit,
    onUndo: (() -> Unit)?,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    val progress = step.progress
    val objective = progress.objective
    val accent = OmnilogTheme.accents.Completed
    val pace = remember(progress, today) { progress.pace(today) }
    val target = objective.targetValue.coerceAtLeast(1).toFloat()
    CelebrationPage(
        key = step,
        withFinale = false,
        onContinue = onContinue,
        onUndo = onUndo,
        modifier = modifier,
        hero = { beats ->
            val fill by animateFloatAsState(
                targetValue = if (beats.entrance > 0.3f) progress.currentValue / target else step.previousValue / target,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "goalFill",
            )
            Ripple(beats.ring, accent, CircleShape, Modifier.size(GoalDiameter))
            Box(Modifier.size(GoalDiameter), contentAlignment = Alignment.Center) {
                GoalRing(fill, pace, objective.mediaType.objectiveAccent(), diameter = GoalDiameter, strokeWidth = 12.dp)
                ObjectiveMediaIcon(objective.mediaType, objective.mediaType.objectiveAccent(), 64.dp)
                CheckStamp(
                    progress = beats.stamp,
                    size = 54.dp,
                    modifier = Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp),
                )
            }
        },
    ) { _ ->
        Eyebrow(stringResource(R.string.objective_reaction_label))
        Headline(objectiveSentenceTitle(objective))
        val daysEarly = ChronoUnit.DAYS.between(today, objective.endDate)
        Facts(
            listOf(
                objectiveProgressLabel(progress),
                if (daysEarly > 0) {
                    pluralStringResource(R.plurals.objective_reaction_days_early, daysEarly.toInt(), daysEarly)
                } else {
                    stringResource(R.string.objective_reaction_on_time)
                },
            ).joinToString(" · "),
        )
    }
}

@Composable
private fun Eyebrow(text: String) {
    Text(
        text = text.uppercase(),
        color = OmnilogTheme.accents.Completed,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
    )
}

@Composable
private fun Headline(text: String) {
    Text(
        text = text,
        color = OmnilogTheme.colors.appInk,
        fontFamily = SerifFontFamily,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun Facts(text: String) {
    Text(
        text = text,
        color = OmnilogTheme.colors.appMuted,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
}

/** One outline that leaves the hero as the stamp lands, then is gone. */
@Composable
private fun Ripple(progress: Float, color: Color, shape: androidx.compose.ui.graphics.Shape, modifier: Modifier) {
    Box(
        modifier
            .graphicsLayer {
                scaleX = 1f + 0.35f * progress
                scaleY = 1f + 0.22f * progress
                alpha = if (progress == 0f) 0f else 0.6f * (1f - progress)
            }
            .border(2.dp, color, shape),
    )
}

@Composable
private fun CheckStamp(progress: Float, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val accent = OmnilogTheme.accents.Completed
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = progress
                scaleY = progress
                alpha = progress.coerceIn(0f, 1f)
                rotationZ = (1f - progress) * -40f
            }
            .size(size)
            .background(accent, CircleShape)
            .border(size / 18, OmnilogTheme.colors.appBackground, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = contentColorOn(accent), modifier = Modifier.size(size * 0.6f))
    }
}

/**
 * The objective a completion moved. When the completion reached it, the panel gets its own act on
 * the page's finale beats: light runs along the full bar, then the panel bounces, rays and an
 * outline leave its new stamp, the count pulses, and the goal's title and timing settle underneath.
 */
@Composable
private fun ObjectivePanel(step: ObjectiveStep, beats: CelebrationBeats, today: LocalDate = LocalDate.now()) {
    val progress = step.progress
    val objective = progress.objective
    val done = step.reached && beats.finale > 0.2f
    val completed = OmnilogTheme.accents.Completed
    val goalAccent = objective.mediaType.objectiveAccent()
    val color by animateColorAsState(if (done) completed else goalAccent, tween(250), label = "objectiveColor")
    val border by animateColorAsState(
        if (done) completed else OmnilogTheme.colors.appLine,
        tween(250),
        label = "objectiveBorder",
    )
    val target = objective.targetValue.coerceAtLeast(1).toFloat()
    val value = if (beats.advanced) progress.currentValue else step.previousValue
    val fill by animateFloatAsState(
        targetValue = (value / target).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "objectiveFill",
    )
    val panelShape = RoundedCornerShape(12.dp)

    Box(Modifier.padding(top = 14.dp).fillMaxWidth()) {
        if (step.reached) Ripple(beats.burst, completed, panelShape, Modifier.matchParentSize())
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = beats.pop
                    scaleY = beats.pop
                }
                .background(OmnilogTheme.colors.appPanel, panelShape)
                .border(if (done) 2.dp else 1.dp, border, panelShape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .drawBehind { if (step.reached) drawBurst(beats.burst, completed, goalAccent) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) {
                        CheckStamp(progress = beats.finale, size = 24.dp)
                    } else {
                        ObjectiveMediaIcon(objective.mediaType, color, 20.dp)
                    }
                }
                Text(
                    text = if (done) stringResource(R.string.completion_reaction_objective_reached) else objectiveSentenceTitle(objective),
                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                    color = if (done) color else OmnilogTheme.colors.appInk,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AnimatedContent(
                    targetState = value,
                    modifier = Modifier.graphicsLayer {
                        // The count takes the bounce twice over, so the number itself reads as the news.
                        val pulse = 1f + (beats.pop - 1f) * 2.5f
                        scaleX = pulse
                        scaleY = pulse
                    },
                    transitionSpec = {
                        (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                    },
                    label = "objectiveCount",
                ) { count ->
                    Text(
                        text = count.toString(),
                        color = color,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "/${objective.targetValue}",
                    color = OmnilogTheme.colors.appMuted,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(OmnilogTheme.colors.appLine, CircleShape),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fill)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(color)
                        .drawWithContent {
                            drawContent()
                            if (step.reached && beats.sweep > 0f && beats.sweep < 1f) {
                                val band = size.width * 0.35f
                                val x = -band + (size.width + band) * beats.sweep
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.75f), Color.Transparent),
                                        startX = x,
                                        endX = x + band,
                                    ),
                                )
                            }
                        },
                )
            }
            // Laid out from the start for a goal this completion reaches, so the finale does not shift the page.
            if (step.reached) {
                val daysEarly = ChronoUnit.DAYS.between(today, objective.endDate)
                Text(
                    text = listOf(
                        objectiveSentenceTitle(objective),
                        if (daysEarly > 0) {
                            pluralStringResource(R.plurals.objective_reaction_days_early, daysEarly.toInt(), daysEarly)
                        } else {
                            stringResource(R.string.objective_reaction_on_time)
                        },
                    ).joinToString(" · "),
                    modifier = Modifier.graphicsLayer {
                        val settle = beats.finale.coerceIn(0f, 1f)
                        alpha = settle
                        translationY = (1f - settle) * 6.dp.toPx()
                    },
                    color = OmnilogTheme.colors.appMuted,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Twelve short rays leaving the centre, alternating the completed colour with the goal's own. */
private fun DrawScope.drawBurst(progress: Float, first: Color, second: Color) {
    if (progress <= 0f || progress >= 1f) return
    val inner = 14.dp.toPx() + 22.dp.toPx() * progress
    val length = 10.dp.toPx() * (1f - progress)
    val alpha = 1f - progress
    repeat(12) { index ->
        val angle = Math.toRadians(index * 30.0 - 90.0)
        val dx = cos(angle).toFloat()
        val dy = sin(angle).toFloat()
        drawLine(
            color = (if (index % 2 == 0) first else second).copy(alpha = alpha),
            start = center + Offset(dx * inner, dy * inner),
            end = center + Offset(dx * (inner + length), dy * (inner + length)),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private val CoverWidth = 150.dp
private val CoverHeight = 222.dp
private val GoalDiameter = 190.dp
