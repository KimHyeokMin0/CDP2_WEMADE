package com.home.cdp2app.view.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.home.cdp2app.health.bloodpressure.usecase.LoadBloodPressure
import com.home.cdp2app.health.heart.usecase.LoadHeartRate
import com.home.cdp2app.health.order.type.HealthCategory
import com.home.cdp2app.health.order.usecase.LoadChartOrder
import com.home.cdp2app.health.order.usecase.SaveChartOrder
import com.home.cdp2app.health.sleep.usecase.LoadSleepHour
import com.home.cdp2app.util.livedata.Event
import com.home.cdp2app.view.chart.Chart
import com.home.cdp2app.view.chart.parser.ChartParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * 대쉬보드 관리에 사용되는 VM
 * @property loadChartOrder RecyclerView에서 차트 순서를 불러오는데 사용됩니다.
 * @property loadHeartRate 심박수를 가져올때 사용됩니다.
 * @property loadBloodPressure 혈압(수축기, 이완기)를 가져올때 사용됩니다.
 * @property loadSleepHour 수면시간을 가져올때 사용됩니다.
 * @property chartParser 읽어온 엔티티를 차트로 파싱할때 사용됩니다. 사용하는 매퍼는 초기화 되어 있습니다.
 */
class DashboardViewModel(private val loadChartOrder: LoadChartOrder,
                         private val loadHeartRate: LoadHeartRate,
                         private val loadBloodPressure: LoadBloodPressure,
                         private val loadSleepHour: LoadSleepHour,
                         private val chartParser: ChartParser) {

    private val LOG_HEADER : String = "Fragment_Dashboard" //for log

    val toastLiveData : MutableLiveData<Event<HealthCategory>> = MutableLiveData() //특정 sync toast 알림 위한 이벤트 라이브데이터
    val chartList: MutableLiveData<MutableList<Chart>> = MutableLiveData<MutableList<Chart>>()
    /* 좋은 방법인가? 의문이 드는 방식. ChartOrder로드 - Order에 맞춰 loadAll 호출하는 방식임.
    * 원래는 vm에서 lazy로 호출 한다음 Order load - loadAllChartData 호출하는 방식으로 했었으나, 현재 init으로 변경하였음
    * 이렇게 되니 VM이 초기화 되는 순간 무조건 모든 유스케이스를 활용해서 데이터를 불러오는 부작용이 생김 - 테스트가 매우 어려워짐
    * 이를 해결하려 하면 vm이 아닌 view (fragment)에서 loadAllChart를 하는방식으로 변경해야함. 하지만 init에서 CoroutineScope로 ChartOrder를 로드하고 있음
    * 이는, 뷰 onCreate등에서 loadAll을 호출할경우 아직 ChartOrder가 로드되지 않았을 가능성이 있음. - 이로 인한 문제 발생
    *  따라서, Chart 순서가 로딩됐을경우 Event LiveData를 보내서 view에서 loadAll 호출하게는 했는데.. 음.. 더 좋은 방법이 있다면 수정하면 될것 같음.
    */
    val chartOrderLoadEvent : MutableLiveData<Event<Boolean>> = MutableLiveData()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            chartList.postValue(loadChartOrder().toEmptyChart())
            chartOrderLoadEvent.postValue(Event(true))
        }
    }

    //최초로 한번 모든 차트 데이터를 불러오는 기능
    fun loadAllChartData() {
        CoroutineScope(Dispatchers.IO).launch {
            val date = Instant.now()
            val heartRateJob = async { loadHeartRateChart(date) }
            val sleepHourJob = async { loadSleepHourChart(date) }
            val systolicJob = async { loadBloodPressureSystolicChart(date) }
            val diastolicJob = async { loadBloodPressureDiastolicChart(date) }
            // async - await를 이용해서 동시에 로드할 수 있도록 수행
            heartRateJob.await()
            sleepHourJob.await()
            systolicJob.await()
            diastolicJob.await()
        }

    }

    //동기화 버튼 클릭
    fun requestSync(category: HealthCategory) {
        val date : Instant = Instant.now()
        CoroutineScope(Dispatchers.IO).launch {
            when (category) {
                HealthCategory.HEART_RATE -> loadHeartRateChart(date)
                HealthCategory.BLOOD_PRESSURE_SYSTOLIC -> loadBloodPressureSystolicChart(date)
                HealthCategory.BLOOD_PRESSURE_DIASTOLIC -> loadBloodPressureDiastolicChart(date)
                HealthCategory.SLEEP_HOUR -> loadSleepHourChart(date)
            }
            toastLiveData.postValue(Event(category))
        }
    }

    suspend fun loadHeartRateChart(date : Instant) {
        val result = loadHeartRate(date)
        if (result.isEmpty())
            Log.w(LOG_HEADER, "심박수 데이터가 비어있습니다.")
        else
            updateChart(result, HealthCategory.HEART_RATE)
    }

    suspend fun loadSleepHourChart(date : Instant) {
        val result = loadSleepHour(date)
        if (result.isEmpty())
            Log.w(LOG_HEADER, "수면시간 데이터가 비어있습니다.")
        else
            updateChart(result, HealthCategory.SLEEP_HOUR)
    }

    suspend fun loadBloodPressureDiastolicChart(date : Instant) {
        val result = loadBloodPressure(date)
        if (result.isEmpty())
            Log.w(LOG_HEADER, "혈압 데이터가 비어있습니다.")
        else
            updateChart(result, HealthCategory.BLOOD_PRESSURE_DIASTOLIC)
    }

    suspend fun loadBloodPressureSystolicChart(date : Instant) {
        val result = loadBloodPressure(date)
        if (result.isEmpty())
            Log.w(LOG_HEADER, "혈압 데이터가 비어있습니다.")
        else
            updateChart(result, HealthCategory.BLOOD_PRESSURE_SYSTOLIC)
    }

    //읽어온 데이터를 이용해서 차트로 파싱하는 메소드
    //data는 비어있어선 안됨.
    private fun updateChart(data : List<*>, category : HealthCategory) {
        //차트가 초기화 되어 있지 않을경우 ignore
        if (chartList.value.isNullOrEmpty()) {
            Log.w(LOG_HEADER, "can't update recycler view. chart is empty.")
            return
        }

        val lastChart : MutableList<Chart> = chartList.value!! //마지막으로 업데이트된 차트
        val parsedChart = chartParser.parse(data, category) //파싱
        val categoryIndex = lastChart.indexOfFirst { it.type == category } //해당 enum을 가진 chart 탐색
        if (categoryIndex == -1) {
            //없는경우 - 순서 등록이 안되어있을때, 발생하지 않을것으로 사료됨
            Log.w(LOG_HEADER, "can't update recycler view. can't find chart index")
            return
        }
        lastChart[categoryIndex] = parsedChart //차트 업데이트
        chartList.postValue(lastChart) //notify

    }

}