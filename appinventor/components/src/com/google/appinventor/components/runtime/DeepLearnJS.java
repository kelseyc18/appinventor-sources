// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Component for classifying images.
 */

@DesignerComponent(version = YaVersion.DEEPLEARNJS_COMPONENT_VERSION,
        category = ComponentCategory.EXPERIMENTAL, nonVisible = true,
        description = "Component for classifying images.")
@SimpleObject
@UsesAssets(fileNames = "deeplearnjs.html, deeplearn.js, deeplearn-main.js, deeplearn-squeeze.js")
@UsesPermissions(permissionNames = "android.permission.INTERNET")
public final class DeepLearnJS extends AndroidNonvisibleComponent implements Component {
    private static final String LOG_TAG = DeepLearnJS.class.getSimpleName();

    private final WebView webview;

    /**
     * Creates a new WebViewer component.
     *
     * @param form the container that this component will be placed in
     */
    public DeepLearnJS(Form form) {
        super(form);
        webview = new WebView(form);
        webview.getSettings().setJavaScriptEnabled(true);
        // adds a way to send strings to the javascript
        webview.addJavascriptInterface(new JsObject(), "DeepLearnJS");
        webview.loadUrl("file:///android_assets/deeplearnjs.html");
        Log.d(LOG_TAG, "Created DeepLearnJS component");
    }

    /**
     * Classifies the image at the given path.
     */
    @SimpleFunction
    public void Classify(final String image) {
        Log.d(LOG_TAG, "Entered Classify");
        Log.d(LOG_TAG, image);

        String imagePath = (image == null) ? "" : image;
        BitmapDrawable imageDrawable;
        Bitmap scaledImageBitmap = null;

        try {
            imageDrawable = MediaUtil.getBitmapDrawable(form.$form(), imagePath);
            scaledImageBitmap = Bitmap.createScaledBitmap(imageDrawable.getBitmap(), 227, 227, false);
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "Unable to load " + imagePath);
        }

        // compression format of PNG -> not lossy
        Bitmap immagex = scaledImageBitmap;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        immagex.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();

        String imageEncodedbase64String = Base64.encodeToString(b, 0);
        Log.d(LOG_TAG, "imageEncodedbase64String: " + imageEncodedbase64String);

        webview.evaluateJavascript("try { infer(" + imageEncodedbase64String +
                ") } catch(e) { DeepLearnJS.reportError(4, e.toString()); }", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String s) {
                Log.d(LOG_TAG, "Test result = " + s);
            }
        });
    }

    @SimpleFunction
    public void Train(final YailList data) {

    }

    @SimpleFunction
    public void TrainSample(final YailList sample) {

    }

    @SimpleFunction
    public void Clear() {

    }

    @SimpleFunction
    public void Save(final String file) {

    }

    @SimpleFunction
    public void Load(final String file) {

    }

    @SimpleFunction
    public void StartTraining(final String label) {

    }

    @SimpleFunction
    public void StopTraining() {

    }

    @SimpleEvent
    public void ClassifierReady() {
        EventDispatcher.dispatchEvent(this, "ClassifierReady");
    }

    @SimpleEvent
    public void AfterTraining(int responseCode, String message) {
        EventDispatcher.dispatchEvent(this, "AfterTraining", responseCode, message);
    }

    @SimpleEvent
    public void GotClassification(YailList result) {
        EventDispatcher.dispatchEvent(this, "GotClassification", result);
    }

    @SimpleEvent
    public void ClassificationFailed(int errorCode, String message) {
        EventDispatcher.dispatchEvent(this, "GotClassification", errorCode, message);
    }

    private class JsObject {
        @JavascriptInterface
        public void ready() {
            Log.d(LOG_TAG, "Entered ready");
            form.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ClassifierReady();
                }
            });
        }

        @JavascriptInterface
        public void reportResult(String result) {
            Log.d(LOG_TAG, "Entered reportResult: " + result);
            try {
                Log.d(LOG_TAG, "Entered try of reportResult");
                JSONObject object = new JSONObject(result);
                final YailList resultList = YailList.makeList(JsonUtil.getListFromJsonObject(object));
                form.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        GotClassification(resultList);
                    }
                });
            } catch (JSONException e) {
                Log.d(LOG_TAG, "Entered catch of reportResult");
                e.printStackTrace();
                reportError(1, e.getMessage());
            }
        }

        @JavascriptInterface
        public void reportError(final int code, final String message) {
            Log.d(LOG_TAG, "Entered reportError: " + message);
            form.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ClassificationFailed(code, message);
                }
            });
        }
    }
}

