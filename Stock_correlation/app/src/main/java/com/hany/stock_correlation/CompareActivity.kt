package com.hany.stock_correlation

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.hany.stock_correlation.databinding.ActivityCompareBinding
import com.hany.stock_correlation.recycle.ContentModel
import com.hany.stock_correlation.recycle.ContentRVAdapter
import com.lakue.pagingbutton.LakuePagingButton
import com.lakue.pagingbutton.OnPageSelectListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.ceil
import kotlin.math.round


class CompareActivity : AppCompatActivity() {
    var ddday = ArrayList<String>()
    var toast: Toast ?=null
    var choice = 0
    var tmp = ArrayList<ILineDataSet>()
    val db = Firebase.firestore
    val rf = db.collection("corrAll")
    var stockName= java.util.ArrayList<String>()
    lateinit var lpb_buttonlist: LakuePagingButton
    var spinnerPosition= 0
    var items = ArrayList<ContentModel>()
    private var itemQuantity: MutableLiveData<ArrayList<ILineDataSet>> = MutableLiveData<ArrayList<ILineDataSet>>().apply {  // 초기값
        value = tmp
    }
    var scode =""
    inner class MyXAxisFormatter(days: java.util.ArrayList<String>) : ValueFormatter() {
        private val days = days
        override fun getAxisLabel(value: Float, axis: AxisBase?): kotlin.String? {
            return days.getOrNull(value.toInt()-1) ?: value.toString()
        }
    }

    suspend fun get_normalize_stock(sday:String,code:ArrayList<String>):ArrayList<ILineDataSet>{
        return try{
            code.add(scode)
            val dataSets = ArrayList<ILineDataSet>()
            val rgb_list = arrayOf(arrayOf(255,53,184),arrayOf(0,255,0),arrayOf(0,0,255),arrayOf(255,255,0),arrayOf(127,0,255),arrayOf(255,0,0))
            var t =0
            for (cod in code){
                val docRef2 = db.collection("stockPrice").document(cod)
                var c = ArrayList<NomalizationModel>()
                var nomalization = ArrayList<NomalizationModel>()
                docRef2.get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            var mm: MutableMap<kotlin.String, Any>? = document.data
                            Log.d("mm5",mm.toString())

                            if (mm != null) {
                                for ((day, price) in mm) {
                                    var spl = price.toString().split(",")
                                    c.add(
                                        NomalizationModel(
                                            day.toInt(),spl[0].toFloat()
                                        ))
                                }
                                c.sortBy(NomalizationModel::day)
                                Log.d("kkk3",c.toString())
                                c=ArrayList(c.slice((c.size- sday.toInt())..c.size-1))
                                //정규화
                                var dday = ArrayList<String>()
                                var pr = ArrayList<Float>()
                                for (i in c){
                                    dday.add(i.day.toString())
                                    pr.add(i.nom)
                                }
                                ddday= dday
                                Log.d("db2", dday.toString())
                                Log.d("db2", Collections.max(pr).toString())
                                var max = Collections.max(pr)
                                var min = Collections.min(pr)
                                for (i in 0 until pr.size){
                                    pr[i] = round((pr[i]-min)/(max-min)*100000) /100000
                                    nomalization.add(NomalizationModel(dday[i].toInt(),pr[i]))

                                }
                                var values1=ArrayList<Entry>()
                                for (i in 0 until nomalization.size){
                                    values1.add(Entry(i.toFloat(), nomalization[i].nom))
                                }
                                var set1 = LineDataSet(values1,cod)
                                var rgb =rgb_list[t]

                                set1.color =Color.rgb(rgb[0],rgb[1],rgb[2])
                                set1.setCircleColor(Color.rgb(rgb[0],rgb[1],rgb[2]))
                                t+=1
                                dataSets.add(set1)
                            }

                        }
                    }.addOnFailureListener{exception->
                        Log.d("tttest", "get failed with ", exception)
                    }.await()
            }
            dataSets
        }catch (e: FirebaseFirestoreException){
            val dataSets = ArrayList<ILineDataSet>()
            dataSets
        }
    }


    suspend fun get_search_best_corr(days:Int, page:Int,code:String):ArrayList<String>{
        return try{
            var code_item  = ArrayList<String>()

            items.clear()
            var rfd= rf.document(code)
            Log.d("kk",rf.toString())
            rfd.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        var mm: MutableMap<kotlin.String, Any>? = document.data
                        if (mm != null) {
                            for (i in 5*page-4..page*5) {
                                var spl = mm[i.toString()].toString().split(";")
                                items.add(
                                    ContentModel(
                                        i!!.toInt(),
                                        code,
                                        spl[days+choice],
                                        spl[days+1+choice].toString()
                                    )
                                )
                            }
                            var days_list = java.util.ArrayList<String>()
                            items.sortBy(ContentModel::rank)
                            for (item in items){
                                code_item.add(item.code2)
                            }
                        }

                    }
                }.addOnFailureListener{exception->
                    Log.d("tttest", "get failed with ", exception)
                }.await()

            code_item
        }catch (e:FirebaseException){
            var tt = ArrayList<String>()
            tt.add(e.toString())
            tt
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityCompareBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val ccode =intent.getStringExtra("code2")!!
        binding.cd.text = ccode.toString()
        scode = ccode

        var max_page = PreferenceManager.getInt(baseContext,"count")


        //page 버튼 한 번에 표시되는 버튼 수 (기본값 : 5)
        lpb_buttonlist = binding.lpbButtonlist
        lpb_buttonlist.setPageItemCount(5)
        //총 페이지 버튼 수와 현재 페이지 설정

        lpb_buttonlist.addBottomPageButton(max_page,1)
        var rvAdapter = ContentRVAdapter(items)

        binding.rv2.adapter =rvAdapter
        binding.rv2.layoutManager = LinearLayoutManager(this)
        val spinner_list=listOf( "30일", "90일","180일","365일","540일")
        val spin_adapter = ArrayAdapter(this, R.layout.spinner_layout, spinner_list)



        binding.edittxt3.setOnEditorActionListener{ textView, action, event ->
            var handled = false

            if (action == EditorInfo.IME_ACTION_DONE) {
                // 키보드 내리기
                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(binding.edittxt3.windowToken, 0)
                handled = true
            }

            handled
        }

        val chart = binding.chart12
        val xAxis = chart!!.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM


        chart.setOnChartValueSelectedListener(object:
            OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry, h: Highlight){
                val xAxisLabel = e.x.let{
                    xAxis.valueFormatter.getAxisLabel(it, xAxis)
                }

                if (toast ==null){
                    toast=Toast.makeText(this@CompareActivity, "" + xAxisLabel, Toast.LENGTH_SHORT)
                    toast!!.show()
                }
                else{
                    toast!!.cancel()
                    toast=Toast.makeText(this@CompareActivity, "" + xAxisLabel, Toast.LENGTH_SHORT)
                    toast!!.show()
                }
            }
            override fun onNothingSelected() {
            }
        })


        binding.spinner2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (position) {
                    0 ->{
                        lpb_buttonlist.addBottomPageButton(max_page,1)
                        //코루틴과 suspend함수를 이용해 동기적으로 처리한다
                        CoroutineScope(Dispatchers.Main).launch {

                            var result = withContext(Dispatchers.IO) {
                                get_search_best_corr(0,1, ccode)
                            }
                            Log.d("stt",items.toString())
                            rvAdapter.notifyDataSetChanged() //읽어오는게 비동기처리되어 리사이클러뷰는 빈칸으로 나오기때문에 다시 리사이클러뷰를 불러와서 바꿔줘여한다
                            var result2 = withContext(Dispatchers.IO) {
                                var dset = get_normalize_stock("30", result)
                                val data = LineData(dset)
                                spinnerPosition=0
                                data
                            }

                            chart.xAxis.valueFormatter = ddday.let { MyXAxisFormatter(it) }
                            chart.setData(result2)
                            chart.invalidate()
                        }

                    }
                    1 -> {
                        CoroutineScope(Dispatchers.Main).launch {
                            lpb_buttonlist.addBottomPageButton(max_page,1)

                            var result = withContext(Dispatchers.IO) {
                                get_search_best_corr(position*2,1, ccode)
                            }
                            Log.d("stt",items.toString())
                            rvAdapter.notifyDataSetChanged() //읽어오는게 비동기처리되어 리사이클러뷰는 빈칸으로 나오기때문에 다시 리사이클러뷰를 불러와서 바꿔줘여한다
                            var result2 = withContext(Dispatchers.IO) {
                                var dset = get_normalize_stock("90", result)
                                val data = LineData(dset)
                                spinnerPosition=1
                                data
                            }

                            // set listeners


                            chart.xAxis.valueFormatter = ddday.let { MyXAxisFormatter(it) }
                            chart.setData(result2)
                            chart.invalidate()
                        }
                    }
                    2 -> {
                        CoroutineScope(Dispatchers.Main).launch {
                            lpb_buttonlist.addBottomPageButton(max_page,1)

                            var result = withContext(Dispatchers.IO) {
                                get_search_best_corr(position*2,1, ccode)
                            }
                            Log.d("stt",items.toString())
                            rvAdapter.notifyDataSetChanged() //읽어오는게 비동기처리되어 리사이클러뷰는 빈칸으로 나오기때문에 다시 리사이클러뷰를 불러와서 바꿔줘여한다
                            var result2 = withContext(Dispatchers.IO) {
                                var dset = get_normalize_stock("180", result)
                                val data = LineData(dset)
                                spinnerPosition=2
                                data
                            }

                            chart.xAxis.valueFormatter = ddday.let { MyXAxisFormatter(it) }
                            chart.setData(result2)
                            chart.invalidate()
                        }
                    }
                    3 -> {
                        CoroutineScope(Dispatchers.Main).launch {
                            lpb_buttonlist.addBottomPageButton(max_page,1)

                            var result = withContext(Dispatchers.IO) {
                                get_search_best_corr(position*2,1, ccode)
                            }
                            Log.d("stt",items.toString())
                            rvAdapter.notifyDataSetChanged() //읽어오는게 비동기처리되어 리사이클러뷰는 빈칸으로 나오기때문에 다시 리사이클러뷰를 불러와서 바꿔줘여한다
                            var result2 = withContext(Dispatchers.IO) {
                                var dset = get_normalize_stock("365", result)
                                val data = LineData(dset)
                                spinnerPosition=3
                                data
                            }

                            chart.xAxis.valueFormatter = ddday.let { MyXAxisFormatter(it) }
                            chart.setData(result2)
                            chart.invalidate()
                        }
                    }
                    4 -> {
                        CoroutineScope(Dispatchers.Main).launch {
                            lpb_buttonlist.addBottomPageButton(max_page,1)

                            var result = withContext(Dispatchers.IO) {
                                get_search_best_corr(position*2,1, ccode)
                            }
                            Log.d("stt",items.toString())
                            rvAdapter.notifyDataSetChanged() //읽어오는게 비동기처리되어 리사이클러뷰는 빈칸으로 나오기때문에 다시 리사이클러뷰를 불러와서 바꿔줘여한다
                            var result2 = withContext(Dispatchers.IO) {
                                var dset = get_normalize_stock("540", result)
                                val data = LineData(dset)
                                spinnerPosition=4
                                data
                            }

                            chart.xAxis.valueFormatter = ddday.let { MyXAxisFormatter(it) }
                            chart.setData(result2)
                            chart.invalidate()
                        }
                    }

                    else -> {
                        println("값이 10도 아니 20도 아닙니다.고")
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }


        val spinner_list2 = listOf("상관계수 높은 순","같은날 상한 Vi 걸린 횟수 높은 순")
        val spin_adapter2 = ArrayAdapter(this, R.layout.spinner_layout, spinner_list2)

        binding.spinner3.adapter = spin_adapter2
        binding.spinner3.selectedItem.toString()
        binding.spinner3.setSelection(0)

        binding.spinner3.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                when (position) {
                    0 -> {
                        binding.corr2.text = "상관계수"

                        choice =0

                        binding.spinner2.adapter = spin_adapter
                        binding.spinner2.selectedItem.toString()
                        binding.spinner2.setSelection(3)
                        //spinner에서 같은 값을 반복해서 선택하면 실행을 하지 않아서 adapter을 다시 연결해주었다.
                    }
                    1 -> {
                        binding.corr2.text = "Vi count"
                        choice =10
                        binding.spinner2.adapter = spin_adapter
                        binding.spinner2.selectedItem.toString()
                        binding.spinner2.setSelection(3)

                    }

                    else -> {
                        println("값이 10도 아니 20도 아닙니다.고")
                    }
                }
                Log.d("spinner",position.toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }

        //페이지버튼 눌렸을떄
        lpb_buttonlist.setOnPageSelectListener(object : OnPageSelectListener {
            //PrevButton Click
            override fun onPageBefore(now_page: Int) {
                //prev 버튼을 클릭하면 버튼이 재설정되고 버튼이 그려집니다.
                lpb_buttonlist.addBottomPageButton(max_page, now_page)
                //해당 페이지에 대한 소스 코드 작성
                //...
            }

            override fun onPageCenter(now_page: Int) {
                //Write source code for there page
                Toast.makeText(this@CompareActivity, "" + now_page, Toast.LENGTH_SHORT).show()
                CoroutineScope(Dispatchers.Main).launch {

                    var result = withContext(Dispatchers.IO) {
                        get_search_best_corr(spinnerPosition*2,now_page, ccode)
                    }
                    Log.d("stt",items.toString())
                    rvAdapter.notifyDataSetChanged() //읽어오는게 비동기처리되어 리사이클러뷰는 빈칸으로 나오기때문에 다시 리사이클러뷰를 불러와서 바꿔줘여한다
                    var result2 = withContext(Dispatchers.IO) {
                        var dayys =""
                        when(spinnerPosition){
                            0 -> dayys="30"
                            1 -> dayys ="90"
                            2 -> dayys ="180"
                            3 -> dayys ="365"
                            4 -> dayys ="540"
                        }
                        var dset = get_normalize_stock(dayys, result)
                        val data = LineData(dset)
                        data
                    }
                    chart.setData(result2)
                    chart.invalidate()

                }
                //...
            }

            //NextButton Click
            override fun onPageNext(now_page: Int) {
                //next 버튼을 클릭하면 버튼이 재설정되고 버튼이 그려집니다.
                lpb_buttonlist.addBottomPageButton(max_page, now_page)
            }
        })

        val name = db.collection("name").document("n")
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    var mm: MutableMap<kotlin.String, Any>? = document.data
                    if (mm != null) {
                        for ((rk, price) in mm) {
                            var tmp = price.toString().split(",")
                            stockName.addAll(tmp)
                        }
                    }
                }
            }

        //자동완성
        var autoCompleteTextView = findViewById<AutoCompleteTextView>(R.id.edittxt3)
        val adapter = ArrayAdapter<String>(this,
            android.R.layout.simple_gallery_item, stockName)
        autoCompleteTextView.setAdapter(adapter)
        binding.btn3.setOnClickListener {
            if(stockName.contains(binding.edittxt3.text.toString())){
                var rfd= rf.document(ccode)
                Log.d("kk",rf.toString())
                rfd.get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            var mm: MutableMap<kotlin.String, Any>? = document.data
                            if (mm != null) {
                                for (i in 1 until mm.size) {
                                    var spl = mm[i.toString()].toString().split(";")
                                    if (spl[spinnerPosition*2+choice]==binding.edittxt3.text.toString()){
                                        var pag = ceil(i.toFloat()/5).toInt()
                                        Log.d("ttdd",pag.toString())

                                        CoroutineScope(Dispatchers.Main).launch {
                                            lpb_buttonlist.addBottomPageButton(max_page,pag)

                                            var result = withContext(Dispatchers.IO) {
                                                get_search_best_corr(spinnerPosition*2,pag, ccode)
                                            }
                                            Log.d("stt",items.toString())
                                            rvAdapter.notifyDataSetChanged() //읽어오는게 비동기처리되어 리사이클러뷰는 빈칸으로 나오기때문에 다시 리사이클러뷰를 불러와서 바꿔줘여한다
                                            var result2 = withContext(Dispatchers.IO) {
                                                var dayys =""
                                                when(spinnerPosition){
                                                    0 -> dayys="30"
                                                    1 -> dayys ="90"
                                                    2 -> dayys ="180"
                                                    3 -> dayys ="365"
                                                    4 -> dayys ="540"
                                                }
                                                var dset = get_normalize_stock(dayys, result)
                                                val data = LineData(dset)
                                                data
                                            }

                                            chart.xAxis.valueFormatter = ddday.let { MyXAxisFormatter(it) }
                                            chart.setData(result2)
                                            chart.invalidate()

                                        }
                                    }

                                }
                        } else {
                            Toast.makeText(this@CompareActivity, "입력이 잘못 되었습니다", Toast.LENGTH_SHORT)
                                .show()

                        }
                    }
            }
        }
    }
}

}