package com.github.jing332.tts_server_android.ui.systts.edit.microsoft

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
import com.github.jing332.tts_server_android.App
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.tts_server_android.constant.MsTtsApiType
import com.github.jing332.tts_server_android.databinding.SysttsMsEditActivityBinding
import com.github.jing332.tts_server_android.model.speech.tts.BgmTTS
import com.github.jing332.tts_server_android.model.speech.tts.MsTTS
import com.github.jing332.tts_server_android.ui.systts.edit.BaseTtsEditActivity
import com.github.jing332.tts_server_android.ui.view.widget.WaitDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*

class MsTtsEditActivity : BaseTtsEditActivity<MsTTS>({
    MsTTS(locale = AppConst.locale.run { "$language-$country" })
}) {
    companion object {
        const val TAG = "MsTtsEditActivity"
    }

    private val tts by lazy { getTts<MsTTS>() }
    private val binding: SysttsMsEditActivityBinding by lazy {
        SysttsMsEditActivityBinding.inflate(layoutInflater).apply { m = vm }
    }
    private val vm: MsTtsEditViewModel by viewModels()

    private val waitDialog by lazy { WaitDialog(this) }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setEditContentView(binding.root, binding.testLayout.tilTest)

        // 帮助 二级语言
        binding.tilSecondaryLocale.setStartIconOnClickListener {
            MaterialAlertDialogBuilder(this).setTitle(R.string.systts_secondaryLocale)
                .setMessage(R.string.systts_help_secondary_locale).show()
        }

        // 接口加载回调
        vm.setCallback(object : MsTtsEditViewModel.CallBack {
            override fun onStart(@MsTtsApiType api: Int) {
                waitDialog.show()
                binding.editView.setFormatByApi(api)
            }

            override fun onDone(ret: Result<Unit>) {
                waitDialog.dismiss()
                ret.onFailure { e ->
                    MaterialAlertDialogBuilder(this@MsTtsEditActivity)
                        .setTitle(R.string.systts_voice_data_load_failed)
                        .setMessage(e.toString())
                        .setPositiveButton(R.string.retry) { _, _ -> vm.reloadApiData() }
                        .setNegativeButton(R.string.exit) { _, _ -> finish() }
                        .show()
                }
            }
        })

        vm.styleDegreeVisibleLiveData.observe(this) {
            binding.editView.isStyleDegreeVisible = it
        }

        // 初始化 注册监听
        vm.init(
            listOf(
                Pair(getString(R.string.systts_api_edge) + " (Java-OkHTTP 支持音频流)", 0),
                Pair(getString(R.string.systts_api_edge) + " (Go-Native)", 0),
            )
        )

        binding.editView.setData(tts)
        vm.initUserData(systemTts)
    }

    override fun onTest(text: String) {
        waitDialog.show()
        vm.doTest(text, { audio, sampleRate, mime ->
            waitDialog.dismiss()
            val s =
                if (tts.format == "webm-24khz-16bit-24kbps-mono-opus") getString(R.string.systts_ms_webm_warn_msg) + "\n" else ""

            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.systts_test_success)
                .setMessage(
                    s + getString(
                        R.string.systts_test_success_info,
                        audio.size / 1024, sampleRate, mime
                    )
                )
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener { stopPlay() }
                .show()
            playAudio(audio)
        }, { err ->
            waitDialog.dismiss()
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.test_failed)
                .setMessage(err.message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        })
    }

    override fun onSave() {
        vm.onSave()
        super.onSave()
    }
}