/*
 * Copyright (C) 2019-2023 James Clef <qwq233@qwq2333.top>
 * https://github.com/KitsunePie/FuckWristPlayer
 *
 * This software is open source software BUT IT IS NOT FREE SOFTWARE: you can redistribute it
 * and/or modify it under our terms.
 */

package top.qwq2333.fuckwristplayer

import android.content.Context
import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage


class MainHook : IXposedHookLoadPackage {
    lateinit var classloader: ClassLoader

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "cn.luern0313.wristvideoplayer") return
        loge("hooked")
        XposedHelpers.findAndHookMethod("com.stub.StubApp", lpparam.classLoader, "attachBaseContext", Context::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                super.afterHookedMethod(param)
                val context = param.args[0] as Context
                classloader = context.classLoader
                loge("getClassLoader")
                XposedHelpers.findAndHookMethod("pc", classloader, "d", String::class.java, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        super.afterHookedMethod(param)
                        param.result = true
                    }
                })
            }
        })

    }

    fun loge(msg: String) {
        Log.e("FuckWristPlayer", msg)
        try {
            XposedBridge.log("FuckWristPlayer: $msg")
        } catch (e: NoClassDefFoundError) {
            Log.e("Xposed", msg)
            Log.e("EdXposed-Bridge", msg)
        }
    }
}