package com.home.cdp2app.main.setting

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.home.cdp2app.databinding.MainSettingBinding
import com.home.cdp2app.main.dashboard.view.callback.ChartDetailCallback
import com.home.cdp2app.main.setting.basicinfo.view.BasicInfoActivity
import com.home.cdp2app.main.setting.order.view.DashboardOrderActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingFragment : Fragment() {

    private var callback : ChartDetailCallback? = null

    private val viewModel : SettingViewModel by viewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as ChartDetailCallback?
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bind = MainSettingBinding.inflate(inflater, container, false)
        initListener(bind)
        return bind.root
    }

    // setup click listener
    private fun initListener(view : MainSettingBinding) {
        view.userInfo.setOnClickListener {
            startActivity(Intent(requireActivity(), BasicInfoActivity::class.java))
        }
        view.changeOrder.setOnClickListener {
            startActivity(Intent(requireActivity(), DashboardOrderActivity::class.java))
        }
        //로그아웃
        view.signOut.setOnClickListener {
            signOut(it) //버튼 상태 변경위한 메소드
        }
    }

    private fun signOut(view : View) {
        view.isClickable = false //두번이상 클릭 못하게 막기
        viewModel.signOut().observe(this) { event ->
            //이벤트 핸들 안될경우 리턴
            val isSuccess = event.getContent() ?: return@observe
            if (isSuccess) {
                callback?.navigateMain()
                view.isClickable = true //혹시나 navigate 미작동 대비해서 click 허용
            }
        }
    }
}