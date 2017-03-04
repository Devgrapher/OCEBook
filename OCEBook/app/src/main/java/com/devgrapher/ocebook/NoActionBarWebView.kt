package com.devgrapher.ocebook

import android.content.Context
import android.util.AttributeSet
import android.view.ActionMode
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.webkit.WebView

/*
    Hook startActionMode to disable Context Action Bar.

    Appearing Action Bar occurs resizing the view and recalculation of page count, which is the
    main reason.
    This class only hook the old method, api under 23, since from 23 it doesn't create action bar.
 */
class NoActionBarWebView : WebView {
    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    override fun startActionMode(callback: ActionMode.Callback): ActionMode {
        return this.dummyActionMode()
    }

    fun dummyActionMode(): ActionMode {
        return object : ActionMode() {
            override fun setTitle(title: CharSequence) {}
            override fun setTitle(resId: Int) {}
            override fun setSubtitle(subtitle: CharSequence) {}
            override fun setSubtitle(resId: Int) {}
            override fun setCustomView(view: View) {}
            override fun invalidate() {}
            override fun finish() {}
            override fun getMenu(): Menu? {
                return null
            }

            override fun getTitle(): CharSequence? {
                return null
            }

            override fun getSubtitle(): CharSequence? {
                return null
            }

            override fun getCustomView(): View? {
                return null
            }

            override fun getMenuInflater(): MenuInflater? {
                return null
            }
        }
    }
}
