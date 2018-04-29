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
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.UsesAssets;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.MediaUtil;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Component that recognizes text.
 */

@DesignerComponent(version = YaVersion.OCR_COMPONENT_VERSION,
        category = ComponentCategory.EXPERIMENTAL,
        description = "Component that recognizes text.")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.INTERNET, android.permission.CAMERA")
public final class OCR extends AndroidViewComponent implements Component {
  private static final String LOG_TAG = OCR.class.getSimpleName();
  private static final int IMAGE_WIDTH = 500;
  public static final int IMAGE_QUALITY = 100;
  public static final int PORT = 8018;

  private final WebView webview;
  private final Form form;

  public OCR(ComponentContainer container) {
    super(container);
    this.form = container.$form();
    webview = new WebView(container.$context());
    webview.getSettings().setJavaScriptEnabled(true);
    webview.getSettings().setMediaPlaybackRequiresUserGesture(false);
    // adds a way to send strings to the javascript
    webview.addJavascriptInterface(new JsObject(), "OCR");
    webview.setWebViewClient(new WebViewClient());
    webview.setWebChromeClient(new WebChromeClient() {
      @Override
      public void onPermissionRequest(PermissionRequest request) {
        String[] requestedResources = request.getResources();
        for (String r : requestedResources) {
          if (r.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
            request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
          }
        }
        Log.d(LOG_TAG, "onPermissionRequest called");
      }
    });
    webview.loadUrl("https://kelseyc18.github.io/appinventor-ocr/");
    Log.d(LOG_TAG, "Created OCR component");
    container.$add(this);
  }

  @SimpleFunction(description = "Performs classification on the image at the given path and triggers the GotClassification event when classification is finished successfully.")
  public void RecognizeImageData(final String image) {
    Log.d(LOG_TAG, "Entered Classify");
    Log.d(LOG_TAG, image);

    String imagePath = (image == null) ? "" : image;
    BitmapDrawable imageDrawable;
    Bitmap scaledImageBitmap = null;

    try {
      imageDrawable = MediaUtil.getBitmapDrawable(form.$form(), imagePath);
      scaledImageBitmap = Bitmap.createScaledBitmap(imageDrawable.getBitmap(), IMAGE_WIDTH, (int) (imageDrawable.getBitmap().getHeight() * ((float) IMAGE_WIDTH) / imageDrawable.getBitmap().getWidth()), false);
    } catch (IOException ioe) {
      Log.e(LOG_TAG, "Unable to load " + imagePath);
    }

    // compression format of PNG -> not lossy
    Bitmap immagex = scaledImageBitmap;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    immagex.compress(Bitmap.CompressFormat.PNG, IMAGE_QUALITY, baos);
    byte[] b = baos.toByteArray();

    String imageEncodedbase64String = Base64.encodeToString(b, 0).replace("\n", "");
    Log.d(LOG_TAG, "imageEncodedbase64String: " + imageEncodedbase64String);

    webview.evaluateJavascript("recognizeImageData(\"" + imageEncodedbase64String + "\");", null);
  }

  @SimpleFunction(description = "Sets a language. Default is English.")
  public void SetLanguage(final String language) {
    webview.evaluateJavascript("setLanguage(\"" + language + "\");", null);
  }

  @SimpleFunction(description = "Toggles between user-facing and environment-facing camera.")
  public void ToggleCameraFacingMode() {
    webview.evaluateJavascript("toggleCameraFacingMode();", null);
  }

  @SimpleFunction(description = "Performs classification on current video frame and triggers the GotClassification event when classification is finished successfully.")
  public void RecognizeVideoData() {
    webview.evaluateJavascript("recognizeVideoData();", null);
  }

  @SimpleFunction(description = "Sets the input mode to image if inputMode is \"image\" or video if inputMode is \"video\".")
  public void SetInputMode(final String inputMode) {
    webview.evaluateJavascript("setInputMode(\"" + inputMode + "\");", null);
  }

  @SimpleEvent(description = "Event indicating that the classifier is ready.")
  public void ClassifierReady() {
    EventDispatcher.dispatchEvent(this, "ClassifierReady");
  }

  @SimpleEvent(description = "temp")
  public void GotProgress(String result) {
    EventDispatcher.dispatchEvent(this, "GotProgress", result);
  }

  @SimpleEvent(description = "Event indicating that classification has finished successfully. Result is of the form [[class1, confidence1], [class2, confidence2], ..., [class10, confidence10]].")
  public void GotText(String result) {
    EventDispatcher.dispatchEvent(this, "GotText", result);
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
    public void reportProgress(final String progress) {
      Log.d(LOG_TAG, "Entered reportProgress: " + progress);
      form.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          GotProgress(progress);
        }
      });
    }

    @JavascriptInterface
    public void reportResult(final String result) {
      Log.d(LOG_TAG, "Entered reportResult: " + result);
      form.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          GotText(result);
        }
      });
      /*
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
        ClassificationFailed();
      }
      */
    }
  }
}

