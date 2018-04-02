// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.PermissionRequest;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Component for classifying images.
 */

@DesignerComponent(version = YaVersion.COMPUTERVISION_COMPONENT_VERSION,
        category = ComponentCategory.EXPERIMENTAL, nonVisible = false,
        description = "Component for classifying images.")
@SimpleObject
@UsesAssets(fileNames = "computervision.html, computervision.js, group1-shard1of1, group10-shard1of1, group11-shard1of1, group12-shard1of1, group13-shard1of1, group14-shard1of1, group15-shard1of1, group16-shard1of1, group17-shard1of1, group18-shard1of1, group19-shard1of1, group2-shard1of1, group20-shard1of1, group21-shard1of1, group22-shard1of1, group23-shard1of1, group24-shard1of1, group25-shard1of1, group26-shard1of1, group27-shard1of1, group28-shard1of1, group29-shard1of1, group3-shard1of1, group30-shard1of1, group31-shard1of1, group32-shard1of1, group33-shard1of1, group34-shard1of1, group35-shard1of1, group36-shard1of1, group37-shard1of1, group38-shard1of1, group39-shard1of1, group4-shard1of1, group40-shard1of1, group41-shard1of1, group42-shard1of1, group43-shard1of1, group44-shard1of1, group45-shard1of1, group46-shard1of1, group47-shard1of1, group48-shard1of1, group49-shard1of1, group5-shard1of1, group50-shard1of1, group51-shard1of1, group52-shard1of1, group53-shard1of1, group54-shard1of1, group55-shard1of1, group6-shard1of1, group7-shard1of1, group8-shard1of1, group9-shard1of1, imagenet_classes.js, model.json, tfjs-0.6.1.js")
@UsesPermissions(permissionNames = "android.permission.INTERNET")
public final class ComputerVision extends AndroidViewComponent implements Component {
    private static final String LOG_TAG = ComputerVision.class.getSimpleName();

    private final WebView webview;
    private final Form form;
    private static TensorflowJSHTTPD httpdServer = null;

    /**
     * Creates a new WebViewer component.
     *
     * @param form the container that this component will be placed in
     */
    public ComputerVision(Form form) {
        super(form);
        this.form = form;
        startHTTPD();
        Log.d(LOG_TAG, form.$context().getFilesDir().getAbsolutePath());
        webview = new WebView(form);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setMediaPlaybackRequiresUserGesture(false);
        //webview.getSettings().setAllowUniversalAccessFromFileURLs(true);
        // adds a way to send strings to the javascript
        webview.addJavascriptInterface(new JsObject(), "ComputerVision");
        webview.setWebViewClient(new WebViewClient());
        webview.setWebChromeClient(new WebChromeClient() {
          @Override
          public void onPermissionRequest(PermissionRequest request) {
            String[] requestedResources = request.getResources();
            for (String r : requestedResources) {
              if (r.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                request.grant(request.getResources());
              }
            }
            //super.onPermissionRequest(request);
            Log.d(LOG_TAG, "onPermissionRequest called");
          }
        });
        //webview.loadUrl("https://kelseyc18.github.io/appinventor-computervision/");
        //webview.loadUrl("file:///android_asset/component/computervision.html");
        webview.loadUrl("http://localhost:8016/computervision.html");
        Log.d(LOG_TAG, "Created ComputerVision component");
        form.$add(this);
    }

    public void startHTTPD() {
        try {
            if (httpdServer == null) {
                httpdServer = new TensorflowJSHTTPD(8016, new File("/sdcard/AppInventor/assets/"), form.$context());
                Log.d(LOG_TAG, "startHTTPD");
            }
        } catch (IOException e) {
            Log.d(LOG_TAG, "startHTTPD not working: ");
            e.printStackTrace();
        }
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
            //scaledImageBitmap = Bitmap.createScaledBitmap(imageDrawable.getBitmap(), 227, 227, false);
            scaledImageBitmap = Bitmap.createScaledBitmap(imageDrawable.getBitmap(), 500, (int) (imageDrawable.getBitmap().getHeight() * 500.0 / imageDrawable.getBitmap().getWidth()), false);
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "Unable to load " + imagePath);
        }

        // compression format of PNG -> not lossy
        Bitmap immagex = scaledImageBitmap;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        immagex.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();

        String imageEncodedbase64String = Base64.encodeToString(b, 0).replace("\n", "");
        Log.d(LOG_TAG, "imageEncodedbase64String: " + imageEncodedbase64String);
        Log.d(LOG_TAG, "javascript: " + "try { classifyImageData(\"" + "placeholder" + "\"); } catch(e) { ComputerVision.reportError(4, e.toString()); }");

        webview.evaluateJavascript("try { classifyImageData(\"" + imageEncodedbase64String +
                "\"); } catch(e) { ComputerVision.reportError(4, e.toString()); }", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String s) {
                Log.d(LOG_TAG, "Test result = " + s);
            }
        });
    }

    @SimpleFunction
    public void StartVideo() {
      webview.evaluateJavascript("start();", null);
    }

    @SimpleFunction
    public void StopVideo() {
      webview.evaluateJavascript("stop();", null);
    }

    @SimpleFunction
    public void ToggleCameraFacingMode() {
      webview.evaluateJavascript("toggleCameraFacingMode();", null);
    }

    @SimpleFunction
    public void ClassifyVideoData() {
      webview.evaluateJavascript("classifyVideoData();", null);
    }

    @SimpleFunction
    public void ShowImage() {
      webview.evaluateJavascript("showImage();", null);
    }

    @SimpleFunction
    public void HideImage() {
      webview.evaluateJavascript("hideImage();", null);
    }

    @SimpleFunction
    public void SetInputMode(final String inputMode) {
      webview.evaluateJavascript("setInputMode(\"" + inputMode + "\");", null);
    }

    @SimpleFunction
    public void SetInputWidth(final int width) {
      webview.evaluateJavascript("setInputWidth(" + width + ");", null);
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
        EventDispatcher.dispatchEvent(this, "ClassificationFailed", errorCode, message);
    }

    @Override
    public View getView() {
        return webview;
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
        public void reportResult(final String result) {
            Log.d(LOG_TAG, "Entered reportResult: " + result);
            try {
                Log.d(LOG_TAG, "Entered try of reportResult");
                JSONArray list = new JSONArray(result);
                YailList intermediateList = YailList.makeList(JsonUtil.getListFromJsonArray(list));
                final List resultList = new ArrayList();
                for (int i = 0; i < intermediateList.size(); i++) {
                    resultList.add(YailList.makeList((List) intermediateList.getObject(i)));
                }
                form.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        GotClassification(YailList.makeList(resultList));
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

