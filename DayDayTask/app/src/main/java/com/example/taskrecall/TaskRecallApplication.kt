package com.example.taskrecall

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 应用入口。
 *
 * 规格：单 Activity + Compose、MVVM、Hilt 依赖注入。
 * 这里使用 @HiltAndroidApp 生成整个应用的依赖容器，后续的
 * AppDatabase、Repository、DataStore 都从这里获得生命周期。
 */
@HiltAndroidApp
class TaskRecallApplication : Application()
