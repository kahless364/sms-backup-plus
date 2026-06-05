/* Copyright (c) 2009 Christoph Studer <chstuder@gmail.com>
 * Copyright (c) 2010 Jan Berkel <jan.berkel@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zegoggles.smssync.utils;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * U-046 (BUG-014): Shared helper for AndroidX edge-to-edge window insets handling.
 *
 * <p>On API 35+, Android enforces edge-to-edge display: the app window extends behind the
 * status bar (top) and navigation bar (bottom). Without handling {@link WindowInsetsCompat},
 * content draws under the system bars — the toolbar title overlaps the status-bar clock and
 * the preferences list is occluded at the bottom.
 *
 * <p>This helper calls {@link WindowCompat#setDecorFitsSystemWindows} to opt into the
 * edge-to-edge layout, then installs an {@link androidx.core.view.OnApplyWindowInsetsListener}
 * that:
 * <ul>
 *   <li>Applies the TOP inset as top padding on the {@code toolbar} view, so the toolbar
 *       background still extends to the physical top edge (green bar behind the status bar)
 *       but its content (title, icons) sits below the status bar.</li>
 *   <li>Applies the BOTTOM inset as bottom padding on the {@code content} view, so
 *       scrollable/preference content clears the gesture/navigation bar.</li>
 *   <li>Applies LEFT and RIGHT insets as horizontal padding on both views, to handle
 *       landscape orientation and display cutouts correctly.</li>
 * </ul>
 *
 * <p>On API 21–34 the insets are zero, so no visual change occurs on older devices.
 *
 * <p>Usage:
 * <pre>{@code
 *   WindowInsetsUtil.applyEdgeToEdgeInsets(
 *       activity,
 *       toolbar,         // receives top + left/right padding
 *       contentView      // receives bottom + left/right padding
 *   );
 * }</pre>
 */
public final class WindowInsetsUtil {

    private WindowInsetsUtil() {
        // utility class — no instances
    }

    /**
     * Enables edge-to-edge display on {@code activity}'s window and applies system-bar
     * insets as padding to the provided {@code toolbar} and {@code content} views.
     *
     * <p>The listener is set on the {@code content} view (the root of the inset dispatch
     * tree that receives insets from the decor). The toolbar is padded from the same
     * insets object before the call returns {@link WindowInsetsCompat#CONSUMED} to prevent
     * child views from re-applying the same insets.
     *
     * @param window  the {@link android.view.Window} whose decor should fill system windows=false
     * @param toolbar the Toolbar view that receives TOP and LEFT/RIGHT padding
     * @param content the content/container view that receives BOTTOM and LEFT/RIGHT padding
     */
    public static void applyEdgeToEdgeInsets(
            @NonNull android.view.Window window,
            @NonNull final View toolbar,
            @NonNull final View content) {

        // Opt this window into the edge-to-edge layout. The system bars are drawn on top of
        // the app content. On API 21–34 this is a no-op for the forced-edge-to-edge change
        // (only API 35+ forces it), but it ensures our insets listener is consistent.
        WindowCompat.setDecorFitsSystemWindows(window, false);

        // Install the insets listener on the content view. The content view is the root that
        // the decor dispatches window insets into (it and all its children receive them).
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
            // Merge systemBars + displayCutout so landscape cutouts and notch devices work.
            final Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());

            // TOP inset → toolbar padding so the toolbar title sits below the status bar.
            // The toolbar background still paints behind the status bar (edge-to-edge look).
            // Preserve any start/end padding the toolbar already has on left/right sides.
            toolbar.setPadding(insets.left, insets.top, insets.right, 0);

            // BOTTOM inset → content padding so preferences/content clears the nav bar.
            // LEFT/RIGHT insets for landscape/cutout.
            view.setPadding(insets.left, 0, insets.right, insets.bottom);

            // Return CONSUMED so the child preference fragments do not re-apply these insets.
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
