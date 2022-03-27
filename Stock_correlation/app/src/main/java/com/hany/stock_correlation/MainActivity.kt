package com.hany.stock_correlation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.hany.stock_correlation.databinding.ActivityMainBinding
import com.hany.stock_correlation.recycle.ContentModel
import com.hany.stock_correlation.recycle.ContentRVAdapter
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    var items = ArrayList<ContentModel>()
    var rvAdapter = ContentRVAdapter(items)
    val spinner_list = listOf( "30일", "90일", "180일", "365일","540일")
    val spinner_list2 = listOf("상관계수 높은 순","같은날 상한 Vi 걸린 횟수 높은 순")
    var choice = 0
    val db = Firebase.firestore
    var stockName= ArrayList<String>()
    var waitTime = 0L
    override fun onBackPressed() {
        if(System.currentTimeMillis() - waitTime >=1500 ) {
            waitTime = System.currentTimeMillis()
            Toast.makeText(this,"뒤로가기 버튼을 한번 더 누르면 종료됩니다.",Toast.LENGTH_SHORT).show()
        } else {
            finish() // 액티비티 종료
        }
    }

    fun get_best_corr(days:String){
        items.clear()
        val rf = db.collection("highestCollection").document(days)
        rf.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    var mm: MutableMap<kotlin.String, Any>? = document.data
                    if (mm != null) {
                        for ((rk, price) in mm) {
                            var spl = price.toString().split(",")
                            items.add(
                                ContentModel(
                                    rk!!.toInt(),
                                    spl[0+choice],
                                    spl[1+choice],
                                    spl[2+choice]
                                ))
                        }
                        var days_list = ArrayList<String>()
                        items.sortBy(ContentModel::rank)
                        Log.d("itt", items.toString())
                        rvAdapter.notifyDataSetChanged() //읽어오는게 비동기처리되어 리사이클러뷰는 빈칸으로 나오기때문에 다시 리사이클러뷰를 불러와서 바꿔줘여한다
                    }
                }
            }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rv.adapter = rvAdapter
        binding.rv.layoutManager = LinearLayoutManager(this)
        val spin_adapter = ArrayAdapter(this, R.layout.spinner_layout, spinner_list)
        val spin_adapter2 = ArrayAdapter(this, R.layout.spinner_layout, spinner_list2)



        //종목명들 가져오기
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
                        PreferenceManager.setInt(baseContext, "count", stockName.size);
                    }
                }
            }

        //자동완성
        var autoCompleteTextView = findViewById<AutoCompleteTextView>(R.id.edittxt2)
        val adapter = ArrayAdapter<String>(this,
            android.R.layout.simple_dropdown_item_1line, stockName)
        autoCompleteTextView.setAdapter(adapter)

        // 종목 상관관계 검색
        binding.btn2.setOnClickListener {
            if(stockName.contains(binding.edittxt2.text.toString())){
                val intent = Intent(this, CompareActivity::class.java)
                intent.putExtra("code2", binding.edittxt2.text.toString())
                intent.putExtra("count", stockName.size)
                startActivity(intent)
            }
            else{
                Toast.makeText(this@MainActivity, "입력이 잘못 되었습니다", Toast.LENGTH_SHORT).show()

            }

        }


        // 스피너
        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                when (position) {
                    0 -> get_best_corr("30")
                    1 -> get_best_corr("90")
                    2 -> get_best_corr("180")
                    3 -> get_best_corr("365")
                    4 -> get_best_corr("540")

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

        binding.spinner2.adapter = spin_adapter2
        binding.spinner2.selectedItem.toString()
        binding.spinner2.setSelection(0)

        binding.spinner2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                when (position) {
                    0 -> {
                        choice =0
                        binding.corr.text= "상관 계수"
                        binding.spinner.adapter = spin_adapter
                        binding.spinner.selectedItem.toString()
                        binding.spinner.setSelection(3)                    }
                    1 -> {
                        binding.corr.text = "Vi count"
                        choice =3
                        binding.spinner.adapter = spin_adapter
                        binding.spinner.selectedItem.toString()
                        binding.spinner.setSelection(3)                    }

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

//        binding.btn.setOnClickListener {
//            var code = binding.edittxt.text.toString()
//            var candle_list = ArrayList<Candle>()
//            val docRef = db.collection("stock").document(code.toString())
//            docRef.get()
//                .addOnSuccessListener { document ->
//
//                    if (document != null) {
//                        var mm: MutableMap<kotlin.String, Any>? = document.data
//
//                        if (mm != null) {
//                            for ((day, price) in mm) {
//                                var spl = price.toString().split(",")
//                                candle_list.add(
//                                    Candle(
//                                        day!!.toInt(),
//                                        spl[0].toInt(),
//                                        spl[1].toInt(),
//                                        spl[2].toInt(),
//                                        spl[3].toInt()
//                                    ))
//                            }
//
//                            var days_list = ArrayList<String>()
//                            candle_list.sortBy(Candle::createdAt)
//
//                            for (i in 0 until candle_list.count()) {
//                                days_list.add(candle_list[i].createdAt.toString())
//                                candle_list[i].createdAt = i
//                            }
//
//                            Log.d("canddd", candle_list.toString())
//                            val intent = Intent(this, GraphActivity::class.java)
//                            intent.putExtra("code", binding.edittxt.text.toString())
//                            intent.putParcelableArrayListExtra("candle",candle_list)
//                            intent.putExtra("days",days_list)
//                            Log.d("days",days_list.toString())
//                            startActivity(intent)
//                        }
//                    }
//                }
//        }
    }
}