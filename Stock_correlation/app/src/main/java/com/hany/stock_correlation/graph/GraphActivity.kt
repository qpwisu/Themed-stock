package com.hany.stock_correlation.graph
import kotlinx.coroutines.*

import android.graphics.Color
import android.os.Bundle
import android.widget.SeekBar
import com.hany.stock_correlation.databinding.ActivityGraphBinding
import com.hany.stock_correlation.notimportant.DemoBase
import com.github.mikephil.charting.data.CandleData

import android.graphics.Paint
import android.os.Build

import com.github.mikephil.charting.components.YAxis.AxisDependency

import com.github.mikephil.charting.data.CandleDataSet

import android.util.Log

import com.github.mikephil.charting.data.CandleEntry
import java.lang.String

import java.util.ArrayList
import android.widget.TextView
import androidx.annotation.RequiresApi

import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.components.XAxis.XAxisPosition
import com.hany.stock_correlation.R
import com.github.mikephil.charting.components.AxisBase

import com.github.mikephil.charting.formatter.ValueFormatter





//:DemoBase(), SeekBar.OnSeekBarChangeListener
class GraphActivity :DemoBase(), SeekBar.OnSeekBarChangeListener  {
    private var chart: CandleStickChart? = null
    private var seekBarX: SeekBar? = null
    private var seekBarY: SeekBar? = null
    private var tvX: TextView? = null
    private var tvY: TextView? = null
    var entiresPrice = ArrayList<CandleEntry>()
    val binding by lazy { ActivityGraphBinding.inflate(layoutInflater) }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(binding.root);
        var code = intent.getStringExtra("code")
        var candle_list=
            intent.getParcelableArrayListExtra<Candle>("candle")



        Log.d("canddd", candle_list.toString())
        var entries = ArrayList<CandleEntry>()
        if (candle_list != null) {
            for (csStock in candle_list) {
                entries.add(
                    CandleEntry(
                        csStock.createdAt.toFloat(),
                        csStock.shadowHigh.toFloat(),
                        csStock.shadowLow.toFloat(),
                        csStock.open.toFloat(),
                        csStock.close.toFloat()
                    )
                )
            }
        }

        entiresPrice= entries
        tvX = binding.tvXMax
        tvY= binding.tvYMax
        seekBarX= binding.seekBar1
        seekBarX!!.max = candle_list!!.size
        seekBarX!!.setOnSeekBarChangeListener(this)
        //seekbar 이벤트 리스너
        seekBarY= binding.seekBar2
        seekBarY!!.max = candle_list!!.size -1
        seekBarY!!.setOnSeekBarChangeListener(this);
        chart = binding.chart1
        chart!!.setBackgroundColor(Color.WHITE)
        chart!!.getDescription().setEnabled(false);
        chart!!.setMaxVisibleValueCount(60);
        //최대 60개 출력
        chart!!.setPinchZoom(false);
        val xAxis = chart!!.xAxis
        var days = intent.getStringArrayListExtra("days")
        Log.d("days",days.toString())
        xAxis.position = XAxisPosition.BOTTOM

        //x축 라벨 숫자에서 날짜로 변경
        xAxis.valueFormatter = days?.let { MyXAxisFormatter(it) }
        xAxis.setDrawGridLines(false)
        val leftAxis = chart!!.axisLeft
//        leftAxis.setEnabled(false);
        //        leftAxis.setEnabled(false);
        leftAxis.setLabelCount(7, false)
        leftAxis.setDrawGridLines(false)
        leftAxis.setDrawAxisLine(false)
        val rightAxis = chart!!.axisRight
        rightAxis.isEnabled = false
//        rightAxis.setStartAtZero(false);

        // setting data
        //        rightAxis.setStartAtZero(false);

        // setting data
        seekBarX!!.setProgress(candle_list!!.size );
        seekBarY!!.setProgress(candle_list!!.size -60);

        chart!!.legend.isEnabled = false
    }

    override fun saveToGallery() {
        TODO("Not yet implemented")
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        var progress =1
        if (seekBarX!!.getProgress()==0){
            progress = 1
        }else{
            progress=seekBarX!!.getProgress()
        }

        tvX!!.setText(String.valueOf(progress))
        tvY!!.setText(String.valueOf(seekBarY!!.getProgress()))
        chart!!.resetTracking()

        var values:ArrayList<CandleEntry> = entiresPrice
        Log.d("vvalues",values.toString())
        Log.d("vvalues",progress.toString())
        var seek = seekBarY!!.getProgress()
        var values2 = ArrayList<CandleEntry>()
        try {
            for (i in 0 until progress) {
                val multi = ( seek+ 1)
                val `val` = (values[i + multi].x).toFloat()
                val high = (values[i + multi].high).toFloat()
                val low = (values[i + multi].low).toFloat()
                val open = (values[i + multi].open).toFloat()
                val close = (values[i + multi].close).toFloat()
                val even = i % 2 == 0
                values2?.add(
                    CandleEntry(
                        (i + multi).toFloat(), high,
                        low,
                        open,
                        close,
                        resources.getDrawable(R.drawable.star)
                    )
                );
                val set1 = CandleDataSet(values2, "Data Set")
                Log.d("vvalues2",values2.toString())

                set1.setDrawIcons(false)
                set1.axisDependency = AxisDependency.LEFT
//        set1.setColor(Color.rgb(80, 80, 80));
                //        set1.setColor(Color.rgb(80, 80, 80));
                set1.shadowColor = Color.DKGRAY
                set1.shadowWidth = 0.7f
                set1.decreasingColor = Color.RED
                set1.decreasingPaintStyle = Paint.Style.FILL
                set1.increasingColor = Color.rgb(0, 0, 254)
                set1.increasingPaintStyle = Paint.Style.FILL
                set1.neutralColor = Color.BLUE
                //set1.setHighlightLineWidth(1f);

                //set1.setHighlightLineWidth(1f);
                val data = CandleData(set1)

                chart!!.data = data
                chart!!.invalidate()
            }
        }catch (e : Exception){
            println(e)
        }



    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
    }
    inner class MyXAxisFormatter(days:ArrayList<kotlin.String>) : ValueFormatter() {
        private val days = days
        override fun getAxisLabel(value: Float, axis: AxisBase?): kotlin.String? {
            return days.getOrNull(value.toInt()-1) ?: value.toString()
        }
    }
}


//
//
//
//            Log.d("aaaa1", "$candle_list")
//
//
//
//        }
//        deferred.join()
//
//    }
//
//    override fun saveToGallery() {
//        TODO("Not yet implemented")
//    }
//
//    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//        var progress = (seekBarX!!.getProgress())
//        tvX!!.setText(String.valueOf(progress))
//        tvY!!.setText(String.valueOf(seekBarY!!.getProgress()))
//        chart!!.resetTracking()
//
//        var values:ArrayList<CandleEntry> = entiresPrice
//        Log.d("vvalues",values.toString())
//
//        for (i in 0 until progress) {
//            val multi = (seekBarY!!.getProgress() + 1).toFloat()
//            val `val` = (values[i].x).toFloat() + multi
//            val high = (values[i].high).toFloat() + 8f
//            val low = (values[i].low).toFloat() + 8f
//            val open = (values[i].open).toFloat() + 1f
//            val close = (values[i].close).toFloat() + 1f
//            val even = i % 2 == 0
//            values.add(
//                CandleEntry(
//                    i.toFloat(), `val` + high,
//                    `val` - low,
//                    if (even) `val` + open else `val` - open,
//                    if (even) `val` - close else `val` + close,
////                    resources.getDrawable(R.drawable.star)
//                )
//            )
//        }
//        val set1 = CandleDataSet(values, "Data Set")
//
//        set1.setDrawIcons(false)
//        set1.axisDependency = AxisDependency.LEFT
////        set1.setColor(Color.rgb(80, 80, 80));
//        //        set1.setColor(Color.rgb(80, 80, 80));
//        set1.shadowColor = Color.DKGRAY
//        set1.shadowWidth = 0.7f
//        set1.decreasingColor = Color.RED
//        set1.decreasingPaintStyle = Paint.Style.FILL
//        set1.increasingColor = Color.rgb(122, 242, 84)
//        set1.increasingPaintStyle = Paint.Style.STROKE
//        set1.neutralColor = Color.BLUE
//        //set1.setHighlightLineWidth(1f);
//
//        //set1.setHighlightLineWidth(1f);
//        val data = CandleData(set1)
//
//        chart!!.data = data
//        chart!!.invalidate()
//    }
//
//    override fun onStartTrackingTouch(seekBar: SeekBar?) {
//    }
//
//    override fun onStopTrackingTouch(seekBar: SeekBar?) {
//        TODO("Not yet implemented")
//    }
