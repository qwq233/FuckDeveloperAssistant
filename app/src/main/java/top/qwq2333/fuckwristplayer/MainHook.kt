/*
 * Copyright (C) 2019-2023 James Clef <qwq233@qwq2333.top>
 * https://github.com/KitsunePie/FuckWristPlayer
 *
 * This software is open source software BUT IT IS NOT FREE SOFTWARE: you can redistribute it
 * and/or modify it under our terms.
 */

package top.qwq2333.fuckwristplayer

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.luckypray.dexkit.DexKitBridge
import io.luckypray.dexkit.enums.MatchType
import java.io.File
import java.lang.reflect.Method


class MainHook : IXposedHookLoadPackage {
    lateinit var classloader: ClassLoader
    lateinit var context: Context
    lateinit var dexkit: DexKitBridge
    val doExportDex = false // Debug only

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "cn.luern0313.wristvideoplayer") return
        loge("hooked")
        XposedHelpers.findAndHookMethod("com.stub.StubApp", lpparam.classLoader, "attachBaseContext", Context::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                super.afterHookedMethod(param)
                context = param.args[0] as Context
                classloader = context.classLoader
                loge("getClassLoader")

                loadNative()
                dexkit = DexKitBridge.create(classloader, true)!!

                if (doExportDex) {
                    // export dex files
                    val file = File("${context.filesDir.absolutePath}/dex/")
                    loge(file.absolutePath)
                    file.let { if (!it.exists()) it.mkdirs() }
                    dexkit.exportDexFile(file.absolutePath)
                }

                // hook PurchaseApi
                // http://154.8.225.108:8080/v/isCodeExist?code={deviceCode}   // http? why not https?
                dexkit.let { bridge ->
                    bridge.batchFindMethodsUsingStrings {
                        addQuery("PurchaseApi", listOf("isCodeExist"))
                        matchType = MatchType.CONTAINS
                    }["PurchaseApi"]!!.firstOrNull()?.let {
                        val classDescriptor = it
                        val method: Method = classDescriptor.getMethodInstance(classloader)
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
                    } ?: loge("Unable to find PurchaseApi.isCodeExist. Maybe the api is changed?")
                }
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

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    @SuppressWarnings("deprecation")
    fun loadNative() = runCatching {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            val targetFolder = File(context.filesDir, "lib")
            targetFolder.let { if (!it.exists()) it.mkdirs() }
            val targetFile = File(targetFolder, "libdexkit.so")
            this@MainHook.javaClass.classLoader!!.getResourceAsStream("lib/${Build.CPU_ABI}/libdexkit.so")!!.let {
                targetFile.writeBytes(it.readBytes())
            }
            System.load(targetFile.absolutePath)
        } else {
            System.loadLibrary("dexkit")
        }
    }.getOrElse { loge(it.stackTraceToString()) }
}