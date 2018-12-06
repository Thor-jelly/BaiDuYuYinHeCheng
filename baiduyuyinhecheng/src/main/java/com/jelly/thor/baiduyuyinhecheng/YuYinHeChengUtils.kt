package com.jelly.thor.baiduyuyinhecheng

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Message
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.baidu.tts.chainofresponsibility.logger.LoggerProxy
import com.baidu.tts.client.SpeechSynthesizer
import com.baidu.tts.client.TtsMode
import com.jelly.thor.baiduyuyinhecheng.control.InitConfig
import com.jelly.thor.baiduyuyinhecheng.control.MySyntherizer
import com.jelly.thor.baiduyuyinhecheng.control.NonBlockSyntherizer
import com.jelly.thor.baiduyuyinhecheng.listener.MessageListener
import com.jelly.thor.baiduyuyinhecheng.util.AutoCheck
import com.jelly.thor.baiduyuyinhecheng.util.OfflineResource
import java.io.IOException
import java.util.*


/**
 * 类描述：百度语音合成工具类<br/>
 * 创建人：吴冬冬<br/>
 * 创建时间：2018/12/6 11:16 <br/>
 */
object YuYinHeChengUtils {
    private const val TAG = "YuYinHeChengUtils"

    //1.首先需要设置一些参数
    private var appId: String? = null
    private var appKey: String? = null
    private var secretKey: String? = null
    private var ttsModel: TtsMode? = null
    private var offlineVoice: String? = null
    private var activity: AppCompatActivity? = null
    private var requestPermissionsCode = 10010

    // ===============初始化参数设置完毕，更多合成参数请至getParams()方法中设置 =================

    // 主控制类，所有合成控制方法从这个类开始
    @SuppressLint("StaticFieldLeak")
    private var synthesizer: NonBlockSyntherizer? = null


    private var DESC = ("请先看完说明。之后点击“合成并播放”按钮即可正常测试。\n"
            + "测试离线合成功能需要首次联网。\n"
            + "纯在线请修改代码里ttsMode为TtsMode.ONLINE， 没有纯离线。\n"
            + "本Demo的默认参数设置为wifi情况下在线合成, 其它网络（包括4G）使用离线合成。 在线普通女声发音，离线男声发音.\n"
            + "合成可以多次调用，SDK内部有缓存队列，会依次完成。\n\n")

    /**
     * @param activity 用来申请权限
     * @param requestPermissionsCode 用来申请权限
     * @param ttsModel TtsMode.MIX; 离在线融合，在线优先； TtsMode.ONLINE 纯在线； 没有纯离线
     * @param offlineVoice 离线发音，OfflineResource.VOICE_MALE即为离线女声发音。OfflineResource.VOICE_MALE男声
     */
    @JvmStatic
    fun init(
        @NonNull activity: AppCompatActivity,
        requestPermissionsCode: Int,
        @NonNull appId: String,
        @NonNull appKey: String,
        @NonNull secretKey: String,
        @NonNull ttsModel: TtsMode = TtsMode.MIX,
        offlineVoice: String = OfflineResource.VOICE_FEMALE
    ) {
        this.appId = appId
        this.appKey = appKey
        this.secretKey = secretKey
        this.ttsModel = ttsModel
        this.offlineVoice = offlineVoice
        this.activity = activity
        this.requestPermissionsCode = requestPermissionsCode

        initPermission() // android 6.0以上动态权限申请
    }

    /**
     * 写在activity中的onRequestPermissionsResult
     */
    @JvmStatic
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            this.requestPermissionsCode -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //用户同意授权
                initialTts()
            } else {
                //用户拒绝授权
                //获取用户 不在提醒的权限
                val showDoNotAskAgainPermissions = showDoNotAskAgainPermissions(permissions)

                showDoNotAskAgainPermissions?.let {
                    Toast.makeText(this.activity, "你拒绝了权限申请，请同意！", Toast.LENGTH_SHORT).show()
                    initPermission()
                } ?: Toast.makeText(this.activity, "您有不再提示的权限，请到设置界面允许该权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 得到语音合成是否初始化成功
     */
    @JvmStatic
    fun getInitSuccess(): Boolean {
        return synthesizer?.isInitSuccess ?: throw UnsupportedOperationException("请先调用初始化方法")
    }

    /**
     * 合成并播放
     * @param text 小于1024 GBK字节，即512个汉字或者字母数字
     */
    @JvmStatic
    fun speak(text: String) {
        val speak = synthesizer?.speak(text) ?: throw UnsupportedOperationException("请先调用初始化方法")
        if (BuildConfig.DEBUG && speak != 0) {
            Log.e(TAG, "合成并播放错误码=$speak 错误码文档:http://yuyin.baidu.com/docs/tts/122")
        }
    }

    /**
     * 结束时候调用
     */
    @JvmStatic
    fun onDestroy() {
        synthesizer?.release() ?: throw UnsupportedOperationException("请先调用初始化方法")
        synthesizer = null
    }

    /**
     * android 6.0 以上需要动态申请权限
     */
    private fun initPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.activity == null) {
                return
            }

            //检查权限，并返回未同意的权限
            val toApplyList = checkPermission()

            //有不同意的权限，请求权限
            if (!toApplyList.isEmpty()) {
                ActivityCompat.requestPermissions(
                    this.activity!!,
                    toApplyList.toTypedArray(),
                    this.requestPermissionsCode
                )
                return
            }
        }
        initialTts()
    }

    /**
     * 检查权限，并返回未同意的权限
     */
    private fun checkPermission(): ArrayList<String> {
        val permissions = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
//            Manifest.permission.WRITE_SETTINGS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE
        )

        val toApplyList = ArrayList<String>()

        //检测权限是否同意
        for (perm in permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(
                    this.activity as Context,
                    perm
                )
            ) {
                // 进入到这里代表没有权限.
                toApplyList.add(perm)
            }
        }
        return toApplyList
    }

    /**
     * 判断是否有不在提示的权限
     */
    private fun showDoNotAskAgainPermissions(deniedPermissions: Array<String>): List<String>? {
        var notAskAgainList: MutableList<String>? = null
        for (deniedPermission in deniedPermissions) {
            /*
                如果应用之前请求过此权限但用户拒绝了请求，此方法将返回 true。
                注：
                如果用户在过去拒绝了权限请求，并在权限请求系统对话框中选择了 Don’t ask again 选项，此方法将返回 false。
                如果设备规范禁止应用具有该权限，此方法也会返回 false。
             */
//            Log.d(
//                "123===",
//                "shouldShowRequestPermissionRationale: " + deniedPermission + "  " + ActivityCompat.shouldShowRequestPermissionRationale(
//                    this.activity,
//                    deniedPermission
//                )
//            )
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this.activity!!, deniedPermission)) {
                if (notAskAgainList == null) {
                    notAskAgainList = ArrayList()
                }
                notAskAgainList.add(deniedPermission)
            }
        }
        return notAskAgainList
    }

    /**
     * 初始化引擎，需要的参数均在InitConfig类里
     *
     *
     * DEMO中提供了3个SpeechSynthesizerListener的实现
     * MessageListener 仅仅用log.i记录日志，在logcat中可以看见
     * UiMessageListener 在MessageListener的基础上，对handler发送消息，实现UI的文字更新
     * FileSaveListener 在UiMessageListener的基础上，使用 onSynthesizeDataArrived回调，获取音频流
     */
    private fun initialTts() {
        //日志打印在logcat中
        LoggerProxy.printable(BuildConfig.DEBUG)
        // 设置初始化参数
        // 此处可以改为 含有您业务逻辑的SpeechSynthesizerListener的实现类
        //需要监听合成状态的时候用到 val listener = MessageListener(mainHandler)
        val listener = null

        val params = getParams()

        // appId appKey secretKey 网站上您申请的应用获取。注意使用离线合成功能的话，需要应用中填写您app的包名。包名在build.gradle中获取。
        val initConfig = InitConfig(this.appId, this.appKey, this.secretKey, this.ttsModel, params, listener)

        // 如果您集成中出错，请将下面一段代码放在和demo中相同的位置，并复制InitConfig 和 AutoCheck到您的项目中
        // 上线时请删除AutoCheck的调用
        if (BuildConfig.DEBUG) {
            AutoCheck.getInstance(this.activity!!.applicationContext).check(initConfig, @SuppressLint("HandlerLeak")
            object : Handler() {
                override fun handleMessage(msg: Message) {
                    if (msg.what == 100) {
                        val autoCheck = msg.obj as AutoCheck
                        synchronized(autoCheck) {
                            val message = autoCheck.obtainDebugMessage()
                            // 可以用下面一行替代，在logcat中查看代码
                            Log.w("AutoCheckMessage", message);
                        }
                    }
                }
            })
        }
        synthesizer =
                NonBlockSyntherizer(this.activity as Context, initConfig) // 此处可以改为MySyntherizer 了解调用过程
    }


    /**
     * 合成的参数，可以初始化时填写，也可以在合成前设置。
     *
     * @return
     */
    private fun getParams(): Map<String, String> {
        val params = HashMap<String, String>()
        // 仅在线生效，在线的发音 以下参数均为选填
        // 设置在线发声音人： 0 普通女声（默认） 1 普通男声 2 特别男声 3 情感男声<度逍遥> 4 情感儿童声<度丫丫>
        params[SpeechSynthesizer.PARAM_SPEAKER] = "0"
        // 设置合成的音量，0-9 ，默认 5
        params[SpeechSynthesizer.PARAM_VOLUME] = "9"
        // 设置合成的语速，0-9 ，默认 5
        params[SpeechSynthesizer.PARAM_SPEED] = "5"
        // 设置合成的语调，0-9 ，默认 5
        params[SpeechSynthesizer.PARAM_PITCH] = "5"

        params[SpeechSynthesizer.PARAM_MIX_MODE] = SpeechSynthesizer.MIX_MODE_HIGH_SPEED_NETWORK
        // 该参数设置为TtsMode.MIX生效。即纯在线模式不生效。
        // MIX_MODE_DEFAULT 默认 ，wifi状态下使用在线，非wifi离线。在线状态下，请求超时6s自动转离线
        // MIX_MODE_HIGH_SPEED_SYNTHESIZE_WIFI wifi状态下使用在线，非wifi离线。在线状态下， 请求超时1.2s自动转离线
        // MIX_MODE_HIGH_SPEED_NETWORK ， 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线
        // MIX_MODE_HIGH_SPEED_SYNTHESIZE, 2G 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线

        // 离线资源文件， 从assets目录中复制到临时目录，需要在initTTs方法前完成
        val offlineResource = createOfflineResource(this.offlineVoice!!)
        // 声学模型文件路径 (离线引擎使用), 请确认下面两个文件存在
        params[SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE] = offlineResource.textFilename
        params[SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE] = offlineResource.modelFilename
        return params
    }

    private fun createOfflineResource(voiceType: String): OfflineResource {
        var offlineResource: OfflineResource? = null
        try {
            offlineResource = OfflineResource(this.activity!!, voiceType)
        } catch (e: IOException) {
            // IO 错误自行处理
            e.printStackTrace()
            //Log.e(TAG, "【error】:copy files from assets failed." + e.message)
        }
        return offlineResource!!
    }
}