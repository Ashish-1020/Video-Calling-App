package com.aashu.natalks.ui.call

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun VideoRendererView(
    eglBase: EglBase,
    videoTrack: VideoTrack?,
    modifier: Modifier = Modifier,
    mirror: Boolean = false
) {
    val renderer = remember {
        RendererHolder()
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                init(eglBase.eglBaseContext, null)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setMirror(mirror)
                setEnableHardwareScaler(true)
                renderer.view = this
            }
        },
        update = { view ->
            view.setMirror(mirror)
        }
    )

    DisposableEffect(videoTrack, renderer.view) {
        val view = renderer.view
        if (view != null && videoTrack != null) {
            videoTrack.addSink(view)
        }
        onDispose {
            if (view != null && videoTrack != null) {
                videoTrack.removeSink(view)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { renderer.view?.release() }
    }
}

private class RendererHolder {
    var view: SurfaceViewRenderer? = null
}
