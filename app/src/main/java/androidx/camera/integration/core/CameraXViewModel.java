/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.integration.core;

import android.app.Application;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.experimental.UseExperimental;
import androidx.camera.camera2.Camera2Config;
//import androidx.camera.lifecycle.ExperimentalCameraProviderConfiguration;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/** View model providing access to the camera */
public class CameraXViewModel extends AndroidViewModel {
    private static final String TAG = "CameraXViewModel";

    private static boolean sIsCameraProviderConfigured = false;
    private MutableLiveData<CameraProviderResult> mProcessCameraProviderLiveData;

    public CameraXViewModel(@NonNull Application application) {
        super(application);
    }

    /**
     * Returns a {@link LiveData} containing CameraX's {@link ProcessCameraProvider} once it has
     * been initialized.
     */
    @MainThread
    LiveData<CameraProviderResult> getCameraProvider() {
        if (mProcessCameraProviderLiveData == null) {
            mProcessCameraProviderLiveData = new MutableLiveData<>();
            //tryConfigureCameraProvider();
            try {
                ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                        ProcessCameraProvider.getInstance(getApplication());

                cameraProviderFuture.addListener(() -> {
                    try {
                        ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                        mProcessCameraProviderLiveData.setValue(
                                CameraProviderResult.fromProvider(cameraProvider));
                    } catch (ExecutionException e) {
                        if (!(e.getCause() instanceof CancellationException)) {
                            mProcessCameraProviderLiveData.setValue(
                                    CameraProviderResult.fromError(
                                            Objects.requireNonNull(e.getCause())));
                        }
                    } catch (InterruptedException e) {
                        throw new AssertionError("Unexpected thread interrupt.", e);
                    }
                }, ContextCompat.getMainExecutor(getApplication()));
            } catch (IllegalStateException e) {
                // Failure during ProcessCameraProvider.getInstance()
                mProcessCameraProviderLiveData.setValue(CameraProviderResult.fromError(e));
            }
        }
        return mProcessCameraProviderLiveData;
    }

//    @UseExperimental(markerClass = ExperimentalCameraProviderConfiguration.class)
//    @MainThread
//    private static void tryConfigureCameraProvider() {
//        if (!sIsCameraProviderConfigured) {
//            // Initialize with default configuration for now. Can be customized in the future.
//            ProcessCameraProvider.configureInstance(Camera2Config.defaultConfig());
//            Log.d(TAG, "Process camera provider initializing.");
//            sIsCameraProviderConfigured = true;
//        }
//    }

    /**
     * Class for wrapping success/error of initializing the {@link ProcessCameraProvider}.
     */
    public static final class CameraProviderResult {

        private final ProcessCameraProvider mProvider;
        private final Throwable mError;

        static CameraProviderResult fromProvider(@NonNull ProcessCameraProvider provider) {
            return new CameraProviderResult(provider, /*error=*/null);
        }

        static CameraProviderResult fromError(@NonNull Throwable error) {
            return new CameraProviderResult(/*provider=*/null, error);
        }

        private CameraProviderResult(@Nullable ProcessCameraProvider provider,
                @Nullable Throwable error) {
            mProvider = provider;
            mError = error;
        }

        /**
         * Returns {@code true} if this result contains a {@link ProcessCameraProvider}. Returns
         * {@code false} if it contains an error.
         */
        public boolean hasProvider() {
            return mProvider != null;
        }

        /**
         * Returns a {@link ProcessCameraProvider} if the result does not contain an error,
         * otherwise returns {@code null}.
         *
         * <p>Use {@link #hasProvider()} to check if this result contains a provider.
         */
        @Nullable
        public ProcessCameraProvider getProvider() {
            return mProvider;
        }

        /**
         * Returns a {@link Throwable} containing the error that prevented the
         * {@link ProcessCameraProvider} from being available. Returns {@code null} if no error
         * occurred.
         *
         * <p>Use {@link #hasProvider()} to check if this result contains a provider.
         */
        @Nullable
        public Throwable getError() {
            return mError;
        }
    }
}
