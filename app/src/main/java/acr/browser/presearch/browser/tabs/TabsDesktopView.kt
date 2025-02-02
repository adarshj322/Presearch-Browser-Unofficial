package acr.browser.presearch.browser.tabs

import acr.browser.presearch.R
import acr.browser.presearch.browser.TabsView
import acr.browser.presearch.list.HorizontalItemAnimator
import acr.browser.presearch.controller.UIController
import acr.browser.presearch.extensions.color
import acr.browser.presearch.extensions.inflater
import acr.browser.presearch.view.LightningView
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * A view which displays browser tabs in a horizontal [RecyclerView].
 */
class TabsDesktopView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), TabsView {

    private val uiController = context as UIController
    private val tabsAdapter: TabsDesktopAdapter
    private val tabList: RecyclerView

    init {
        setBackgroundColor(context.color(R.color.black))
        context.inflater.inflate(R.layout.tab_strip, this, true)
        findViewById<ImageView>(R.id.new_tab_button).apply {
            setColorFilter(context.color(R.color.icon_dark_theme))
            setOnClickListener {
                uiController.newTabButtonClicked()
            }
            setOnLongClickListener {
                uiController.newTabButtonLongClicked()
                true
            }
        }

        val layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)

        val animator = HorizontalItemAnimator().apply {
            supportsChangeAnimations = false
            addDuration = 200
            changeDuration = 0
            removeDuration = 200
            moveDuration = 200
        }

        tabsAdapter = TabsDesktopAdapter(context, context.resources, uiController = uiController)

        tabList = findViewById<RecyclerView>(R.id.tabs_list).apply {
            setLayerType(View.LAYER_TYPE_NONE, null)
            itemAnimator = animator
            this.layoutManager = layoutManager
            adapter = tabsAdapter
            setHasFixedSize(true)
        }
    }

    override fun tabAdded() {
        displayTabs()
        tabList.postDelayed({ tabList.smoothScrollToPosition(tabsAdapter.itemCount - 1) }, 500)
    }

    override fun tabRemoved(position: Int) {
        displayTabs()
    }

    override fun tabChanged(position: Int) {
        displayTabs()
    }

    private fun displayTabs() {
        tabsAdapter.showTabs(uiController.getTabModel().allTabs.map(LightningView::asTabViewState))
    }

    override fun tabsInitialized() {
        tabsAdapter.notifyDataSetChanged()
    }

    override fun setGoBackEnabled(isEnabled: Boolean) = Unit

    override fun setGoForwardEnabled(isEnabled: Boolean) = Unit

}
