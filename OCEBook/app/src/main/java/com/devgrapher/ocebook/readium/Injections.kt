package com.devgrapher.ocebook.readium

/**
 * Created by Brent on 2/16/17.
 */

object Injections {
    val ASSET_PREFIX = "file:///android_asset/readium-shared-js/"

    // Installs "hook" function so that top-level window (application) can later
    // inject the window.navigator.epubReadingSystem into this HTML document's
    // iframe
    val INJECT_EPUB_RSO_SCRIPT_1 = "" +
            "window.readium_set_epubReadingSystem = function (obj) {" +
            "\nwindow.navigator.epubReadingSystem = obj;" +
            "\nwindow.readium_set_epubReadingSystem = undefined;" +
            "\nvar el1 = document.getElementById(\"readium_epubReadingSystem_inject1\");" +
            "\nif (el1 && el1.parentNode) { el1.parentNode.removeChild(el1); }" +
            "\nvar el2 = document.getElementById(\"readium_epubReadingSystem_inject2\");" +
            "\nif (el2 && el2.parentNode) { el2.parentNode.removeChild(el2); }" +
            "\n};"

    // Iterate top-level iframes, inject global
    // window.navigator.epubReadingSystem if the expected hook function exists (
    // readium_set_epubReadingSystem() ).
    val INJECT_EPUB_RSO_SCRIPT_2 = "" +
            "var epubRSInject =\nfunction(win) {" +
            "\nvar ret = '';" +
            "\nret += win.location.href;" +
            "\nret += ' ---- ';" +
    // "\nret += JSON.stringify(win.navigator.epubReadingSystem);" +
    // "\nret += ' ---- ';" +
            "\nif (win.frames)" +
            "\n{" +
            "\nfor (var i = 0; i < win.frames.length; i++)" +
            "\n{" +
            "\nvar iframe = win.frames[i];" +
            "\nret += ' IFRAME ';" +
            "\nif (iframe.readium_set_epubReadingSystem)" +
            "\n{" +
            "\nret += ' EPBRS ';" +
            "\niframe.readium_set_epubReadingSystem(window.navigator.epubReadingSystem);" +
            "\n}" + "\nret += epubRSInject(iframe);" + "\n}" + "\n}" +
            "\nreturn ret;" + "\n};" + "\nepubRSInject(window);"

    // Script tag to inject the "hook" function installer script, added to the
    // head of every epub iframe document
    val INJECT_HEAD_EPUB_RSO_1 = "" +
            "<script id=\"readium_epubReadingSystem_inject1\" type=\"text/javascript\">\n" +
            "//<![CDATA[\n" + INJECT_EPUB_RSO_SCRIPT_1 + "\n" + "//]]>\n" +
            "</script>"
    // Script tag that generates an HTTP request to a fake script => triggers
    // push of window.navigator.epubReadingSystem into this HTML document's
    // iframe
    val INJECT_HEAD_EPUB_RSO_2 = "" +
            "<script id=\"readium_epubReadingSystem_inject2\" type=\"text/javascript\" " +
            "src=\"/%d/readium_epubReadingSystem_inject.js\"> </script>"
    // Script tag to load the mathjax script payload, added to the head of epub
    // iframe documents, only if <math> tags are detected
    val INJECT_HEAD_MATHJAX = "<script type=\"text/javascript\" src=\"/readium_MathJax.js\"> </script>"

    // Location of payloads in the asset folder
    val PAYLOAD_MATHJAX_ASSET = "reader-payloads/MathJax.js"
    val PAYLOAD_ANNOTATIONS_CSS_ASSET = "reader-payloads/annotations.css"
}
