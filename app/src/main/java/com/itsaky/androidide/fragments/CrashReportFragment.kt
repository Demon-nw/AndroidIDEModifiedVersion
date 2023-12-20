/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.itsaky.androidide.fragments

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.blankj.utilcode.util.AppUtils.getAppVersionCode
import com.blankj.utilcode.util.ClipboardUtils
import com.blankj.utilcode.util.DeviceUtils.getManufacturer
import com.blankj.utilcode.util.DeviceUtils.getModel
import com.itsaky.androidide.BuildConfig
import com.itsaky.androidide.buildinfo.BuildInfo
import com.itsaky.androidide.databinding.LayoutCrashReportBinding
import com.itsaky.androidide.resources.R

class CrashReportFragment : Fragment() {

  private var binding: LayoutCrashReportBinding? = null
  private var closeAppOnClick = true

  companion object {

    const val KEY_TITLE = "crash_title"
    const val KEY_MESSAGE = "crash_message"
    const val KEY_TRACE = "crash_trace"
    const val KEY_CLOSE_APP_ON_CLICK = "close_on_app_click"

    private val CRASH_REPORT_HEADER by lazy {
      val map = mapOf(
        "Version" to "v${BuildInfo.VERSION_NAME_SIMPLE} (${getAppVersionCode()})",
        "CI Build" to BuildInfo.CI_BUILD,
        "Branch" to BuildInfo.CI_GIT_BRANCH,
        "Commit" to BuildInfo.CI_GIT_COMMIT_HASH,
        "Variant" to "${BuildConfig.FLAVOR} (${BuildConfig.BUILD_TYPE})",
        "SDK Version" to Build.VERSION.SDK_INT,
        "Supported ABIs" to "[${Build.SUPPORTED_ABIS.joinToString(separator = ", ")}]",
        "Manufacturer" to getManufacturer(),
        "Device" to getModel()
      )
      """
AndroidIDE Crash Report
${map.entries.joinToString(separator = System.lineSeparator()) { "${it.key} : ${it.value}" }}

Stacktrace:
""".trim()
    }

    @JvmStatic
    fun newInstance(trace: String): CrashReportFragment {
      return newInstance(null, null, trace, true)
    }

    @JvmStatic
    fun newInstance(
      title: String?,
      message: String?,
      trace: String,
      closeAppOnClick: Boolean
    ): CrashReportFragment {
      val frag = CrashReportFragment()
      val args = Bundle().apply {
        putString(KEY_TRACE, trace)
        putBoolean(KEY_CLOSE_APP_ON_CLICK, closeAppOnClick)
        title?.let { putString(KEY_TITLE, it) }
        message?.let { putString(KEY_MESSAGE, it) }
      }
      frag.arguments = args
      return frag
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return LayoutCrashReportBinding.inflate(inflater, container, false).also { binding = it }.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val args = requireArguments()
    closeAppOnClick = args.getBoolean(KEY_CLOSE_APP_ON_CLICK)
    var title: String? = getString(R.string.msg_ide_crashed)
    var message: String? = getString(R.string.msg_report_crash)
    if (args.containsKey(KEY_TITLE)) {
      title = args.getString(KEY_TITLE)
    }

    if (args.containsKey(KEY_MESSAGE)) {
      message = args.getString(KEY_MESSAGE)
    }

    val trace: String = if (args.containsKey(KEY_TRACE)) {
      buildReportText(args.getString(KEY_TRACE))
    } else {
      "No stack strace was provided for the report"
    }

    binding!!.apply {
      crashTitle.text = title
      crashSubtitle.text = message
      logText.text = trace

      val report: String = trace
      closeButton.setOnClickListener {
        if (closeAppOnClick) {
          requireActivity().finishAffinity()
        } else {
          requireActivity().finish()
        }
      }

      reportButton.setOnClickListener { reportTrace(report) }
    }
  }

  private fun reportTrace(report: String) {
    ClipboardUtils.copyText("AndroidIDE CrashLog", report)
    val url = BuildInfo.REPO_URL + "/issues"
    val intent = Intent()
    intent.action = Intent.ACTION_VIEW
    intent.data = Uri.parse(url)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
  }

  private fun buildReportText(trace: String?): String {
    return """
$CRASH_REPORT_HEADER
$trace
    """
  }

  override fun onDestroyView() {
    super.onDestroyView()
    binding = null
  }
}