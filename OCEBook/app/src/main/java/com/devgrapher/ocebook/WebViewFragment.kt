package com.devgrapher.ocebook

import android.annotation.TargetApi
import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

import com.devgrapher.ocebook.model.OpenPageRequest
import com.devgrapher.ocebook.model.PaginationInfo
import com.devgrapher.ocebook.model.ViewerSettings
import com.devgrapher.ocebook.readium.ObjectHolder
import com.devgrapher.ocebook.readium.ReadiumContext
import com.devgrapher.ocebook.util.Laz
import com.devgrapher.ocebook.util.PaginationPrefs
import com.devgrapher.ocebook.util.WebViewMotionHandler

import org.readium.sdk.android.Container

import java.io.IOException
import java.io.InputStream

import kotlinx.android.synthetic.main.fragment_web_view.*


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [WebViewFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [WebViewFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
open class WebViewFragment : Fragment() {
    private val TAG = WebViewFragment::class.java.toString()
    private var mListener: OnFragmentInteractionListener? = null
    private var mContainer: Container? = null
    private var mLastPageinfo: PaginationPrefs? = null

    // protected to be accessed in HiddenRendererFragment
    protected val mReadiumCtx: ReadiumContext by lazy { initReadium() }
    protected val mViewerSettings = ViewerSettings(
            ViewerSettings.SyntheticSpreadMode.AUTO,
            ViewerSettings.ScrollMode.AUTO, 100, 20)
    protected var mContext: Context? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mContainer = ObjectHolder.instance.getContainer(
                    arguments.getLong(ARG_CONTAINER_ID))
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater!!.inflate(R.layout.fragment_web_view, container, false)
        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initWebView()
        ObjectHolder.instance.putContext(mReadiumCtx.id, mReadiumCtx)
    }

    private fun initWebView() {
        if (App.isDebugging) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.settings.allowUniversalAccessFromFileURLs = true
        webView.setWebViewClient(ReadiumWebViewClient())
        webView.setWebChromeClient(WebChromeClient())
        webView.setOnTouchListener(
                WebViewMotionHandler(mContext!!, object : WebViewMotionHandler.OnMotionListener {
                    override fun onMoveNextPage() {
                        mReadiumCtx.api.openPageRight()
                    }

                    override fun onMovePreviousPage() {
                        mReadiumCtx.api.openPageLeft()
                    }

                    override fun onOpenMenu() {
                        mListener?.onOpenMenu()
                    }
                }))
    }

    private fun initReadium(): ReadiumContext {
        val readiumCtx = ReadiumContext(object : ReadiumContext.WebViewDelegate {
            override fun evaluateJavascript(script: String) {
                runOnUiThread(Runnable {
                    Log.d(TAG, "WebView evaluateJavascript: " + script + "")
                    webView.evaluateJavascript(script) {
                        str -> Log.d(TAG, "WebView evaluateJavascript RETURN: " + str)
                    }
                })
            }

            override fun openAsset(fileName: String): InputStream? {
                try {
                    return mContext!!.assets.open(fileName)
                } catch (e: IOException) {
                    Log.e(TAG, "Asset Open Fail! : " + fileName)
                    return null
                }
            }

            override fun addJavascriptInterface(jsInterface: ReadiumContext.JsInterface, name: String) {
                webView.addJavascriptInterface(jsInterface, name)
            }

            override fun loadUrl(url: String) {
                webView.loadUrl(url)
            }

        }, createPageEventListener(), mContainer!!)


        return readiumCtx
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onAttach(activity: Activity) {
        super.onAttach(activity)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mContext = activity.applicationContext
            if (activity is OnFragmentInteractionListener) {
                mListener = activity
            } else {
                throw RuntimeException(activity.toString() + " must implement OnFragmentInteractionListener")
            }

        }
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
        mContext = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (webView.parent as ViewGroup).removeView(webView)
        webView.removeAllViews()
        webView.clearCache(true)
        webView.clearHistory()
        webView.destroy()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (activity.isFinishing) {
            mReadiumCtx.dispose()
        }
        ObjectHolder.instance.removeContext(mReadiumCtx.id)
    }

    fun runOnUiThread(action: Runnable) {
        activity?.runOnUiThread(action)
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnFragmentInteractionListener {
        fun onPackageOpen(readiumContext: ReadiumContext)
        fun onPageChanged(pageIndex: Int, spineIndex: Int)
        fun onOpenMenu()
    }

    inner class ReadiumWebViewClient : WebViewClient() {

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            Laz.y { Log.d(TAG, "onPageStarted: " + url) }
        }

        override fun onPageFinished(view: WebView, url: String) {
            Laz.y { Log.d(TAG, "onPageFinished: " + url) }
        }

        override fun onLoadResource(view: WebView, url: String) {
            Laz.y { Log.d(TAG, "onLoadResource: " + url) }
        }

        override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean {
            Laz.y { Log.d(TAG, "shouldOverrideUrlLoading: " + req.url) }
            return false
        }

        override fun shouldInterceptRequest(view: WebView, req: WebResourceRequest): WebResourceResponse {
            Laz.y { Log.d(TAG, "-------- interceptRequest: " + req.url.toString()) }

            return mReadiumCtx.handleWebRequest(req.url.toString())
                    ?: return super.shouldInterceptRequest(view, req)
        }
    }

    // Create PageEventListener which delivers web browsing events.
    // This method is meant to be overrided in HiddenReadererFragment.
    open fun createPageEventListener(): ReadiumContext.PageEventListener {
        return object : ReadiumContext.PageEventListener {

            override fun onReaderInitialized() {
                runOnUiThread(Runnable {
                    val pckg = mReadiumCtx.pckg

                    mContext?.let { context ->
                        // Get last open page number.
                        mLastPageinfo = PaginationPrefs(context)
                        val spine = pckg.spineItems[mLastPageinfo?.spineIndex ?: 0]

                        mReadiumCtx.api.openBook(pckg, mViewerSettings,
                                OpenPageRequest.fromIdref(spine.idRef))

                        mListener?.onPackageOpen(mReadiumCtx)
                    }
                })
            }

            override fun onPaginationChanged(currentPagesInfo: PaginationInfo) {
                Laz.y { Log.d(TAG, "onPaginationChanged: " + currentPagesInfo) }

                val openPages = currentPagesInfo.openPages
                if (openPages.isEmpty())
                    return

                runOnUiThread(Runnable {
                    val pckg = mReadiumCtx.pckg
                    val page = openPages[0]

                    val spineItem = pckg.getSpineItem(page.idref)
                    val isFixedLayout = spineItem!!.isFixedLayout(pckg)
                    webView.settings.builtInZoomControls = isFixedLayout
                    webView.settings.displayZoomControls = false

                    if (mLastPageinfo != null) {
                        val newPage = mLastPageinfo!!.recalculateSpinePage(
                                page.spineItemPageCount)
                        Laz.y { Log.d("PaginationPref", "" + newPage) }

                        mReadiumCtx.api.openSpineItemPage(spineItem.idRef, newPage)

                        // Set null not to reuse.
                        mLastPageinfo = null
                    } else {
                        mContext?.let {
                            PaginationPrefs.save(it,
                                    page.spineItemIndex,
                                    page.spineItemPageIndex,
                                    page.spineItemPageCount)
                        }
                    }

                    mListener?.onPageChanged(page.spineItemPageIndex, page.spineItemIndex)
                })
            }
        }
    }

    companion object {
        private val ARG_CONTAINER_ID = "container"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.

         * @param containerId container id held by ObjectHolder.
         * *
         * @return A new instance of fragment WebViewFragment.
         */
        fun newInstance(containerId: Long): WebViewFragment {
            val fragment = WebViewFragment()

            val args = Bundle()
            args.putLong(ARG_CONTAINER_ID, containerId)

            fragment.arguments = args
            return fragment
        }
    }

}// Required empty public constructor