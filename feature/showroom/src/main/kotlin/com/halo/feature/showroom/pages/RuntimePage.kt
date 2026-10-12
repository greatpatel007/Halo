package com.halo.feature.showroom.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.halo.core.designsystem.component.HaloLogo
import com.halo.core.designsystem.component.HaloLogoSize
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.enterRise
import com.halo.core.designsystem.runtime.HaloSensor
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.feature.showroom.ui.Knob
import com.halo.feature.showroom.ui.SectionLabel
import com.halo.feature.showroom.ui.ShowroomCard
import com.halo.feature.showroom.ui.SpecLine
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * The Halo runtime, live (proposal 0003): what the policy allows and why, what the shared clock and sensors are doing,
 * the scene's light and HDR, and the feedback engine. Everything here is read from `HaloTheme.runtime`, the same
 * services every component uses.
 */
@Composable
internal fun RuntimePage() {
    val runtime = HaloTheme.runtime
    val policy = runtime.policy
    val haptics = HaloTheme.haptics
    val s = HaloTheme.spacing
    val colors = HaloTheme.colors
    var logoOn by rememberSaveable { mutableStateOf(true) }

    // ---- Verdict
    SectionLabel("Verdict", Modifier.enterRise(0))
    ShowroomCard(Modifier.enterRise(1)) {
        val paused = policy.pausedBecause
        HaloText(
            text = if (paused == null) "Ambient motion and sensors may run" else "Ambient paused: $paused",
            style = HaloTheme.typography.subtitle,
            color = if (paused == null) colors.accent.text else colors.status.warning,
        )
        Spacer(Modifier.height(s.md))
        SpecLine("sound", if (policy.soundAllowed) "allowed" else "silent (ringer or touch sounds)")
        SpecLine("power", if (policy.conserving) "conserving: HDR off, short Tide" else "normal")
    }

    // ---- Policy inputs
    SectionLabel("Policy · inputs", Modifier.enterRise(2))
    ShowroomCard(Modifier.enterRise(3)) {
        SpecLine("foreground", onOff(policy.foreground))
        SpecLine("reduced motion", onOff(policy.reducedMotion))
        SpecLine("battery saver", onOff(policy.powerSave))
        SpecLine("thermal", if (policy.hot) "hot (severe+)" else "ok")
        SpecLine("ringer", if (policy.ringerNormal) "normal" else "silent / vibrate")
        SpecLine("touch sounds", onOff(policy.touchSounds))
    }

    // ---- Clock, with a live effect to watch
    SectionLabel("Clock · ≈30 steps/s calm · 60 busy · 0 paused", Modifier.enterRise(4))
    ShowroomCard(Modifier.enterRise(5)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.padding(end = s.lg)) {
                HaloLogo(size = HaloLogoSize.Medium, animated = logoOn, surfaceColor = colors.bg.surface)
            }
            Knob("Live effect", listOf(true, false), logoOn, { logoOn = it }, optionLabel = { if (it) "on" else "off" })
        }
        Spacer(Modifier.height(s.md))
        SpecLine("visible effects", runtime.clock.activeEffects.toString())
        SpecLine("steps / s", runtime.clock.stepRate.toString())
    }

    // ---- Sensors
    SectionLabel("Sensors · shared, ref-counted", Modifier.enterRise(6))
    ShowroomCard(Modifier.enterRise(7)) {
        val active = runtime.sensors.active
        SpecLine("gravity (tilt light)", onOff(HaloSensor.Gravity in active))
        SpecLine("gyroscope (logo inertia)", onOff(HaloSensor.Gyroscope in active))
    }

    // ---- Scene
    SectionLabel("Scene · light and HDR", Modifier.enterRise(8))
    ShowroomCard(Modifier.enterRise(9)) {
        val rotation = runtime.scene.rig.rotation
        val hdr = runtime.scene.hdr
        SpecLine("light tilt", String.format(Locale.US, "%.1f°", Math.toDegrees(rotation.angle.toDouble())))
        SpecLine("hdr headroom", String.format(Locale.US, "×%.2f", hdr.headroom))
        SpecLine("emission gain", String.format(Locale.US, "×%.2f%s", hdr.emission, if (hdr.limited) " (limited)" else ""))
    }

    // ---- Feedback
    SectionLabel("Feedback", Modifier.enterRise(10))
    ShowroomCard(Modifier.enterRise(11)) {
        // Latency is measured on the audio thread; sample it once a second while this page is open.
        val latency by produceState(haptics.audioLatencyMs) {
            while (true) {
                value = haptics.audioLatencyMs
                delay(LATENCY_SAMPLE_MS)
            }
        }
        SpecLine("haptic tier", haptics.tier.name.lowercase())
        SpecLine("haptics / sound", "${onOff(haptics.hapticsEnabled)} / ${onOff(haptics.soundEnabled)}")
        SpecLine("strength", String.format(Locale.US, "×%.1f", haptics.strength))
        SpecLine("audio latency", if (latency < 0f) "not measured yet (play a sound)" else String.format(Locale.US, "%.0f ms", latency))
        Spacer(Modifier.height(s.md))
        HaloText(
            "Try it: scroll the logo away, press Home, turn on battery saver, or simulate heat " +
                "(adb shell cmd thermalservice override-status 3). Everything above follows.",
            style = HaloTheme.typography.bodyMedium,
            color = colors.content.secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun onOff(value: Boolean) = if (value) "on" else "off"

private const val LATENCY_SAMPLE_MS = 1_000L
