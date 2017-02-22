package com.devgrapher.ocebook;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.devgrapher.ocebook.model.OpenPageRequest;
import com.devgrapher.ocebook.model.Page;
import com.devgrapher.ocebook.model.PaginationInfo;
import com.devgrapher.ocebook.readium.ReadiumContext;
import com.devgrapher.ocebook.util.Laz;

import org.readium.sdk.android.Package;
import org.readium.sdk.android.SpineItem;

import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HiddenRendererFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HiddenRendererFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HiddenRendererFragment extends WebViewFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String TAG = WebViewFragment.class.toString();
    private static final String ARG_CONTAINER_ID = "container";

    private OnFragmentInteractionListener mListener;

    public HiddenRendererFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param containerId container id held by ObjectHolder.
     * @return A new instance of fragment HiddenRendererFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HiddenRendererFragment newInstance(long containerId) {
        HiddenRendererFragment fragment = new HiddenRendererFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_CONTAINER_ID, containerId);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        WebView webView = (WebView) view.findViewById(R.id.webView);
        webView.setVisibility(View.INVISIBLE);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (activity instanceof OnFragmentInteractionListener) {
                mListener = (OnFragmentInteractionListener) activity;
            } else {
                throw new RuntimeException(activity.toString()
                        + " must implement OnFragmentInteractionListener");
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        void onBrowsePageInProgress(int currentSpine, int totalSpine, int pageCount);
    }

    @Override
    public ReadiumContext.PageEventListener createPageEventListener() {
        return new ReadiumContext.PageEventListener() {

            @Override
            public void onReaderInitialized() {
                getActivity().runOnUiThread(() -> {
                    final Package pckg = mReadiumCtx.getPackage();
                    if (pckg.getSpineItems().size() > 0) {
                        SpineItem item = pckg.getSpineItems().get(0);
                        mReadiumCtx.getApi().openBook(pckg, mViewerSettings,
                                OpenPageRequest.fromIdref(item.getIdRef()));
                    }
                });
            }

            @Override public void onContentLoaded() {}
            @Override public void onPageLoaded() {}
            @Override public void onIsMediaOverlayAvailable(String available) {}
            @Override public void onMediaOverlayStatusChanged(String status) {}
            @Override public void onMediaOverlayTTSSpeak() {}
            @Override public void onMediaOverlayTTSStop() {}
            @Override public void getBookmarkData(String bookmarkData) {}

            @Override
            public void onPaginationChanged(PaginationInfo currentPagesInfo) {
                Laz.y(()->Log.d(TAG, "onPaginationChanged: " + currentPagesInfo));
                List<Page> openPages = currentPagesInfo.getOpenPages();
                if (openPages.isEmpty())
                    return;

                getActivity().runOnUiThread(() -> {
                    final Page page = openPages.get(0);

                    int totalSpine = currentPagesInfo.getSpineItemCount();
                    int spineIdx = page.getSpineItemIndex();
                    int nextSpine = spineIdx + 1;

                    // notify the page count in current spine.
                    mListener.onBrowsePageInProgress(
                            spineIdx, totalSpine, page.getSpineItemPageCount());

                    if (nextSpine < totalSpine) {
                        // open next spine
                        Log.i(TAG, "Browse spines" + nextSpine + "/" + totalSpine);

                        SpineItem spine = mReadiumCtx.getPackage().getSpineItems().get(nextSpine);
                        mReadiumCtx.getApi().openBook(mReadiumCtx.getPackage(), mViewerSettings,
                                OpenPageRequest.fromIdref(spine.getIdRef()));
                    }
                });
            }

            @Override public void onSettingsApplied() {}
        };
    }
}
