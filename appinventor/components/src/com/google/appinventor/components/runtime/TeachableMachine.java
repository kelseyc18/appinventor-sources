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
import android.webkit.JavascriptInterface;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.PermissionRequest;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.UsesAssets;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.util.IOUtils;
import com.google.appinventor.components.runtime.util.JsonUtil;
import com.google.appinventor.components.runtime.util.MediaUtil;
import com.google.appinventor.components.runtime.util.YailList;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static android.net.Uri.encode;

/**
 * Component for teaching a machine to recognize different images.
 */

@DesignerComponent(version = YaVersion.TEACHABLEMACHINE_COMPONENT_VERSION,
        category = ComponentCategory.EXPERIMENTAL,
        description = "Component for teaching a machine to recognize different images.")
@SimpleObject
@UsesAssets(fileNames = "teachablemachine.html, teachablemachine.js, group1-shard1of1, group10-shard1of1, group11-shard1of1, group12-shard1of1, group13-shard1of1, group14-shard1of1, group15-shard1of1, group16-shard1of1, group17-shard1of1, group18-shard1of1, group19-shard1of1, group2-shard1of1, group20-shard1of1, group21-shard1of1, group22-shard1of1, group23-shard1of1, group24-shard1of1, group25-shard1of1, group26-shard1of1, group27-shard1of1, group28-shard1of1, group29-shard1of1, group3-shard1of1, group30-shard1of1, group31-shard1of1, group32-shard1of1, group33-shard1of1, group34-shard1of1, group35-shard1of1, group36-shard1of1, group37-shard1of1, group38-shard1of1, group39-shard1of1, group4-shard1of1, group40-shard1of1, group41-shard1of1, group42-shard1of1, group43-shard1of1, group44-shard1of1, group45-shard1of1, group46-shard1of1, group47-shard1of1, group48-shard1of1, group49-shard1of1, group5-shard1of1, group50-shard1of1, group51-shard1of1, group52-shard1of1, group53-shard1of1, group54-shard1of1, group55-shard1of1, group6-shard1of1, group7-shard1of1, group8-shard1of1, group9-shard1of1, model.json, tfjs-0.10.3.js")
@UsesPermissions(permissionNames = "android.permission.INTERNET, android.permission.CAMERA")
public final class TeachableMachine extends AndroidViewComponent implements Component {
  private static final String LOG_TAG = TeachableMachine.class.getSimpleName();
  private static final String MODEL_DIRECTORY = "/sdcard/AppInventor/assets/TeachableMachine/";

  private static final String MODEL_PREFIX = "https://storage.googleapis.com/tfjs-models/tfjs/mobilenet_v1_0.25_224/";

  private final WebView webview;
  private final Form form;

  public TeachableMachine(ComponentContainer container) {
    super(container);
    this.form = container.$form();
    webview = new WebView(container.$context());
    webview.getSettings().setJavaScriptEnabled(true);
    webview.getSettings().setMediaPlaybackRequiresUserGesture(false);
    // adds a way to send strings to the javascript
    webview.addJavascriptInterface(new JsObject(), "TeachableMachine");
    webview.setWebViewClient(new WebViewClient() {
      @Override
      public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        Log.d(LOG_TAG, "shouldInterceptRequest called");
        if (url.contains(MODEL_PREFIX)) {
          Log.d(LOG_TAG, "overriding " + url);
          try {
            InputStream inputStream = form.$context().getAssets().open("component/" + url.substring(MODEL_PREFIX.length()));
            if (url.endsWith(".json")) {
              return new WebResourceResponse("application/json", "UTF-8", inputStream);
            } else {
              return new WebResourceResponse("application/octet-stream", "binary", inputStream);
            }
          } catch (IOException e) {
            e.printStackTrace();
            return super.shouldInterceptRequest(view, url);
          }
        }
        Log.d(LOG_TAG, url);
        return super.shouldInterceptRequest(view, url);
      }
    });
    webview.setWebChromeClient(new WebChromeClient() {
      @Override
      public void onPermissionRequest(PermissionRequest request) {
        Log.d(LOG_TAG, "onPermissionRequest called");
        String[] requestedResources = request.getResources();
        for (String r : requestedResources) {
          if (r.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
            request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
          }
        }
      }
    });
    webview.loadUrl("file:///android_asset/component/teachablemachine.html");
    Log.d(LOG_TAG, "Created TeachableMachine component");
    container.$add(this);
  }

  @SimpleFunction(description = "Toggles between user-facing and environment-facing camera.")
  public void ToggleCameraFacingMode() {
    webview.evaluateJavascript("toggleCameraFacingMode();", null);
  }

  @SimpleFunction(description = "Starts training machine to associate images from the camera with the provided label.")
  public void StartTraining(final String label) {
    webview.evaluateJavascript("startTraining(\"" + encode(label) + "\");", null);
  }

  @SimpleFunction(description = "Stops collecting images from the camera to train machine.")
  public void StopTraining() {
    webview.evaluateJavascript("stopTraining();", null);
  }

  @SimpleFunction(description = "Clears training data associated with provided label.")
  public void Clear(final String label) {
    webview.evaluateJavascript("clear(\"" + encode(label) + "\");", null);
  }

  @SimpleFunction(description = "Saves model (current set of samples and labels) with provided name.")
  public void SaveModel(final String name) {
    webview.evaluateJavascript("saveModel(\"" + encode(name) + "\");", null);
  }

  @SimpleFunction(description = "Loads model with provided name.")
  public void LoadModel(final String name) {
    try {
      String model = new String(Files.readAllBytes(Paths.get(MODEL_DIRECTORY + name)));
      webview.evaluateJavascript("loadModel(\"" + encode(name) + "\", \"" + encode(model) + "\");", null);
    } catch (IOException e) {
      e.printStackTrace();
      Error("LoadModel: problem reading model with name " + name);
    }
  }

  @SimpleEvent(description = "Event indicating that the classifier is ready.")
  public void ClassifierReady() {
    EventDispatcher.dispatchEvent(this, "ClassifierReady");
  }

  @SimpleEvent(description = "Event indicating that sample counts have been updated.<br>Result is of the form [[label1, sampleCount1], [label2, sampleCount2], ..., [labelN, sampleCountN]].")
  public void GotSampleCounts(YailList result) {
    EventDispatcher.dispatchEvent(this, "GotSampleCounts", result);
  }

  @SimpleEvent(description = "Event indicating that confidences have been updated.<br>Result is of the form [[label1, confidence1], [label2, confidence2], ..., [labelN, confidenceN]].")
  public void GotConfidences(YailList result) {
    EventDispatcher.dispatchEvent(this, "GotConfidences", result);
  }

  @SimpleEvent(description = "Event indicating that classification has finished successfully. Label is the one with the highest confidence.")
  public void GotClassification(String label) {
    EventDispatcher.dispatchEvent(this, "GotClassification", label);
  }

  @SimpleEvent(description = "Event indicating that SaveModel with the specified name has completed successfully.")
  public void DoneSavingModel(String name) {
    EventDispatcher.dispatchEvent(this, "DoneSavingModel", name);
  }

  @SimpleEvent(description = "Event indicating that LoadModel with the specified name has completed successfully.")
  public void DoneLoadingModel(String name) {
    EventDispatcher.dispatchEvent(this, "DoneLoadingModel", name);
  }

  @SimpleEvent(description = "Event indicating that an error has occurred.")
  public void Error(String message) {
    EventDispatcher.dispatchEvent(this, "Error", message);
  }

  private void saveModel(String name, String model) {
    // save to file system
    String path = MODEL_DIRECTORY + name;
    new File(MODEL_DIRECTORY).mkdirs();
    PrintStream out = null;
    try {
      out = new PrintStream(new FileOutputStream(path));
      out.print(model);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      Error("SaveModel: problem saving model with name " + name);
    } finally {
      IOUtils.closeQuietly(LOG_TAG, out);
    }
    DoneSavingModel(name);
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
    public void gotSampleCounts(final String result) {
      Log.d(LOG_TAG, "Entered gotSampleCounts: " + result);
      form.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          try {
            JSONArray list = new JSONArray(result);
            YailList intermediateList = YailList.makeList(JsonUtil.getListFromJsonArray(list));
            final List resultList = new ArrayList();
            for (int i = 0; i < intermediateList.size(); i++) {
              resultList.add(YailList.makeList((List) intermediateList.getObject(i)));
            }
            GotSampleCounts(YailList.makeList(resultList));
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }
      });
    }

    @JavascriptInterface
    public void gotConfidences(final String result) {
      Log.d(LOG_TAG, "Entered gotConfidences: " + result);
      form.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          try {
            JSONArray list = new JSONArray(result);
            YailList intermediateList = YailList.makeList(JsonUtil.getListFromJsonArray(list));
            final List resultList = new ArrayList();
            for (int i = 0; i < intermediateList.size(); i++) {
              resultList.add(YailList.makeList((List) intermediateList.getObject(i)));
            }
            GotConfidences(YailList.makeList(resultList));
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
    public void gotSavedModel(final String name, final String model) {
      Log.d(LOG_TAG, "Entered gotSavedModel: " + name);
      form.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          saveModel(name, model);
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
    public void error(final String message) {
      Log.d(LOG_TAG, "Entered error: message = " + message);
      form.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          Error(message);
        }
      });
    }
  }
}
