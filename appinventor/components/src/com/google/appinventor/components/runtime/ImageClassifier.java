// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.FroyoUtil;
import com.google.appinventor.components.runtime.util.SdkLevel;
import com.google.appinventor.components.runtime.util.YailList;

/**
 * Component for classifying images.
 *
 * @author halabelson@google.com (Hal Abelson)
 */

@DesignerComponent(version = YaVersion.IMAGECLASSIFIER_COMPONENT_VERSION,
        category = ComponentCategory.MEDIA, nonVisible = true,
        description = "Component for classifying images.")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.INTERNET")
public final class ImageClassifier extends AndroidNonvisibleComponent implements Component {

    private final ComponentContainer container;
    private final WebView webview;

    // whether or not to follow links when they are tapped
    private boolean followLinks = true;

    // ignore SSL Errors (mostly certificate errors. When set
    // self signed certificates should work.

    private boolean ignoreSslErrors = false;

    // allows passing strings to javascript
    WebViewInterface wvInterface;

    /**
     * Creates a new WebViewer component.
     *
     * @param container container the component will be placed in
     */
    public ImageClassifier(ComponentContainer container) {
        super(container.$form());

        this.container = container;
        webview = new WebView(container.$context());
        resetWebViewClient();       // Set up the web view client
        webview.getSettings().setJavaScriptEnabled(true);
        webview.setFocusable(true);
        // adds a way to send strings to the javascript
        wvInterface = new WebViewInterface(webview.getContext());
        webview.addJavascriptInterface(wvInterface, "AppInventor");
        // enable pinch zooming and zoom controls
    }

    /**
     * Classifies the image at the given path.
     */
    @SimpleFunction
    public void Classify(final String image) {

    }

    @SimpleFunction
    public void Train(final YailList data) {

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
    public void GotClassification(int responseCode, YailList result, String message) {
        EventDispatcher.dispatchEvent(this, "GotClassification", responseCode, result, message);
    }

    // Create a class so we can override the default link following behavior.
    // The handler doesn't do anything on its own.  But returning true means that
    // this do nothing will override the default WebVew behavior.  Returning
    // false means to let the WebView handle the Url.  In other words, returning
    // true will not follow the link, and returning false will follow the link.
    private class WebViewerClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return !followLinks;
        }
    }

    private void resetWebViewClient() {
        if (SdkLevel.getLevel() >= SdkLevel.LEVEL_FROYO) {
            webview.setWebViewClient(FroyoUtil.getWebViewClient(ignoreSslErrors, followLinks, container.$form(), this));
        } else {
            webview.setWebViewClient(new WebViewerClient());
        }
    }

    /**
     * Allows the setting of properties to be monitored from the javascript
     * in the WebView
     */
    public class WebViewInterface {
        Context mContext;
        String webViewString;

        /**
         * Instantiate the interface and set the context
         */
        WebViewInterface(Context c) {
            mContext = c;
            webViewString = " ";
        }

        /**
         * Gets the web view string
         *
         * @return string
         */
        @JavascriptInterface
        public String getWebViewString() {
            return webViewString;
        }

        /**
         * Sets the web view string
         */
        public void setWebViewString(String newString) {
            webViewString = newString;
        }

    }
}

