/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.example.background

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.background.workers.BlurWorker
import com.example.background.workers.CleanupWorker
import com.example.background.workers.SaveImageToFileWorker

/**2
 * WorkRequest: This represents a request to do some work. You'll pass in your Worker as part of creating your WorkRequest.
 * When making the WorkRequest you can also specify things like Constraints on when the Worker should run.
 * **/

/**3
 * WorkManager: This class actually schedules your WorkRequest and makes it run.
 * It schedules WorkRequests in a way that spreads out the load on system resources, while honoring the constraints you specify.
 * **/
class BlurViewModel(application: Application) : AndroidViewModel(application) {

    internal var imageUri: Uri? = null
    internal var outputUri: Uri? = null

    private val workManager = WorkManager.getInstance(application)

    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     * @param blurLevel The amount to blur the image
     */
    internal fun applyBlur(blurLevel: Int) {
        // Add WorkRequest to Cleanup temporary images
        var continuation = workManager
                .beginWith(OneTimeWorkRequest
                        .from(CleanupWorker::class.java))

        // Add WorkRequests to blur the image the number of times requested
        for (i in 0 until blurLevel) {
            val blurBuilder = OneTimeWorkRequestBuilder<BlurWorker>()

            // Input the Uri if this is the first blur operation
            // After the first blur operation the input will be the output of previous
            // blur operations.
            if (i == 0) {
                blurBuilder.setInputData(createInputDataForUri())
            }

            continuation = continuation.then(blurBuilder.build())
        }

        // Add WorkRequest to save the image to the filesystem
        val save = OneTimeWorkRequest.Builder(SaveImageToFileWorker::class.java).build()

        continuation = continuation.then(save)

        // Actually start the work
        continuation.enqueue()
    }

    private fun uriOrNull(uriString: String?): Uri? {
        return if (!uriString.isNullOrEmpty()) {
            Uri.parse(uriString)
        } else {
            null
        }
    }

    /**
     * Setters
     */
    internal fun setImageUri(uri: String?) {
        imageUri = uriOrNull(uri)
    }

    internal fun setOutputUri(outputImageUri: String?) {
        outputUri = uriOrNull(outputImageUri)
    }

    /**input data**/
    private fun createInputDataForUri(): Data {
        val builder = Data.Builder()
        imageUri?.let {
            builder.putString(KEY_IMAGE_URI, imageUri.toString())
        }
        return builder.build()
    }
}
