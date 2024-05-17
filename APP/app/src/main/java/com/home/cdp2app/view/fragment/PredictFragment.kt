package com.home.cdp2app.view.fragment

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import com.home.cdp2app.R
import com.home.cdp2app.databinding.MainPredictBinding
import com.home.cdp2app.memory.SharedPreferencesStorage
import com.home.cdp2app.predict.entity.PredictResult
import com.home.cdp2app.predict.repository.PreferencePredictCacheRepository
import com.home.cdp2app.predict.repository.RemotePredictRepository
import com.home.cdp2app.predict.usecase.GetCachePredictResult
import com.home.cdp2app.predict.usecase.PredictUseCase
import com.home.cdp2app.predict.usecase.SaveCachePredictResult
import com.home.cdp2app.rest.type.NetworkStatus
import com.home.cdp2app.user.auth.repository.PreferenceAuthTokenRepository
import com.home.cdp2app.user.auth.usecase.DeleteAuthToken
import com.home.cdp2app.user.auth.usecase.GetAuthToken
import com.home.cdp2app.util.network.NetworkModule
import com.home.cdp2app.view.callback.MainPagerCallback
import com.home.cdp2app.view.mapper.PredictViewMapper
import com.home.cdp2app.view.viewmodel.predict.PredictViewModel
import com.ramijemli.percentagechartview.callback.AdaptiveColorProvider
import java.util.concurrent.ThreadLocalRandom

class PredictFragment : Fragment() {

    private var callback : MainPagerCallback? = null //메인 화면으로 이동하기 위한 콜백
    private var binding: MainPredictBinding? = null

    private var viewMapper: PredictViewMapper? = null //predict시 뷰 변경하는 클래스
    //todo hilt inject
    private val viewModel : PredictViewModel by lazy {
        val storage = SharedPreferencesStorage(requireContext())
        val authTokenRepository = PreferenceAuthTokenRepository(storage)
        val predictRepository = RemotePredictRepository(NetworkModule.predictAPI)
        val predictCacheRepository = PreferencePredictCacheRepository(storage)
        PredictViewModel(DeleteAuthToken(authTokenRepository), PredictUseCase(GetAuthToken(authTokenRepository), predictRepository), SaveCachePredictResult(predictCacheRepository), GetCachePredictResult(predictCacheRepository))
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as MainPagerCallback?
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val bind = MainPredictBinding.inflate(inflater, container, false).also {
            this.binding = it // global binding 초기화
            this.viewMapper = PredictViewMapper(requireContext(), it)
        }
        initView(bind)
        initListener(bind)
        initObserver()
        return bind.root

    }

    private fun initObserver() {
        viewModel.predictLiveData.observe(viewLifecycleOwner) {
            it.getContent()?.let { result ->
                handlePredictResult(result)
            }
        }

        viewModel.networkStatus.observe(viewLifecycleOwner) {
            hideLoadingView()
            it.getContent()?.let { status ->
                handleNetworkStatus(status)
            }
        }
    }

    //chart 업데이트 하면서 문자열 수정하기.
    private fun handlePredictResult(result : PredictResult) {
        viewMapper?.notify(result)
    }

    // 네트워크 결과값에 따른 핸들링
    private fun handleNetworkStatus(status : NetworkStatus) {
        when (status) {
            NetworkStatus.UNAUTHORIZED -> {
                Toast.makeText(requireContext(), R.string.login_required, Toast.LENGTH_LONG).show()
                callback?.navigateMain()
            }
            NetworkStatus.CONNECTION_ERROR -> Toast.makeText(requireContext(), R.string.connection_error, Toast.LENGTH_LONG).show() //통신 오류
            NetworkStatus.BAD_REQUEST -> Toast.makeText(requireContext(), R.string.other_error, Toast.LENGTH_LONG).show() //예측 api에서 bad request는 코딩 문제. 따라서 개발자에게 문의 메시지 노출하기
            NetworkStatus.INTERNAL_ERROR -> Toast.makeText(requireContext(), R.string.internal_error, Toast.LENGTH_LONG).show() //내부 문제
            NetworkStatus.OTHER -> Toast.makeText(requireContext(), R.string.other_error, Toast.LENGTH_LONG).show() //기타 개발자 문제
            NetworkStatus.OK -> {} //현재 뷰에선 OK 따로 처리하지 않음 (Chart listen)
        }
    }

    private fun initListener(bind: MainPredictBinding) {
        bind.predict.setOnClickListener {
            displayLoadingView()
            viewModel.requestPredict()
        }
    }

    private fun initView(bind : MainPredictBinding) {
        //뷰 색상 지정등등
        bind.percentageChartView.let {
            it.textSize = 70f
            it.setAdaptiveColorProvider(ChartColorProvider(requireContext()))
            it.apply()
        }
    }

    override fun onPause() {
        super.onPause()
        // 현재 실행중인 작업 취소 및 뷰 초기화 (Disable 다시 Enable)
        hideLoadingView()
        viewModel.cancel() //요청 중단
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null //바인딩 제거
        viewMapper = null //mapper null
    }

    //로딩 뷰 보여주는 메소드 -
    private fun displayLoadingView() {
        binding?.apply {
            indicator.visibility = View.VISIBLE
            percentageChartView.visibility = View.INVISIBLE
            predict.isEnabled = false
            subText.text = getString(R.string.in_predicting)
        }
    }

    //로딩 뷰 제거하는 메소드
    private fun hideLoadingView() {
        binding?.apply {
            indicator.visibility = View.INVISIBLE
            percentageChartView.visibility = View.VISIBLE
            predict.isEnabled = true
            subText.text = getString(R.string.predict_request_now) //최초 메시지로 변경. hide를 요청한 뒤 text 변경하기!
        }
    }


    class ChartColorProvider(private val context : Context) : AdaptiveColorProvider {
        override fun provideTextColor(progress: Float): Int {
            return ContextCompat.getColor(context, R.color.black)
        }

        override fun provideProgressColor(progress: Float): Int {
            return if (progress <= 25) Color.GREEN //0~25%
            else if (progress <= 50) Color.YELLOW //25~50%
            else if (progress <= 75) ContextCompat.getColor(context, R.color.vermilion)
            else ContextCompat.getColor(context, R.color.scarlet)
        }

        override fun provideBackgroundColor(progress: Float): Int {
            return ColorUtils.blendARGB(provideProgressColor(progress), Color.BLACK, .8f)
        }

        override fun provideBackgroundBarColor(progress: Float): Int {
            return ContextCompat.getColor(context, R.color.light_grey)
        }
    }
}