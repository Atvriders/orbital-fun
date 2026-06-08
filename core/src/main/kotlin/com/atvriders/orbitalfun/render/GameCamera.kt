package com.atvriders.orbitalfun.render

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3

/**
 * Top-down camera with pinch-zoom, drag-pan, and a follow-ship toggle. One world
 * unit maps to one pixel at zoom 1.
 */
class GameCamera(viewportWidth: Float, viewportHeight: Float) {

    val camera = OrthographicCamera()
    var followShip: Boolean = true

    private val tmp = Vector3()

    init {
        camera.setToOrtho(false, viewportWidth, viewportHeight)
        // Start zoomed out enough to see the inner system.
        camera.zoom = 1.6f
        camera.update()
    }

    fun resize(width: Float, height: Float) {
        camera.viewportWidth = width
        camera.viewportHeight = height
        camera.update()
    }

    fun zoomBy(factor: Float) {
        camera.zoom = MathUtils.clamp(camera.zoom * factor, MIN_ZOOM, MAX_ZOOM)
    }

    fun setZoom(zoom: Float) {
        camera.zoom = MathUtils.clamp(zoom, MIN_ZOOM, MAX_ZOOM)
    }

    /** Pan by a screen-space drag delta (pixels), turning off follow. */
    fun panByScreen(dxScreen: Float, dyScreen: Float) {
        followShip = false
        camera.position.add(-dxScreen * camera.zoom, dyScreen * camera.zoom, 0f)
    }

    fun centerOn(target: Vector2) {
        camera.position.set(target.x, target.y, 0f)
    }

    fun update(shipPosition: Vector2) {
        if (followShip) camera.position.set(shipPosition.x, shipPosition.y, 0f)
        camera.update()
    }

    /** Convert a screen point (origin top-left) into world coordinates. */
    fun screenToWorld(screenX: Float, screenY: Float, out: Vector2): Vector2 {
        tmp.set(screenX, screenY, 0f)
        camera.unproject(tmp)
        return out.set(tmp.x, tmp.y)
    }

    val zoom: Float get() = camera.zoom

    companion object {
        const val MIN_ZOOM = 0.25f
        const val MAX_ZOOM = 8f
    }
}
