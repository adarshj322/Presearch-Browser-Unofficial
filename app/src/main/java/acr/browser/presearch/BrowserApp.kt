package acr.browser.presearch

import acr.browser.presearch.database.bookmark.BookmarkExporter
import acr.browser.presearch.database.bookmark.BookmarkRepository
import acr.browser.presearch.device.BuildInfo
import acr.browser.presearch.device.BuildType
import acr.browser.presearch.di.AppComponent
import acr.browser.presearch.di.DaggerAppComponent
import acr.browser.presearch.di.DatabaseScheduler
import acr.browser.presearch.di.injector
import acr.browser.presearch.log.Logger
import acr.browser.presearch.preference.DeveloperPreferences
import acr.browser.presearch.utils.FileUtils
import acr.browser.presearch.utils.MemoryLeakUtils
import acr.browser.presearch.utils.installMultiDex
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.StrictMode
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import com.squareup.leakcanary.LeakCanary
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.plugins.RxJavaPlugins
import javax.inject.Inject
import kotlin.system.exitProcess

class BrowserApp : Application() {

    @Inject internal lateinit var developerPreferences: DeveloperPreferences
    @Inject internal lateinit var bookmarkModel: BookmarkRepository
    @Inject @field:DatabaseScheduler internal lateinit var databaseScheduler: Scheduler
    @Inject internal lateinit var logger: Logger
    @Inject internal lateinit var buildInfo: BuildInfo

    lateinit var applicationComponent: AppComponent

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT < 21) {
            installMultiDex(context = base)
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build())
        }

        if (Build.VERSION.SDK_INT >= 28) {
            if (getProcessName() == "$packageName:incognito") {
                WebView.setDataDirectorySuffix("incognito")
            }
        }

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
            if (BuildConfig.DEBUG) {
                FileUtils.writeCrashToStorage(ex)
            }

            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, ex)
            } else {
                exitProcess(2)
            }
        }

        RxJavaPlugins.setErrorHandler { throwable: Throwable? ->
            if (BuildConfig.DEBUG && throwable != null) {
                FileUtils.writeCrashToStorage(throwable)
                throw throwable
            }
        }

        applicationComponent = DaggerAppComponent.builder()
            .application(this)
            .buildInfo(createBuildInfo())
            .build()
        injector.inject(this)

        Single.fromCallable(bookmarkModel::count)
            .filter { it == 0L }
            .flatMapCompletable {
                val assetsBookmarks = BookmarkExporter.importBookmarksFromAssets(this@BrowserApp)
                bookmarkModel.addBookmarkList(assetsBookmarks)
            }
            .subscribeOn(databaseScheduler)
            .subscribe()

        if (developerPreferences.useLeakCanary && buildInfo.buildType == BuildType.DEBUG) {
            LeakCanary.install(this)
        }
        if (buildInfo.buildType == BuildType.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        registerActivityLifecycleCallbacks(object : MemoryLeakUtils.LifecycleAdapter() {
            override fun onActivityDestroyed(activity: Activity) {
                logger.log(TAG, "Cleaning up after the Android framework")
                MemoryLeakUtils.clearNextServedView(activity, this@BrowserApp)
            }
        })
    }

    /**
     * Create the [BuildType] from the [BuildConfig].
     */
    private fun createBuildInfo() = BuildInfo(when {
        BuildConfig.DEBUG -> BuildType.DEBUG
        else -> BuildType.RELEASE
    })

    companion object {
        private const val TAG = "BrowserApp"

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT)
        }
    }

}
