/*
 * Copyright (C) 2019-2023 James Clef <qwq233@qwq2333.top>
 * https://github.com/KitsunePie/FuckWristPlayer
 *
 * This software is open source software BUT IT IS NOT FREE SOFTWARE: you can redistribute it
 * and/or modify it under our terms.
 */

package top.qwq2333.appfucker

import android.util.Log
import com.github.kyuubiran.ezxhelper.utils.hookAllConstructorBefore
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.isStatic
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import org.luckypray.dexkit.DexKitBridge


class MainHook : IXposedHookLoadPackage {
    val doExportDex = false // Debug only
    lateinit var dexKit: DexKitBridge

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        runCatching {
            if (lpparam.packageName != "com.appsisle.developerassistant") return@runCatching
            System.loadLibrary("dexkit")
            val classloader = lpparam.classLoader
            dexKit = DexKitBridge.create(classloader, true)

            loge("find licenseStatus")
            val licenseStatus = dexKit.findClass {
                matcher {
                    addMethod {
                        name = "toString"
                        usingStrings = listOf("LicenseStatus(license=", ", expiryDate=")
                    }
                }
            }.single().getInstance(classloader).run {
                getPermanentLicense(dexKit, classloader, this).also {
                    hookAllConstructorBefore {
                        it.result = this
                    }
                }
            }

            loge("hook licenseManager")
            dexKit.findClass {
                matcher {
                    usingStrings = listOf("refreshLicenseIfNotRefreshedRecently", "refreshLicense", "onLicenseRefreshFailed")
                }
            }.single().getInstance(classloader).declaredMethods.single {
                it.parameterTypes.size == 1 && it.returnType.name == it.parameterTypes[0].name
            }.hookBefore {
                it.result = licenseStatus
            }
        }.onFailure {
            loge(it.message!!)
            loge(it.stackTraceToString())
            dexKit.close()
        }.onSuccess {
            dexKit.close()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getPermanentLicense(dexkit: DexKitBridge, classLoader: ClassLoader, clazz: Class<*>): Any = dexkit.findClass {
        matcher {
            usingStrings = listOf("Permanent", "Subscription", "Evaluation", "Expired", "NotKnownYet", "Invalid")
        }
    }.single().getInstance(classLoader).run result@{
        (declaredFields.single {
            it.type.isArray && it.isSynthetic && it.isStatic
        }.apply {
            isAccessible = true
        }.get(null) as Array<Enum<*>>).forEach {
            if (it.name == "Permanent") {
                return@result clazz.constructors.single { it.parameterTypes.size == 1 }.apply {
                    isAccessible = true
                }.newInstance(it)
            }
        }
    }

    fun loge(msg: String) {
        Log.e("FuckDeveloperAssistant", msg)
        try {
            XposedBridge.log("FuckDeveloperAssistant: $msg")
        } catch (e: NoClassDefFoundError) {
            Log.e("Xposed", msg)
            Log.e("EdXposed-Bridge", msg)
        }
    }
}
