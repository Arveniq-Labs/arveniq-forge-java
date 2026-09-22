package io.arveniq.forge.android

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Invokes a caller-owned stream-resume routine after the app returns from the background. */
class ForgeAndroidLifecycleController(
    private val scope: CoroutineScope,
    private val onForegroundAfterBackground: suspend () -> Unit,
) : DefaultLifecycleObserver {
    private var backgrounded = false

    override fun onStop(owner: LifecycleOwner) {
        backgrounded = true
    }

    override fun onStart(owner: LifecycleOwner) {
        if (!backgrounded) return
        backgrounded = false
        scope.launch { onForegroundAfterBackground() }
    }
}
