package com.checkmoney.Main

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.checkmoney.R
import com.checkmoney.TransactionModel
import com.checkmoney.category
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter

class TabFragmentYear : Fragment() {
    private lateinit var allTransaction: ArrayList<TransactionModel>
    private lateinit var pieChart: PieChart
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let{
            allTransaction = it.getParcelableArrayList("transaction")!!
        }
    }

    override fun onCreateView (
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        pieChart = inflater.inflate(R.layout.fragment_month, container).findViewById(R.id.pieChart)
        createPieChart2()
        return inflater.inflate(R.layout.fragment_year, container)
    }

    override fun onStart() {
        super.onStart()
        createPieChart2()
    }

    private fun createPieChart2() {
        // 퍼센트 사용
        pieChart.setUsePercentValues(true)
        // 설명
        pieChart.description.text = ""
        // 터치가능
        pieChart.setTouchEnabled(true)
        // 회전가능
        pieChart.isRotationEnabled = true
        // 차트안에 항목이름
        pieChart.setDrawEntryLabels(true)
        // 하단항목이름
        pieChart.legend.isEnabled = false
        //pieChart.legend.orientation = Legend.LegendOrientation.HORIZONTAL
        //pieChart.legend.isWordWrapEnabled = true
        // 차트에 데이터 추가
        val dataEntries = ArrayList<PieEntry>()
        dataEntries.add(PieEntry(56f, "식비"))
        dataEntries.add(PieEntry(26f, "교통비"))
        dataEntries.add(PieEntry(10f, "생활용품비"))
        dataEntries.add(PieEntry(6f, "주거비"))
        dataEntries.add(PieEntry(2f, "쇼핑"))
        // 차트 색깔
        val colors: ArrayList<Int> = ArrayList()
        colors.add(Color.parseColor("#0096FF"))
        colors.add(Color.parseColor("#0064FF"))
        colors.add(Color.parseColor("#4C39E1"))
        colors.add(Color.parseColor("#FFF176"))
        colors.add(Color.parseColor("#FF8A65"))

        // dataset
        val dataSet = PieDataSet(dataEntries, "")
        // 개별 data
        val data = PieData(dataSet)

        // In Percentage
        data.setValueFormatter(PercentFormatter())
        // 항목간 여백 길이
        dataSet.sliceSpace = 1f
        // 차트 색깔 적용
        dataSet.colors = colors
        // 차트에 data 적용
        pieChart.data = data
        // data 글자 크기
        data.setValueTextSize(15f)
        pieChart.setExtraOffsets(5f, 10f, 5f, 5f)
        // 차트 생성시 애니메이션
        pieChart.animateY(1400, Easing.EaseInOutQuad)

        // 구멍의 지름
        pieChart.holeRadius = 40f
        // 흰색 원 지름
        pieChart.transparentCircleRadius = 45f
        // 차트안 구멍 생성
        pieChart.isDrawHoleEnabled = true
        // 구멍 색
        pieChart.setHoleColor(Color.WHITE)

        pieChart.invalidate()
    }

    // 차트 생성
    private fun createPieChart() {
        // 퍼센트 사용
        pieChart.setUsePercentValues(true)
        // 설명
        pieChart.description.text = ""
        // 터치가능
        pieChart.setTouchEnabled(true)
        // 회전가능
        pieChart.isRotationEnabled = true
        // 차트안에 항목이름
        pieChart.setDrawEntryLabels(true)
        // 하단항목이름
        pieChart.legend.isEnabled = false
        //pieChart.legend.orientation = Legend.LegendOrientation.HORIZONTAL
        //pieChart.legend.isWordWrapEnabled = true
        // 차트에 데이터 추가
        val priceCategory = Array(8) { 0.0F }
        val colors: ArrayList<Int> = ArrayList()
        allTransaction.forEach {
            if(it.is_consumption == 1 && it.category == 0){
                priceCategory[0] = priceCategory[0] + it.price
            }
            else if(it.is_consumption == 1 && it.category == 1){
                priceCategory[1] = priceCategory[1] + it.price
            }
            else if(it.is_consumption == 1 && it.category == 2){
                priceCategory[2] = priceCategory[2] + it.price
            }
            else if(it.is_consumption == 1 && it.category == 3){
                priceCategory[3] = priceCategory[3] + it.price
            }
            else if(it.is_consumption == 1 && it.category == 4){
                priceCategory[4] = priceCategory[4] + it.price
            }
            else if(it.is_consumption == 1 && it.category == 5){
                priceCategory[5] = priceCategory[5] + it.price
            }
            else if(it.is_consumption == 1 && it.category == 6){
                priceCategory[6] = priceCategory[6] + it.price
            }
            else if(it.is_consumption == 1 && it.category == 7){
                priceCategory[7] = priceCategory[7] + it.price
            }
        }
        val totalPrice = priceCategory.sum()
        val dataEntries = ArrayList<PieEntry>()
        if(priceCategory[0] != 0.0F){
            val category0 = priceCategory[0] / totalPrice * 100
            dataEntries.add(PieEntry(category0, category.category[0]))
            colors.add(Color.parseColor("#0096FF"))
        }
        if(priceCategory[1] != 0.0F){
            val category0 = priceCategory[1] / totalPrice * 100
            dataEntries.add(PieEntry(category0, category.category[1]))
            colors.add(Color.parseColor("#0064FF"))
        }
        if(priceCategory[2] != 0.0F){
            val category0 = priceCategory[2] / totalPrice * 100
            dataEntries.add(PieEntry(category0, category.category[2]))
            colors.add(Color.parseColor("#4C39E1"))
        }
        if(priceCategory[3] != 0.0F){
            val category0 = priceCategory[3] / totalPrice * 100
            dataEntries.add(PieEntry(category0, category.category[3]))
            colors.add(Color.parseColor("#FFF176"))
        }
        if(priceCategory[4] != 0.0F){
            val category0 = priceCategory[4] / totalPrice * 100
            dataEntries.add(PieEntry(category0, category.category[4]))
            colors.add(Color.parseColor("#FF8A65"))
        }
        if(priceCategory[5] != 0.0F){
            val category0 = priceCategory[5] / totalPrice * 100
            dataEntries.add(PieEntry(category0, category.category[5]))
            colors.add(Color.parseColor("#CAF0F8"))
        }
        if(priceCategory[6] != 0.0F){
            val category0 = priceCategory[6] / totalPrice * 100
            dataEntries.add(PieEntry(category0, category.category[6]))
            colors.add(Color.parseColor("#90E0EF"))
        }
        if(priceCategory[7] != 0.0F){
            val category0 = priceCategory[7] / totalPrice * 100
            dataEntries.add(PieEntry(category0, category.category[7]))
            colors.add(Color.parseColor("#90C8EF"))
        }

        // dataset
        val dataSet = PieDataSet(dataEntries, "")
        // 개별 data
        val data = PieData(dataSet)

        // In Percentage
        data.setValueFormatter(PercentFormatter())
        // 항목간 여백 길이
        dataSet.sliceSpace = 1f
        // 차트 색깔 적용
        dataSet.colors = colors
        // 차트에 data 적용
        pieChart.data = data
        // data 글자 크기
        data.setValueTextSize(15f)
        pieChart.setExtraOffsets(5f, 10f, 5f, 5f)
        // 차트 생성시 애니메이션
        pieChart.animateY(1400, Easing.EaseInOutQuad)

        // 구멍의 지름
        pieChart.holeRadius = 40f
        // 흰색 원 지름
        pieChart.transparentCircleRadius = 45f
        // 차트안 구멍 생성
        pieChart.isDrawHoleEnabled = true
        // 구멍 색
        pieChart.setHoleColor(Color.WHITE)

        pieChart.invalidate()
    }

    companion object {
        fun create(): TabFragmentYear {
            return TabFragmentYear()
        }
    }
}