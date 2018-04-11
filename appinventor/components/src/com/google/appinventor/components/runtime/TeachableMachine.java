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
import android.view.View;
import android.webkit.*;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.util.JsonUtil;
import com.google.appinventor.components.runtime.util.MediaUtil;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.appinventor.components.runtime.util.*;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static android.net.Uri.encode;

/**
 * Component for classifying images.
 */

@DesignerComponent(version = YaVersion.TEACHABLEMACHINE_COMPONENT_VERSION,
        category = ComponentCategory.EXPERIMENTAL, nonVisible = false,
        description = "Component for classifying images.")
@SimpleObject
@UsesAssets(fileNames = "teachablemachine.html, teachablemachine.js, group1-shard1of1, group10-shard1of1, group11-shard1of1, group12-shard1of1, group13-shard1of1, group14-shard1of1, group15-shard1of1, group16-shard1of1, group17-shard1of1, group18-shard1of1, group19-shard1of1, group2-shard1of1, group20-shard1of1, group21-shard1of1, group22-shard1of1, group23-shard1of1, group24-shard1of1, group25-shard1of1, group26-shard1of1, group27-shard1of1, group28-shard1of1, group29-shard1of1, group3-shard1of1, group30-shard1of1, group31-shard1of1, group32-shard1of1, group33-shard1of1, group34-shard1of1, group35-shard1of1, group36-shard1of1, group37-shard1of1, group38-shard1of1, group39-shard1of1, group4-shard1of1, group40-shard1of1, group41-shard1of1, group42-shard1of1, group43-shard1of1, group44-shard1of1, group45-shard1of1, group46-shard1of1, group47-shard1of1, group48-shard1of1, group49-shard1of1, group5-shard1of1, group50-shard1of1, group51-shard1of1, group52-shard1of1, group53-shard1of1, group54-shard1of1, group55-shard1of1, group6-shard1of1, group7-shard1of1, group8-shard1of1, group9-shard1of1, model.json, tfjs-0.8.0.js")
@UsesPermissions(permissionNames = "android.permission.INTERNET")
public final class TeachableMachine extends AndroidViewComponent implements Component {
    private static final String LOG_TAG = TeachableMachine.class.getSimpleName();
    private static final String MODEL_DIRECTORY = "/sdcard/AppInventor/assets/TeachableMachine/";

    private static final int ERROR_FILE_EXCEPTION = -3;

    private final WebView webview;
    private final Form form;
    private static TensorflowJSHTTPD httpdServer = null;

    /**
     * Creates a new WebViewer component.
     *
     * @param form the container that this component will be placed in
     */
    public TeachableMachine(ComponentContainer container) {
        super(container);
        this.form = container.$form();
        startHTTPD();
        webview = new WebView(container.$context());
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setMediaPlaybackRequiresUserGesture(false);
        // adds a way to send strings to the javascript
        webview.addJavascriptInterface(new JsObject(), "TeachableMachine");
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
        //webview.loadUrl("https://kevin-vr.github.io/teachable-machine/");
        //webview.loadUrl("file:///android_asset/component/teachable-machine.html");
        webview.loadUrl("http://localhost:8017/teachablemachine.html");
        Log.d(LOG_TAG, "Created TeachableMachine component");
        container.$add(this);
    }

    public void startHTTPD() {
        try {
            if (httpdServer == null) {
                httpdServer = new TensorflowJSHTTPD(8017, new File("/sdcard/AppInventor/assets/"), form.$context());
                Log.d(LOG_TAG, "startHTTPD");
            }
        } catch (IOException e) {
            Log.d(LOG_TAG, "startHTTPD not working: ");
            e.printStackTrace();
        }
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER,
        defaultValue = "3")
    @SimpleProperty(userVisible = false, description = "No description")
    public void NumberOfLabels(int num) {
      Log.d(LOG_TAG, "NumberOfLabels is " + String.valueOf(num));
      if (num == 3) {
        webview.loadUrl("http://localhost:8017/teachablemachine.html");
      } else {
        webview.loadUrl("http://localhost:8017/teachablemachine.html?n=" + String.valueOf(num));
      }
    }

    @SimpleFunction
    public void ToggleCameraFacingMode() {
      webview.evaluateJavascript("toggleCameraFacingMode();", null);
    }

    @SimpleFunction
    public void SetInputWidth(final int width) {
      webview.evaluateJavascript("setInputWidth(" + width + ");", null);
    }

    @SimpleFunction
    public void StartTraining(final String label) {
        webview.evaluateJavascript("startTraining(\"" + encode(label) + "\");", null);
    }

    @SimpleFunction
    public void StopTraining() {
        webview.evaluateJavascript("stopTraining();", null);
    }

    @SimpleFunction
    public void Clear(final String label) {
        webview.evaluateJavascript("clear(\"" + encode(label) + "\");", null);
    }

    @SimpleFunction
    public void SaveModel(final String filename) {
        webview.evaluateJavascript("saveModel(\"" + encode(filename) + "\");", null);
    }

    @SimpleFunction
    public void LoadModel(final String filename) {
        try {
            String model = new String(Files.readAllBytes(Paths.get(MODEL_DIRECTORY + filename)));
            webview.evaluateJavascript("loadModel(\"" + encode(filename) + "\", \"" + encode(model) + "\");", null);
        } catch (IOException e) {
            e.printStackTrace();
            Error(ERROR_FILE_EXCEPTION, e.getMessage());
        }
    }

    @SimpleEvent
    public void ClassifierReady() {
        EventDispatcher.dispatchEvent(this, "ClassifierReady");
    }

    @SimpleEvent
    public void GotSampleCounts(YailList labels, YailList sampleCounts) {
        EventDispatcher.dispatchEvent(this, "GotSampleCounts", labels, sampleCounts);
    }

    @SimpleEvent
    public void GotConfidences(YailList labels, YailList confidences) {
        EventDispatcher.dispatchEvent(this, "GotConfidences", labels, confidences);
    }

    @SimpleEvent
    public void GotClassification(String label) {
        EventDispatcher.dispatchEvent(this, "GotClassification", label);
    }

    @SimpleEvent
    public void DoneSavingModel(String filename) {
        EventDispatcher.dispatchEvent(this, "DoneSavingModel", filename);
    }

    @SimpleEvent
    public void DoneLoadingModel(String filename) {
        EventDispatcher.dispatchEvent(this, "DoneLoadingModel", filename);
    }

    @SimpleEvent
    public void Error(int errorCode, String message) {
        EventDispatcher.dispatchEvent(this, "Error", errorCode, message);
    }

    private void saveModel(String filename, String model) {
        // save to file system
        String path = MODEL_DIRECTORY + filename;
        new File(MODEL_DIRECTORY).mkdirs();
        PrintStream out = null;
        try {
            out = new PrintStream(new FileOutputStream(path));
            out.print(model);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Error(ERROR_FILE_EXCEPTION, e.getMessage());
        } finally {
            IOUtils.closeQuietly(LOG_TAG, out);
        }
        DoneSavingModel(filename);
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
        public void gotSampleCounts(final String labels, final String sampleCounts) {
            Log.d(LOG_TAG, "Entered gotSampleCounts: " + labels + " " + sampleCounts);
            form.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONArray list1 = new JSONArray(labels);
                        JSONArray list2 = new JSONArray(sampleCounts);
                        GotSampleCounts(YailList.makeList(JsonUtil.getListFromJsonArray(list1)), YailList.makeList(JsonUtil.getListFromJsonArray(list2)));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @JavascriptInterface
        public void gotConfidences(final String labels, final String confidences) {
            Log.d(LOG_TAG, "Entered gotConfidences: " + labels + " " + confidences);
            form.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONArray list1 = new JSONArray(labels);
                        JSONArray list2 = new JSONArray(confidences);
                        GotConfidences(YailList.makeList(JsonUtil.getListFromJsonArray(list1)), YailList.makeList(JsonUtil.getListFromJsonArray(list2)));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @JavascriptInterface
        public void gotClassification(final String label) {
            Log.d(LOG_TAG, "Entered gotClassification: " + label);
            form.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    GotClassification(label);
                }
            });
        }

        @JavascriptInterface
        public void gotSavedModel(final String filename, final String model) {
            Log.d(LOG_TAG, "Entered gotSavedModel: " + filename);
            form.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    saveModel(filename, model);
                }
            });
        }

        @JavascriptInterface
        public void doneLoadingModel(final String label) {
            Log.d(LOG_TAG, "Entered doneLoadingModel: " + label);
            form.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DoneLoadingModel(label);
                }
            });
        }

        @JavascriptInterface
        public void error(final int errorCode, final String message) {
            Log.d(LOG_TAG, "Entered error: errorCode = " + String.valueOf(errorCode) + " message = " + message);
            form.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Error(errorCode, message);
                }
            });
        }
    }
}

