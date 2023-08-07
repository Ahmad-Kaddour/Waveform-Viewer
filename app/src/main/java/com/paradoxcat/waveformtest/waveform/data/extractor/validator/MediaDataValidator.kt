package com.paradoxcat.waveformtest.waveform.data.extractor.validator

import android.media.MediaExtractor

/** Interface to validate the passed media data **/
interface MediaDataValidator {
    /**
     * @param mediaExtractor -- media extractor object of the file to be validated.
     **/
    suspend fun isValid(mediaExtractor: MediaExtractor): Boolean
}