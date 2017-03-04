package com.devgrapher.ocebook

import android.annotation.TargetApi
import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView

import com.devgrapher.ocebook.model.OpenPageRequest
import com.devgrapher.ocebook.model.Page
import com.devgrapher.ocebook.model.PaginationInfo
import com.devgrapher.ocebook.readium.ReadiumContext
import com.devgrapher.ocebook.util.Laz

import org.readium.sdk.android.Package
import org.readium.sdk.android.SpineItem


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [HiddenRendererFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [HiddenRendererFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HiddenRendererFragment : WebViewFragment() {
    private val TAG = WebViewFragment::class.java.toString()
    private var mListener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        val webView = view?.findViewById(R.id.webView) as WebView
        webView.visibility = View.INVISIBLE

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
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
            if (activity is OnFragmentInteractionListener) {
                mListener = activity
            } else {
                throw RuntimeException(activity.toString() + " must implement OnFragmentInteractionListener")
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    interface OnFragmentInteractionListener {
        fun onBrowsePageInProgress(currentSpine: Int, totalSpine: Int, pageCount: Int)
    }

    override fun createPageEventListener(): ReadiumContext.PageEventListener {
        return object : ReadiumContext.PageEventListener {

            override fun onReaderInitialized() {
                runOnUiThread(Runnable {
                    mReadiumCtx.pckg.spineItems[0]
                            .let { item ->
                                mReadiumCtx.api.openBook(mReadiumCtx.pckg,
                                        mViewerSettings,
                                        OpenPageRequest.fromIdref(item.href))
                    }
                })
            }

            override fun onPaginationChanged(currentPagesInfo: PaginationInfo) {
                Laz.y { Log.d(TAG, "onPaginationChanged: " + currentPagesInfo) }
                val openPages = currentPagesInfo.openPages
                if (openPages.isEmpty())
                    return

                runOnUiThread(Runnable {
                    val page = openPages[0]

                    val totalSpine = currentPagesInfo.spineItemCount
                    val spineIdx = page.spineItemIndex
                    val nextSpine = spineIdx + 1

                    // notify the page count in current spine.
                    mListener?.onBrowsePageInProgress(
                            spineIdx, totalSpine, page.spineItemPageCount)

                    if (nextSpine < totalSpine) {
                        // open next spine
                        Log.i(TAG, "Browse spines$nextSpine/$totalSpine")

                        mReadiumCtx.pckg.spineItems[nextSpine]
                                .let { item ->
                                    mReadiumCtx.api.openBook(mReadiumCtx.pckg, mViewerSettings,
                                            OpenPageRequest.fromIdref(item.idRef))
                                }
                    }
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
         * @return A new instance of fragment HiddenRendererFragment.
         */
        fun newInstance(containerId: Long): HiddenRendererFragment {
            val fragment = HiddenRendererFragment()

            val args = Bundle()
            args.putLong(ARG_CONTAINER_ID, containerId)

            fragment.arguments = args
            return fragment
        }
    }
}// Required empty public constructor
