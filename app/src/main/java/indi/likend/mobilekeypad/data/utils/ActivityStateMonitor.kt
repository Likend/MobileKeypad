package indi.likend.mobilekeypad.data.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class ActivityStateMonitor @Inject constructor(application: Application) : Application.ActivityLifecycleCallbacks {
    private val _isAppForeground = MutableStateFlow(false)
    val isAppForeground = _isAppForeground.asStateFlow()

    private var startedActivityCount = 0 // 活跃 Activity 计数器

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivityCount++
        // 只要有一个 Activity 启动了，App 就在前台
        // 只要 count > 0 就设置为 true。
        // 如果之前已经是 true，StateFlow 内部会发现值没变，从而忽略这次赋值。
        _isAppForeground.value = startedActivityCount > 0
        Log.d(
            TAG,
            "onActivityStarted, startedActivityCount=$startedActivityCount isAppForeground=${isAppForeground.value}"
        )
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivityCount--

        // 只有当不是因为旋转导致的停止时，才可能触发 false
        if (!activity.isChangingConfigurations) {
            // 如果 count 归零，这里会设为 false
            // 同样，只有从 true 变成 false 的那一刻，下游才会收到通知
            _isAppForeground.value = startedActivityCount > 0
        }
        Log.d(
            TAG,
            "onActivityStopped, startedActivityCount=$startedActivityCount, isChangingConfigurations=${activity.isChangingConfigurations} isAppForeground=${isAppForeground.value}"
        )
    }

    // --- 其他回调留空 ---
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    companion object {
        const val TAG = "ActivityStateMonitor"
    }
}
