/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;

import com.android.gallery3d.app.GalleryActivity;
import com.android.gallery3d.ui.GLCanvas;
import com.android.gallery3d.ui.Raw2DTexture;
import com.android.gallery3d.ui.ScreenNail;
import com.android.gallery3d.ui.SurfaceTextureScreenNail;

/*
 * This is a ScreenNail which can displays camera preview.
 */
public class CameraScreenNail extends SurfaceTextureScreenNail {
    private static final String TAG = "CameraScreenNail";
    private static final int ANIM_NONE = 0;
    private static final int ANIM_TO_START = 1;
    private static final int ANIM_RUNNING = 2;

    private boolean mVisible;
    private RenderListener mRenderListener;
    private final float[] mTextureTransformMatrix = new float[16];

    // Animation.
    private CaptureAnimManager mAnimManager = new CaptureAnimManager();
    private int mAnimState = ANIM_NONE;
    private Raw2DTexture mAnimTexture;

    public interface RenderListener {
        void requestRender();
    }

    public CameraScreenNail(RenderListener listener) {
        mRenderListener = listener;
    }

    @Override
    public void acquireSurfaceTexture() {
        super.acquireSurfaceTexture();
        mAnimTexture = new Raw2DTexture(getWidth(), getHeight());
    }

    public void animate(int animOrientation) {
        switch (mAnimState) {
            case ANIM_TO_START:
                break;
            case ANIM_NONE:
                mAnimManager.setOrientation(animOrientation);
                // No break here. Continue to set the state and request for rendering.
            case ANIM_RUNNING:
                // Don't change the animation orientation during animation.
                mRenderListener.requestRender();
                mAnimState = ANIM_TO_START;
                break;
        }
    }

    public void directDraw(GLCanvas canvas, int x, int y, int width, int height) {
        super.draw(canvas, x, y, width, height);
    }

    @Override
    public void draw(GLCanvas canvas, int x, int y, int width, int height) {
        if (getSurfaceTexture() == null) return;
        if (!mVisible) setVisibility(true);

        switch (mAnimState) {
            case ANIM_TO_START:
                getSurfaceTexture().getTransformMatrix(mTextureTransformMatrix);
                Raw2DTexture.copy(canvas, mExtTexture, mAnimTexture);
                mAnimManager.startAnimation(x, y, width, height,
                        mTextureTransformMatrix);
                mAnimState = ANIM_RUNNING;
                // Continue to draw the animation. No break is needed here.
            case ANIM_RUNNING:
                if (mAnimManager.drawAnimation(canvas, this, mAnimTexture)) {
                    mRenderListener.requestRender();
                    break;
                }
                // No break here because we continue to the normal draw
                // procedure if the animation is not drawn.
                mAnimState = ANIM_NONE;
            case ANIM_NONE:
                super.draw(canvas, x, y, width, height);
                break;
        }
    }

    @Override
    public synchronized void noDraw() {
        setVisibility(false);
    }

    @Override
    public synchronized void recycle() {
        setVisibility(false);
    }

    @Override
    public synchronized void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if (mVisible) {
            // We need to ask for re-render if the SurfaceTexture receives a new
            // frame.
            mRenderListener.requestRender();
        }
    }

    private void setVisibility(boolean visible) {
        if (mVisible != visible) {
            mVisible = visible;
        }
    }
}