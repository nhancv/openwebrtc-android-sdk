/*
 * Copyright (c) 2015, Ericsson AB.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */
package com.ericsson.research.owr.sdk;

import android.util.Log;

import com.ericsson.research.owr.AudioRenderer;
import com.ericsson.research.owr.MediaSource;
import com.ericsson.research.owr.MediaType;

import java.util.Arrays;
import java.util.List;

public class SimpleStreamSet extends StreamSet {
    private static final String TAG = "SimpleStreamSet";

    private final boolean mSendVideo;
    private final boolean mSendAudio;
    private final boolean mRevcVideo;
    private final boolean mRevcAudio;

    private final AudioRenderer mAudioRenderer;

    private final SimpleMediaStream mAudioStream;
    private final SimpleMediaStream mVideoStream;

    private VideoSourceProvider mRemoteVideoSourceProvider = new VideoSourceProvider();

    private SimpleStreamSet(MediaSourceProvider audioSourceProvider, MediaSourceProvider videoSourceProvider,
                            boolean sendAudio, boolean sendVideo) {
        this(audioSourceProvider, videoSourceProvider, sendAudio, sendVideo, true, true);
    }

    private SimpleStreamSet(MediaSourceProvider audioSourceProvider, MediaSourceProvider videoSourceProvider,
                            boolean sendAudio, boolean sendVideo, boolean revcAudio, boolean revcVideo) {
        mSendAudio = sendAudio;
        mSendVideo = sendVideo;
        mRevcAudio = revcAudio;
        mRevcVideo = revcVideo;

        mAudioRenderer = new AudioRenderer();

        mAudioStream = new SimpleMediaStream(false);
        mVideoStream = new SimpleMediaStream(true);

        videoSourceProvider.addMediaSourceListener(new MediaSourceListener() {
            @Override
            public void setMediaSource(final MediaSource mediaSource) {
                mVideoStream.setMediaSource(mediaSource);
            }
        });

        audioSourceProvider.addMediaSourceListener(new MediaSourceListener() {
            @Override
            public void setMediaSource(final MediaSource mediaSource) {
                mAudioStream.setMediaSource(mediaSource);
            }
        });
    }

    /**
     * Creates a configuration for setting up a basic audio/video call.
     *
     * @param sendAudio true if audio should be sent, audio may still be received
     * @param sendVideo true if video should be sent, video may still be received
     * @return a new RtcConfig with a simple audio/video call configuration
     */
    public static SimpleStreamSet defaultConfig(boolean sendAudio, boolean sendVideo) {
        return new SimpleStreamSet(MicrophoneSource.getInstance(), CameraSource.getInstance(), sendAudio, sendVideo);
    }

    /**
     * Creates a configuration for setting up a basic audio/video call.
     *
     * @param sendAudio true if audio should be sent
     * @param sendVideo true if video should be sent
     * @param revcAudio true if video should be received
     * @param revcVideo true if video should be received
     * @return a new RtcConfig with a simple audio/video call configuration
     */
    public static SimpleStreamSet defaultConfig(boolean sendAudio, boolean sendVideo, boolean revcAudio, boolean revcVideo) {
        return new SimpleStreamSet(MicrophoneSource.getInstance(), CameraSource.getInstance(), sendAudio, sendVideo, revcAudio, revcVideo);
    }

    public VideoView createRemoteView() {
        return new VideoViewImpl(mRemoteVideoSourceProvider, 0, 0, 0);
    }

    @Override
    protected List<? extends Stream> getStreams() {
        return Arrays.asList(mAudioStream, mVideoStream);
    }

    /**
     * @return the current audio renderer pipeline graph in dot format.
     */
    public String dumpPipelineGraph() {
        return mAudioRenderer.getDotData();
    }

    private class VideoSourceProvider implements MediaSourceProvider {
        private MediaSourceListenerSet set = new MediaSourceListenerSet();

        public void notifyListeners(MediaSource mediaSource) {
            set.notifyListeners(mediaSource);
        }

        @Override
        public void addMediaSourceListener(final MediaSourceListener listener) {
            set.addListener(listener);
        }
    }

    private class SimpleMediaStream extends MediaStream {
        private final String mId;
        private final boolean mIsVideo;
        private MediaSource mMediaSource;
        private MediaSourceDelegate mMediaSourceDelegate;

        private SimpleMediaStream(boolean isVideo) {
            mIsVideo = isVideo;
            mId = Utils.randomString(27);
        }

        @Override
        protected String getId() {
            return mId;
        }

        @Override
        protected MediaType getMediaType() {
            return mIsVideo ? MediaType.VIDEO : MediaType.AUDIO;
        }

        @Override
        protected boolean wantSend() {
            return mIsVideo ? mSendVideo : mSendAudio;
        }

        @Override
        protected boolean wantReceive() {
            return mIsVideo ? mRevcVideo : mRevcAudio;
        }

        @Override
        protected void onRemoteMediaSource(final MediaSource mediaSource) {
            if (mIsVideo) {
                mRemoteVideoSourceProvider.notifyListeners(mediaSource);
            } else {
                mAudioRenderer.setSource(mediaSource);
            }
        }

        @Override
        protected synchronized void setMediaSourceDelegate(final MediaSourceDelegate mediaSourceDelegate) {
            mMediaSourceDelegate = mediaSourceDelegate;
            if (mMediaSource != null && mediaSourceDelegate != null) {
                mediaSourceDelegate.setMediaSource(mMediaSource);
            }
        }

        @Override
        public void setStreamMode(final StreamMode mode) {
            Log.i(TAG, (mIsVideo ? "video" : "audio") + " stream mode set: " + mode.name());
        }

        public synchronized void setMediaSource(final MediaSource mediaSource) {
            mMediaSource = mediaSource;
            if (mMediaSourceDelegate != null) {
                mMediaSourceDelegate.setMediaSource(mediaSource);
            }
        }
    }
}
