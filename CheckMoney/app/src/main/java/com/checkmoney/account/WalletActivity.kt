package com.checkmoney

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.checkmoney.account.CalTotal
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.hdodenhof.circleimageview.CircleImageView
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class WalletActivity : AppCompatActivity(), CalTotal,
    NavigationView.OnNavigationItemSelectedListener {
    private val gson = Gson()
    private val type = object : TypeToken<ErrorResult>() {}.type

    private lateinit var walletData: ProfileData
    private lateinit var moneyProfileAdapter: MoneyProfileAdapter
    private lateinit var profileAdapter: ProfileAdapter
    private lateinit var subsProfileAdapter: SubscriptionProfileAdaptor
    private lateinit var moneyRvProfile: RecyclerView
    private lateinit var rvProfile: RecyclerView
    private lateinit var layoutDrawer: DrawerLayout
    private lateinit var navHeader: View
    private lateinit var textWname: TextView
    private lateinit var textPrice: TextView
    private lateinit var imgProfile: ImageView
    private lateinit var btnLeft: ImageView
    private lateinit var btnRight: ImageView
    private lateinit var btnTotal: TextView
    private lateinit var btnExpense: TextView
    private lateinit var btnIncome: TextView
    private lateinit var btnNavi: ImageView
    private lateinit var btnAdd: FloatingActionButton
    private lateinit var btnAddTimer: FloatingActionButton
    private lateinit var textYm: TextView
    private lateinit var textEmail: TextView
    private lateinit var textName: TextView
    private lateinit var textMonth: TextView
    private lateinit var textPartPrice: TextView
    private lateinit var naviView: NavigationView

    private lateinit var edit_user_dlg: Dialog
    private lateinit var et_name: EditText
    private lateinit var et_oldPw: EditText
    private lateinit var et_pw: EditText
    private lateinit var et_pwConfirm: EditText
    private lateinit var text_pwRegular: TextView
    private lateinit var text_pwConfirmCheck: TextView
    private lateinit var text_EditCheck: TextView
    private lateinit var text_userEmail: TextView
    private lateinit var img_profile_dlg: CircleImageView
    private lateinit var btn_edit: Button
    private lateinit var btn_cancle: Button
    private lateinit var btn_getImage: TextView
    private lateinit var choiceImage: Bitmap
    private lateinit var body: MultipartBody.Part

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var bearerAccessToken: String

    private var editOldPassword = ""
    private var editUserPassword = ""
    private var editUserName = ""
    private var userName = ""
    private var userEmail = ""
    private var userProfile: String? = null
    private var name_count = 0
    private var pw_count = 1
    private var profile_count = 0

    @SuppressLint("SimpleDateFormat")
    private val df = SimpleDateFormat("yyyy/MM")

    @SuppressLint("SimpleDateFormat")
    private val yf = SimpleDateFormat("yyyy")

    @SuppressLint("SimpleDateFormat")
    private val mf = SimpleDateFormat("MM")

    @SuppressLint("SimpleDateFormat")
    private val qf = SimpleDateFormat("dd")

    private var refreshToken = RefreshToken(tokens.refresh_token, tokens.push_token)
    private var totalPrice: Long = 0
    private var accountId = -1
    private val REQUEST_OPEN_GALLERY: Int = 1
    private val REQ_PERMISSION_GALLERY = 1001
    private val TAG = "WalletActivity"
    private val TAG2 = "WalletActivity_API"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)

        // 변수 초기화
        setVariable()
        // drawer layout 사이즈 조절
        setLayoutSize()
        // 구글 세팅
        googleBuildIn()
        // access token, refresh token, 사용자 이메일을 LoginActivity에서 받아옴
        initSetting()
        // menu recyceler항목 추가
        menuRecycler()
        // 총액계산
        initTotalPrice()
        // 내 정보 받아오기
        getMyInfo(bearerAccessToken)
        // 내역 받아오기
        getTransaction(bearerAccessToken)

        getSubscriptions(bearerAccessToken)
        // 내정보수정 dlg setting
        setEditUserInfoDlg()

        // 전 월로 이동
        btnLeft.setOnClickListener {
            if (ThisTime.cal.get(Calendar.MONTH) == 0) {
                ThisTime.cal.add(Calendar.MONTH, 11)
                ThisTime.cal.add(Calendar.YEAR, -1)
            } else {
                ThisTime.cal.add(Calendar.MONTH, -1)
            }
            ((ThisTime.cal.get(Calendar.MONTH) + 1).toString() + "월 총액").also {
                textMonth.text = it
            }
            textYm.text = df.format(ThisTime.cal.time)
            btnTotal.callOnClick()
        }

        // 다음 월로 이동
        btnRight.setOnClickListener {
            if (ThisTime.cal.get(Calendar.MONTH) == 11) {
                ThisTime.cal.add(Calendar.MONTH, -11)
                ThisTime.cal.add(Calendar.YEAR, 1)
            } else {
                ThisTime.cal.add(Calendar.MONTH, 1)
            }
            ((ThisTime.cal.get(Calendar.MONTH) + 1).toString() + "월 총액").also {
                textMonth.text = it
            }
            textYm.text = df.format(ThisTime.cal.time)
            btnTotal.callOnClick()
        }

        // 합계 표시
        btnTotal.setOnClickListener {
            btnTotal.setTypeface(null, Typeface.BOLD)
            btnExpense.typeface = Typeface.DEFAULT
            btnIncome.typeface = Typeface.DEFAULT
            ListType.listype = ListType.TOTAL
            initRecycler()
            ((ThisTime.cal.get(Calendar.MONTH) + 1).toString() + "월 총액").also {
                textMonth.text = it
            }
            val filterDatas = (MoneyProfileDataList.datas.filter {
                (it.date.date.month == String.format(
                    "%02d",
                    (ThisTime.cal.get(Calendar.MONTH) + 1)
                )) && (it.date.date.year == (ThisTime.cal.get(Calendar.YEAR)
                    .toString()))
            }).toMutableList()
            var totalPrice = 0
            filterDatas.forEach {
                if (it.is_consumption == 0)
                    totalPrice += it.price
                else
                    totalPrice -= it.price
            }
            val format = DecimalFormat("#,###")
            var strPrice = format.format(totalPrice)
            strPrice += " 원"
            if (totalPrice >= 0) {
                textPartPrice.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.logopurple
                    )
                )
            } else
                textPartPrice.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.red
                    )
                )
            textPartPrice.text = strPrice
        }

        // 지출 표시
        btnExpense.setOnClickListener {
            btnTotal.typeface = Typeface.DEFAULT
            btnExpense.setTypeface(null, Typeface.BOLD)
            btnIncome.typeface = Typeface.DEFAULT
            ListType.listype = ListType.EXPENSE
            ((ThisTime.cal.get(Calendar.MONTH) + 1).toString() + "월 지출 합계").also {
                textMonth.text = it
            }
            val filterDatas = (MoneyProfileDataList.datas.filter {
                (it.date.date.month == String.format(
                    "%02d",
                    (ThisTime.cal.get(Calendar.MONTH) + 1)
                )) && (it.date.date.year == (ThisTime.cal.get(Calendar.YEAR)
                    .toString())) && (it.is_consumption == 1)
            }).toMutableList()
            var totalPrice = 0
            filterDatas.forEach {
                totalPrice += it.price
            }
            val format = DecimalFormat("#,###")
            var strPrice = format.format(totalPrice)
            strPrice += " 원"
            textPartPrice.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.red
                )
            )
            textPartPrice.text = strPrice
            expenseInitRecycler()
        }

        // 수입 표시
        btnIncome.setOnClickListener {
            btnTotal.typeface = Typeface.DEFAULT
            btnExpense.typeface = Typeface.DEFAULT
            btnIncome.setTypeface(null, Typeface.BOLD)
            ListType.listype = ListType.INCOME
            ((ThisTime.cal.get(Calendar.MONTH) + 1).toString() + "월 수입 합계").also {
                textMonth.text = it
            }
            val filterDatas = (MoneyProfileDataList.datas.filter {
                (it.date.date.month == String.format(
                    "%02d",
                    (ThisTime.cal.get(Calendar.MONTH) + 1)
                )) && (it.date.date.year == (ThisTime.cal.get(Calendar.YEAR)
                    .toString())) && (it.is_consumption == 0)
            }).toMutableList()
            var totalPrice = 0
            filterDatas.forEach {
                totalPrice += it.price
            }
            val format = DecimalFormat("#,###")
            var strPrice = format.format(totalPrice)
            strPrice += " 원"
            textPartPrice.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.logopurple
                )
            )
            textPartPrice.text = strPrice
            incomeInitRecycler()
        }

        // 내역 추가
        btnAdd.setOnClickListener {
            addDetail()
        }

        btnAddTimer.setOnClickListener {
            addSubscription()
        }

        btnNavi.setOnClickListener {
            layoutDrawer.openDrawer(GravityCompat.START)
        }

        btnTotal.callOnClick()
    }

    private fun calTotalPrice(is_consumption: Int, price: Int) {
        if (is_consumption == 0)
            totalPrice += price
        else
            totalPrice -= price
        val format = DecimalFormat("#,###")
        val strPrice = format.format(totalPrice)

        textPrice.text = strPrice + "원"
        if (totalPrice < 0)
            textPrice.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.red
                )
            )
        else
            textPrice.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.logoBlue
                )
            )
    }

    // 수입내역표시
    @SuppressLint("NotifyDataSetChanged")
    private fun incomeInitRecycler() {
        // 필터링으로 수입만 표시
        MoneyProfileDataList.datas.apply {
            val filterDatas = (MoneyProfileDataList.datas.filter {
                (it.date.date.month == String.format(
                    "%02d",
                    (ThisTime.cal.get(Calendar.MONTH) + 1)
                )) && (it.date.date.year == (ThisTime.cal.get(Calendar.YEAR)
                    .toString())) && (it.is_consumption == 0)
            }).toMutableList()
            moneyProfileAdapter.datas = filterDatas
            moneyProfileAdapter.notifyDataSetChanged()
        }
    }

    // 지출내역표시
    @SuppressLint("NotifyDataSetChanged")
    private fun expenseInitRecycler() {
        // 필터링으로 지출만 표시
        MoneyProfileDataList.datas.apply {
            val filterDatas = (MoneyProfileDataList.datas.filter {
                (it.date.date.month == String.format(
                    "%02d",
                    (ThisTime.cal.get(Calendar.MONTH) + 1)
                )) && (it.date.date.year == (ThisTime.cal.get(Calendar.YEAR)
                    .toString())) && (it.is_consumption == 1)
            }).toMutableList()
            moneyProfileAdapter.datas = filterDatas
            moneyProfileAdapter.notifyDataSetChanged()
        }
    }

    //사용내역추가
    private fun addDetail() {
        // 내역추가 dlg
        val dlg = Dialog(this@WalletActivity)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)   //타이틀바 제거
        dlg.setContentView(R.layout.dialog_datepicker)     //다이얼로그에 사용할 xml 파일을 불러옴
        dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dlg.show()

        val et_detail = dlg.findViewById<EditText>(R.id.et_detail)
        val et_price = dlg.findViewById<EditText>(R.id.et_price)
        val btn_choice = dlg.findViewById<Button>(R.id.btn_choice)
        val btn_cancle = dlg.findViewById<Button>(R.id.btn_cancel)
        val spinner = dlg.findViewById<Spinner>(R.id.spinner)
        val spinner2 = dlg.findViewById<Spinner>(R.id.spinner2)
        val text_alarm = dlg.findViewById<TextView>(R.id.text_alarm)

        val year: NumberPicker = dlg.findViewById(R.id.yearpicker_datepicker)
        val month: NumberPicker = dlg.findViewById(R.id.monthpicker_datepicker)
        val day: NumberPicker = dlg.findViewById(R.id.daypicker_datepicker)


        val adapter = ArrayAdapter(this, R.layout.spinner_layout, SpinnerArray.sData)
        spinner.adapter = adapter

        val adapter2 = ArrayAdapter(this, R.layout.spinner_layout, SpinnerArray.sData2)
        spinner2.adapter = adapter2

        // datepicker의 회전여부
        year.wrapSelectorWheel = false
        month.wrapSelectorWheel = false
        day.wrapSelectorWheel = false

        // 최소값 설정
        year.minValue = 2021
        month.minValue = 1
        day.minValue = 1

        // 최대값 설정
        year.maxValue = 2025
        month.maxValue = 12
        day.maxValue = 31

        // 수입, 지출 여부
        var is_consumtion = 0
        // 카테고리 포지션
        var category = 0

        // 현재 시각
        val c = System.currentTimeMillis()
        val a = Date(c)

        val strYear = yf.format(a)
        val strMonth = mf.format(a)
        val strDay = qf.format(a)

        year.value = strYear.toInt()
        month.value = strMonth.toInt()
        day.value = strDay.toInt()

        spinner.setSelection(0)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                category = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        spinner2.setSelection(1)
        spinner2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (SpinnerArray.sData2[position] == "지출") {
                    is_consumtion = 1
                } else
                    is_consumtion = 0
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        btn_choice.setOnClickListener {
            if (et_detail.text.toString() == "" || et_price.text.toString() == "") {
                text_alarm.text = "사용내역과 금액을 입력해 주세요."
            } else {
                val date = Date(
                    String.format("%02d", year.value),
                    String.format("%02d", month.value),
                    String.format("%02d", day.value),
                )
                calTotalPrice(is_consumtion, et_price.text.toString().toInt())
                // 날짜 데이터
                val dateData = MoneyProfileData(
                    is_consumption = is_consumtion, price = 0, detail = "", date = DateType(
                        0, MoneyProfileData.DATE_TYPE, date
                    ),
                    category = 0, account_id = accountId
                )
                // 내역 데이터
                val priceData = MoneyProfileData(
                    is_consumption = is_consumtion, price = et_price.text.toString().toInt(),
                    detail = et_detail.text.toString(), date = DateType(
                        0, MoneyProfileData.PRICE_TYPE, date
                    ), category = category, account_id = accountId
                )

                // 추가한 내역 서버로 전송
                val transaction = Transaction(
                    is_consumption = is_consumtion,
                    price = et_price.text.toString().toInt(),
                    detail = et_detail.text.toString(),
                    date = "${year.value}-${month.value}-${day.value}",
                    category = category,
                    account_id = accountId
                )
                postTransaction(bearerAccessToken, transaction, dateData, priceData)
                dlg.dismiss()
                text_alarm.text = ""
            }
        }

        btn_cancle.setOnClickListener {
            dlg.dismiss()
        }
    }

    private fun addSubscription() {
        val dlg = Dialog(this@WalletActivity)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)   //타이틀바 제거
        dlg.setContentView(R.layout.dialog_subscription_list)     //다이얼로그에 사용할 xml 파일을 불러옴
        dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dlg.show()

        val rvSubscription = dlg.findViewById<RecyclerView>(R.id.rv_subscription)
        val btnAdd = dlg.findViewById<Button>(R.id.btn_add)
        val btnCancle = dlg.findViewById<Button>(R.id.btn_cancel)

        rvSubscription.adapter = subsProfileAdapter

        btnAdd.setOnClickListener {
            val dlgDate = Dialog(this@WalletActivity)
            dlgDate.requestWindowFeature(Window.FEATURE_NO_TITLE)   //타이틀바 제거
            dlgDate.setContentView(R.layout.dialog_datepicker_subs)     //다이얼로그에 사용할 xml 파일을 불러옴
            dlgDate.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dlgDate.show()

            val et_detail = dlgDate.findViewById<EditText>(R.id.et_detail)
            val et_price = dlgDate.findViewById<EditText>(R.id.et_price)
            val btn_choice = dlgDate.findViewById<Button>(R.id.btn_choice)
            val btn_cancle = dlgDate.findViewById<Button>(R.id.btn_cancel)
            val spinner = dlgDate.findViewById<Spinner>(R.id.spinner)
            //val spinner2 = dlgDate.findViewById<Spinner>(R.id.spinner2)
            val text_alarm = dlgDate.findViewById<TextView>(R.id.text_alarm)

            val year: NumberPicker = dlgDate.findViewById(R.id.yearpicker_datepicker)
            val month: NumberPicker = dlgDate.findViewById(R.id.monthpicker_datepicker)
            val day: NumberPicker = dlgDate.findViewById(R.id.daypicker_datepicker)


            val adapter = ArrayAdapter(this, R.layout.spinner_layout, SpinnerArray.sData)
            spinner.adapter = adapter

            /*
            val adapter2 = ArrayAdapter(this, R.layout.spinner_layout, SpinnerArray.sData2)
            spinner2.adapter = adapter2
             */

            // datepicker의 회전여부
            year.wrapSelectorWheel = false
            month.wrapSelectorWheel = false
            day.wrapSelectorWheel = false

            // 최소값 설정
            year.minValue = 2021
            month.minValue = 1
            day.minValue = 1

            // 최대값 설정
            year.maxValue = 2025
            month.maxValue = 12
            day.maxValue = 31

            // 수입, 지출 여부
            //var is_consumtion = 0
            // 카테고리 포지션
            var category = 0

            // 현재 시각
            val c = System.currentTimeMillis()
            val a = Date(c)

            val strYear = yf.format(a)
            val strMonth = mf.format(a)
            val strDay = qf.format(a)

            year.value = strYear.toInt()
            month.value = strMonth.toInt()
            day.value = strDay.toInt()

            spinner.setSelection(0)
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    category = position
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    TODO("Not yet implemented")
                }
            }

            /*
            spinner2.setSelection(1)
            spinner2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (SpinnerArray.sData2[position] == "지출") {
                        is_consumtion = 1
                    } else
                        is_consumtion = 0
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    TODO("Not yet implemented")
                }
            }
             */

            btn_choice.setOnClickListener {
                if (et_detail.text.toString() == "" || et_price.text.toString() == "") {
                    text_alarm.text = "사용내역과 금액을 입력해 주세요."
                } else {
                    // 추가한 내역 서버로 전송
                    val transaction = Transaction(
                        is_consumption = 1,
                        price = et_price.text.toString().toInt(),
                        detail = et_detail.text.toString(),
                        date = "${year.value}-${month.value}-${day.value}",
                        category = category,
                        account_id = accountId
                    )
                    postSubscriptions(bearerAccessToken, transaction)
                    dlgDate.dismiss()
                    text_alarm.text = ""
                }
            }

            btn_cancle.setOnClickListener {
                dlgDate.dismiss()
            }
        }
        btnCancle.setOnClickListener {
            dlg.dismiss()
        }

    }

    //변수 초기화 및 세팅
    private fun setVariable() {
        layoutDrawer = findViewById(R.id.layout_drawer)
        naviView = findViewById(R.id.naviView)
        navHeader = naviView.getHeaderView(0)
        textWname = findViewById(R.id.text_wname)
        textPrice = findViewById(R.id.text_price)
        moneyRvProfile = findViewById(R.id.rv_profile)
        rvProfile = navHeader.findViewById(R.id.rv_profile)
        imgProfile = navHeader.findViewById(R.id.img_profile)
        btnLeft = findViewById(R.id.btn_left)
        btnRight = findViewById(R.id.btn_right)
        btnTotal = findViewById(R.id.btn_total)
        btnExpense = findViewById(R.id.btn_expense)
        btnIncome = findViewById(R.id.btn_income)
        btnNavi = findViewById(R.id.btn_navi)
        btnAdd = findViewById(R.id.btn_add)
        btnAddTimer = findViewById(R.id.btn_add_time)
        textYm = findViewById(R.id.text_ym)
        textEmail = navHeader.findViewById(R.id.text_email)
        textName = navHeader.findViewById(R.id.text_name)
        textMonth = findViewById(R.id.text_month)
        textPartPrice = findViewById(R.id.text_part_price)
    }

    //drawer layout 사이즈 조절
    private fun setLayoutSize() {
        val display = windowManager.defaultDisplay // in case of Activity
        val size = Point()
        display.getRealSize(size) // or getSize(size)
        val width = size.x * (0.8)
        val height = size.y * (0.67)
        navHeader.layoutParams.height = height.toInt()
        naviView.layoutParams.width = width.toInt()
    }

    //token과 유저정보 가져옴, 그 외 세팅
    private fun initSetting() {
        ThisTime.cal.time = Date()
        textYm.text = df.format(ThisTime.cal.time)
        walletData = intent.getParcelableExtra("data")!!
        textWname.text = walletData.title
        naviView.setNavigationItemSelectedListener(this)// 네비게이션 메뉴 아이템에 클릭 속성 부여

        SpinnerArray.sData = category.category
        SpinnerArray.sData2 = consumption.consumption

        accountId = intent.getIntExtra("accountId", -1)
        bearerAccessToken = "Bearer ${tokens.access_token}"

        subsProfileAdapter = SubscriptionProfileAdaptor(this, accountId)
        moneyProfileAdapter = MoneyProfileAdapter(this, this, accountId)
        moneyRvProfile.adapter = moneyProfileAdapter
    }

    //recycler항목 추가, 전체내역표시
    @SuppressLint("NotifyDataSetChanged")
    private fun initRecycler() {
        MoneyProfileDataList.datas.apply {
            val total_datas_list =
                MoneyProfileDataList.datas.distinctBy { MoneyProfileData -> MoneyProfileData.date }
            val filterDatas = total_datas_list.filter {
                (it.date.date.month == String.format(
                    "%02d",
                    (ThisTime.cal.get(Calendar.MONTH) + 1)
                )) && (it.date.date.year == (ThisTime.cal.get(Calendar.YEAR).toString()))
            }.toMutableList()
            moneyProfileAdapter.datas = filterDatas
            moneyProfileAdapter.notifyDataSetChanged()
        }
    }

    //통장목록표시
    @SuppressLint("NotifyDataSetChanged")
    private fun menuRecycler() {
        profileAdapter = ProfileAdapter(this, accountId, layoutDrawer)
        rvProfile.adapter = profileAdapter

        ProfileDataList.datas.apply {
            Log.d(TAG, "Profile Data list" + ProfileDataList.datas.toString())
            profileAdapter.datas = ProfileDataList.datas
            profileAdapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initTotalPrice() {
        val plus = MoneyProfileDataList.datas.filter { it.is_consumption == 0 }
        val minus = MoneyProfileDataList.datas.filter { it.is_consumption == 1 }
        plus.forEach { totalPrice += it.price }
        minus.forEach { totalPrice -= it.price }
        val format = DecimalFormat("#,###")
        val strPrice = format.format(totalPrice)
        textPrice.text = strPrice + "원"
    }

    // 네비게이션 메뉴 아이템 클릭 시 수행
    @SuppressLint("NotifyDataSetChanged")
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add -> {
                val dlg = Dialog(this@WalletActivity)
                dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)   //타이틀바 제거
                dlg.setContentView(R.layout.dialog_wallet_create)     //다이얼로그에 사용할 xml 파일을 불러옴
                dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dlg.show()

                val et_wname = dlg.findViewById<EditText>(R.id.et_wname)
                val et_description = dlg.findViewById<EditText>(R.id.et_description)
                val btn_create = dlg.findViewById<Button>(R.id.btn_create)
                val btn_cancle = dlg.findViewById<Button>(R.id.btn_cancel)

                btn_create.setOnClickListener {
                    val account = Account(
                        title = et_wname?.text.toString(),
                        description = et_description?.text.toString()
                    )
                    postAccount(bearerAccessToken, account)
                    dlg.dismiss()
                }
                btn_cancle.setOnClickListener {
                    dlg.dismiss()
                }
            }
            R.id.home -> {
                val mainIntent = Intent(this, MainActivity::class.java)
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(mainIntent)
            }
            // 내정보수정버튼 클릭 - 정보수정 dlg
            R.id.edit -> {
                editUserInfo()
            }
            // 로그아웃버튼 클릭 - 로그아웃
            R.id.logout -> {
                ProfileDataList.datas.clear()
                AppPref.prefs.clearUser(this)
                googleSignOut()
                val loginIntent = Intent(this, LoginActivity::class.java)
                startActivity(loginIntent)
                finish()
            }
        }
        return false
    }

    override fun calTotal(deleteConsum: Int, deletePrice: Int, addConsum: Int, addPrice: Int) {
        if (deleteConsum == 0)
            totalPrice -= deletePrice
        else
            totalPrice += deletePrice
        if (addConsum == 0)
            totalPrice += addPrice
        else
            totalPrice -= addPrice
        val format = DecimalFormat("#,###")
        val strPrice = format.format(totalPrice)
        textPrice.text = strPrice + "원"
        if (totalPrice < 0)
            textPrice.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.red
                )
            )
        else
            textPrice.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.logoBlue
                )
            )
    }

    override fun onBackPressed() {
        if (layoutDrawer.isDrawerOpen(GravityCompat.START)) {
            layoutDrawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed()
        }
    }

    //-----------------------------------------------------------------------
    //                             Edit My Info
    //-----------------------------------------------------------------------

    // 내정보수정 세팅
    private fun setEditUserInfoDlg() {
        edit_user_dlg = Dialog(this@WalletActivity)
        edit_user_dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)   //타이틀바 제거
        edit_user_dlg.setContentView(R.layout.dialog_userinfo_edit)     //다이얼로그에 사용할 xml 파일을 불러옴
        edit_user_dlg.window!!.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        edit_user_dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        et_name = edit_user_dlg.findViewById(R.id.et_name)
        et_oldPw = edit_user_dlg.findViewById(R.id.et_oldPassword)
        et_pw = edit_user_dlg.findViewById(R.id.et_password)
        et_pwConfirm = edit_user_dlg.findViewById(R.id.et_password_check)
        text_pwRegular = edit_user_dlg.findViewById(R.id.text_pw_regular)
        text_pwConfirmCheck = edit_user_dlg.findViewById(R.id.text_pw_confirm_result)
        text_EditCheck = edit_user_dlg.findViewById(R.id.text_edit_check)

        text_userEmail = edit_user_dlg.findViewById(R.id.text_userEmail)
        img_profile_dlg = edit_user_dlg.findViewById(R.id.img_profile)
        btn_edit = edit_user_dlg.findViewById(R.id.btn_edit)
        btn_cancle = edit_user_dlg.findViewById(R.id.btn_cancel)
        btn_getImage = edit_user_dlg.findViewById(R.id.btn_getImage)
    }

    // 내정보수정 dlg
    private fun editUserInfo() {
        edit_user_dlg.show()
        profile_count = 0
        getMyInfo(bearerAccessToken)
        if (userProfile == null) {
            img_profile_dlg.setImageResource(R.drawable.profile)
        } else {
            val url =
                "https://checkmoneyproject.azurewebsites.net/api$userProfile"
            Glide.with(this@WalletActivity).load(url).into(img_profile_dlg)
        }

        pwCheck()
        nameCheck()

        btn_edit.setOnClickListener {
            userEdit()
        }

        btn_cancle.setOnClickListener {
            edit_user_dlg.dismiss()
        }
        btn_getImage.setOnClickListener {
            val dlg = Dialog(this@WalletActivity)
            dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)   //타이틀바 제거
            dlg.setContentView(R.layout.dialog_choice_profile)     //다이얼로그에 사용할 xml 파일을 불러옴
            dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dlg.show()

            val btn_basic: Button = dlg.findViewById(R.id.btn_basic)
            val btn_gallery: Button = dlg.findViewById(R.id.btn_gallery)
            val btn_cancle: Button = dlg.findViewById(R.id.btn_cancel)

            btn_basic.setOnClickListener {
                imgProfile.setImageResource(R.drawable.profile)
                img_profile_dlg.setImageResource(R.drawable.profile)
                userProfile = null
                dlg.dismiss()
            }

            btn_gallery.setOnClickListener {
                try {
                    if (galleryPermissionGranted(edit_user_dlg)) {
                        openGallery()
                    }
                } catch (e: ActivityNotFoundException) {
                    Log.e(TAG, e.message.toString())
                }
                dlg.dismiss()
            }

            btn_cancle.setOnClickListener {
                dlg.dismiss()
            }
        }
    }

    // 갤러리 오픈
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_OPEN_GALLERY)
    }

    // 비밀번호 조건 체크
    private fun pwCheck() {
        var regular_count = 0
        et_oldPw.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                editOldPassword = et_oldPw.text.toString()
                if (editOldPassword == "" && editUserPassword == "") {
                    pw_count = 1
                } else if (editOldPassword != "" && editUserPassword == "") {
                    pw_count = 0
                }
            }
        })
        et_pw.addTextChangedListener(object : TextWatcher {
            var pw_first = et_pw.text.toString()
            var pw_check = et_pwConfirm.text.toString()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                pw_first = et_pw.text.toString()
                pw_check = et_pwConfirm.text.toString()
                editUserPassword = pw_first
                if (pw_first != "") {
                    val regex =
                        "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[$@$!%*#?&])[A-Za-z\\d$@$!%*#?&]{8,}$".toRegex()
                    if (!regex.containsMatchIn(pw_first)) {
                        text_pwRegular.text = "영문+숫자+특수문자를 포함하여 8자리 이상을 입력해 주세요."
                        regular_count = 0
                    } else {
                        text_pwRegular.text = ""
                        regular_count = 1
                    }
                } else {
                    text_pwRegular.text = ""
                }
                if (pw_check != "") {
                    if (pw_first == pw_check) {
                        text_pwConfirmCheck.setTextColor(
                            ContextCompat.getColor(
                                this@WalletActivity,
                                R.color.logoBlue
                            )
                        )
                        text_pwConfirmCheck.text = "비밀번호가 일치합니다."
                        pw_count = if (regular_count == 1) {
                            1
                        } else 0
                    } else {
                        text_pwConfirmCheck.setTextColor(
                            ContextCompat.getColor(
                                this@WalletActivity,
                                R.color.red
                            )
                        )
                        text_pwConfirmCheck.text = "비밀번호가 일치하지 않습니다."
                        pw_count = 0
                    }
                } else {
                    text_pwConfirmCheck.text = ""
                    pw_count = if (regular_count == 1) {
                        1
                    } else 0
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (editOldPassword == "" && editUserPassword == "") {
                    pw_count = 1
                }
            }
        })

        et_pwConfirm.addTextChangedListener(object : TextWatcher {
            var pw_first = et_pw.text.toString()
            var pw_check = et_pwConfirm.text.toString()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                pw_first = et_pw.text.toString()
                pw_check = et_pwConfirm.text.toString()
                if (pw_check != "") {
                    if (pw_first == pw_check) {
                        text_pwConfirmCheck.setTextColor(
                            ContextCompat.getColor(
                                this@WalletActivity,
                                R.color.logoBlue
                            )
                        )
                        text_pwConfirmCheck.text = "비밀번호가 일치합니다."
                        pw_count = if (regular_count == 1) {
                            1
                        } else 0
                    } else {
                        text_pwConfirmCheck.setTextColor(
                            ContextCompat.getColor(
                                this@WalletActivity,
                                R.color.red
                            )
                        )
                        text_pwConfirmCheck.text = "비밀번호가 일치하지 않습니다."
                        pw_count = 0
                    }
                } else {
                    text_pwConfirmCheck.text = ""
                    pw_count = 0
                }

            }

            override fun afterTextChanged(s: Editable?) {
                if (editOldPassword == "" && editUserPassword == "") {
                    pw_count = 1
                }
            }
        })
    }

    // 이름 적었나 체크
    private fun nameCheck() {
        et_name.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (et_name.text.toString() != "") {
                    name_count = 1
                    editUserName = et_name.text.toString()
                } else {
                    name_count = 0
                }
            }
        })
    }

    // 회원가입시 모두 입력했나 확인
    private fun userEdit() {
        if (pw_count == 1 && name_count == 1) {
            if (profile_count == 1) {
                postImage(bearerAccessToken, body)
                imgProfile.setImageBitmap(choiceImage)
            } else {
                if (editUserPassword == "") {
                    val myInfo = EditMyInfo(null, editUserName, null, null)
                    textName.text = editUserName
                    putMyInfo(bearerAccessToken, myInfo)
                } else {
                    val myInfo =
                        EditMyInfo(null, editUserName, editOldPassword, editUserPassword)
                    textName.text = editUserName
                    putMyInfo(bearerAccessToken, myInfo)
                }
            }
        } else {
            Log.d("!!!!!!!!!!!!!", pw_count.toString() + name_count.toString())
            text_EditCheck.text = "다시 한번 확인하여 주십시오."
        }
    }

    // 갤러리 오픈
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OPEN_GALLERY) {
            if (resultCode == RESULT_OK) {
                val currentImageUri = data?.data

                try {
                    currentImageUri?.let {
                        choiceImage = MediaStore.Images.Media.getBitmap(
                            this.contentResolver,
                            currentImageUri
                        )
                    }
                    val absolutePath = getFullPathFromUri(this, currentImageUri)
                    val file = File(absolutePath!!)
                    val requestFile =
                        RequestBody.create(MediaType.parse("MultipartBody.Part"), file)
                    body = MultipartBody.Part.createFormData("img", file.path, requestFile)
                    profile_count = 1
                    img_profile_dlg.setImageBitmap(choiceImage)
                } catch (e: Exception) {
                    //e.printStackTrace()
                }
            }
        }
    }

    // 절대경로 구하기
    fun getFullPathFromUri(ctx: Context, fileUri: Uri?): String? {
        var fullPath: String? = null
        val column = "_data"
        var cursor = ctx.contentResolver.query(fileUri!!, null, null, null, null)
        if (cursor != null) {
            cursor.moveToFirst()
            var document_id = cursor.getString(0)
            if (document_id == null) {
                for (i in 0 until cursor.columnCount) {
                    if (column.equals(cursor.getColumnName(i), ignoreCase = true)) {
                        fullPath = cursor.getString(i)
                        break
                    }
                }
            } else {
                document_id = document_id.substring(document_id.lastIndexOf(":") + 1)
                cursor.close()
                val projection = arrayOf(column)
                try {
                    cursor = ctx.contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        MediaStore.Images.Media._ID + " = ? ",
                        arrayOf(document_id),
                        null
                    )
                    if (cursor != null) {
                        cursor.moveToFirst()
                        fullPath = cursor.getString(cursor.getColumnIndexOrThrow(column))
                    }
                } finally {
                    cursor.close()
                }
            }
        }
        return fullPath
    }

    //-----------------------------------------------------------------------
    //                             Google Login
    //-----------------------------------------------------------------------
    // 구글 세팅
    private fun googleBuildIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("500159069581-m2dqev5jhbpumksnoodl7bmi90v5kjtl.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    //구글이메일 로그아웃
    private fun googleSignOut() { // 로그아웃
        // Google sign out
        googleSignInClient.signOut().addOnCompleteListener(this) {
            //updateUI(null)
            Log.d(TAG, "Logout success")
            /*
            googleSignInClient.revokeAccess().addOnCompleteListener(this){
                Log.d(TAG, "revokeAccess success")
            }
            */
        }
    }

    //-----------------------------------------------------------------------
    //                            Rest Api function
    //-----------------------------------------------------------------------
    // 계좌 생성
    private fun postAccount(accessToken: String, account: Account) {
        RetrofitBuild.api.postAccount(accessToken, account).enqueue(object : Callback<ResultId> {
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(call: Call<ResultId>, response: Response<ResultId>) {
                if (response.isSuccessful) { // <--> response.code == 200
                    Log.d(TAG2, "연결성공")
                    val responseApi = response.body()
                    Log.d(TAG2, responseApi.toString())
                    ProfileDataList.datas.apply {
                        add(
                            ProfileData(
                                title = account.title,
                                description = account.description,
                                id = responseApi!!.id
                            )
                        )
                        profileAdapter.datas = ProfileDataList.datas
                        profileAdapter.notifyDataSetChanged()
                    }
                } else { // code == 400
                    Log.d(TAG2, "연결실패")
                }
            }

            override fun onFailure(call: Call<ResultId>, t: Throwable) { // code == 500
                // 실패 처리
                Log.d(TAG2, "인터넷 네트워크 문제")
                Log.d(TAG2, t.toString())
            }
        })
    }

    // 내역 받아오기
    private fun getTransaction(accessToken: String) {
        val page = mapOf("page" to 1, "limit" to 1000)
        RetrofitBuild.api.getTransaction(accessToken, accountId, page)
            .enqueue(object : Callback<ResultTransactions> {
                override fun onResponse(
                    call: Call<ResultTransactions>,
                    response: Response<ResultTransactions>
                ) {
                    if (response.isSuccessful) { // <--> response.code == 200
                        Log.d(TAG2, "연결성공")
                        val responseApi = response.body()
                        Log.d(TAG2, responseApi.toString())
                        responseApi!!.rows.forEach {
                            val arr = it.date.split("-")
                            MoneyProfileDataList.datas.add(
                                MoneyProfileData(
                                    is_consumption = it.is_consumption,
                                    price = it.price,
                                    date = DateType(
                                        it.id,
                                        MoneyProfileData.PRICE_TYPE,
                                        Date(year = arr[0], month = arr[1], day = arr[2])
                                    ),
                                    detail = it.detail,
                                    category = it.category,
                                    account_id = it.account_id
                                )
                            )
                            MoneyProfileDataList.datas.add(
                                MoneyProfileData(
                                    is_consumption = it.is_consumption,
                                    price = 0,
                                    date = DateType(
                                        -1,
                                        MoneyProfileData.DATE_TYPE,
                                        Date(year = arr[0], month = arr[1], day = arr[2])
                                    ),
                                    detail = "",
                                    category = 0,
                                    account_id = it.account_id
                                )
                            )
                            calTotalPrice(it.is_consumption, it.price)
                        }
                        MoneyProfileDataList.datas =
                            MoneyProfileDataList.datas.distinct().toMutableList()
                        MoneyProfileDataList.datas.sortWith(compareByDescending<MoneyProfileData> { it.date.date.year }.thenByDescending { it.date.date.month }
                            .thenByDescending { it.date.date.day }
                            .thenByDescending { it.date.type })
                        btnTotal.callOnClick()
                    } else { // code == 400
                        Log.d(TAG2, "연결실패")
                    }
                }

                override fun onFailure(
                    call: Call<ResultTransactions>,
                    t: Throwable
                ) { // code == 500
                    // 실패 처리
                    Log.d(TAG2, "인터넷 네트워크 문제")
                    Log.d(TAG2, t.toString())
                }
            })
    }

    // 내역 추가
    private fun postTransaction(
        accessToken: String,
        transaction: Transaction,
        dateData: MoneyProfileData,
        priceData: MoneyProfileData
    ) {
        RetrofitBuild.api.postTransaction(accessToken, transaction)
            .enqueue(object : Callback<ResultId> {
                @SuppressLint("NotifyDataSetChanged")
                override fun onResponse(call: Call<ResultId>, response: Response<ResultId>) {
                    if (response.isSuccessful) { // <--> response.code == 200
                        Log.d(TAG2, "연결성공")
                        val responseApi = response.body()
                        dateData.date.id = -1
                        priceData.date.id = responseApi!!.id
                        MoneyProfileDataList.datas.apply {
                            add(dateData)
                            add(priceData)
                        }
                        MoneyProfileDataList.datas =
                            MoneyProfileDataList.datas.distinct().toMutableList()
                        MoneyProfileDataList.datas.sortWith(compareByDescending<MoneyProfileData> { it.date.date.year }.thenByDescending { it.date.date.month }
                            .thenByDescending { it.date.date.day }
                            .thenByDescending { it.date.type })
                        Log.d("!!!!!!!!!!!!!!!!!!!!!", MoneyProfileDataList.datas.toString())
                        btnTotal.callOnClick()
                        Log.d(TAG2, responseApi.toString())
                    } else { // code == 400
                        Log.d(TAG2, "연결실패")
                    }
                }

                override fun onFailure(call: Call<ResultId>, t: Throwable) { // code == 500
                    // 실패 처리
                    Log.d(TAG2, "인터넷 네트워크 문제")
                    Log.d(TAG2, t.toString())
                }
            })
    }

    private fun getMyInfo(accessToken: String) {
        RetrofitBuild.api.getMyInfo(accessToken).enqueue(object : Callback<ResultMyInfo> {
            override fun onResponse(call: Call<ResultMyInfo>, response: Response<ResultMyInfo>) {
                if (response.isSuccessful) { // <--> response.code == 200
                    Log.d(TAG2, "연결성공")
                    val responseApi = response.body()
                    Log.d(TAG2, responseApi.toString())

                    userName = responseApi!!.name
                    userEmail = responseApi.email
                    userProfile = responseApi.img_url
                    // 네비뷰 헤더 정보(이메일, 이름) 초기화
                    text_userEmail.text = userEmail
                    textEmail.text = userEmail
                    textName.text = userName
                    et_name.setText(userName)
                    if (userProfile == null) {
                        imgProfile.setImageResource(R.drawable.profile)
                    } else {
                        val url =
                            "https://checkmoneyproject.azurewebsites.net/api" + userProfile
                        Glide.with(this@WalletActivity).load(url).into(imgProfile)
                    }

                } else { // code == 400
                    val errorResponse: ErrorResult? =
                        gson.fromJson(response.errorBody()!!.charStream(), type)
                    Log.d(TAG2, "연결실패")
                    when (errorResponse!!.code) {
                        // access토큰 만료
                        40300 -> {
                            postRefresh(refreshToken)
                            getMyInfo(bearerAccessToken)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<ResultMyInfo>, t: Throwable) { // code == 500
                // 실패 처리
                Log.d(TAG2, "인터넷 네트워크 문제")
                Log.d(TAG2, t.toString())
            }
        })
    }

    private fun putMyInfo(accessToken: String, editMyInfo: EditMyInfo) {
        RetrofitBuild.api.putMyInfo(accessToken, editMyInfo).enqueue(object : Callback<Result> {
            override fun onResponse(call: Call<Result>, response: Response<Result>) {
                if (response.isSuccessful) { // <--> response.code == 200
                    Log.d(TAG2, "연결성공")
                    val responseApi = response.body()
                    Log.d(TAG2, responseApi.toString())
                    edit_user_dlg.dismiss()
                } else { // code == 400
                    val errorResponse: ErrorResult? =
                        gson.fromJson(response.errorBody()!!.charStream(), type)
                    Log.d(TAG2, "연결실패")
                    when (errorResponse!!.code) {
                        40007 -> {
                            postRefresh(refreshToken)
                            putMyInfo(bearerAccessToken, editMyInfo)
                        }
                        40008 -> text_EditCheck.text = "이전 비밀번호가 일치하지 않습니다."
                        // access토큰 만료
                        40300 -> {
                            postRefresh(refreshToken)
                            putMyInfo(bearerAccessToken, editMyInfo)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<Result>, t: Throwable) { // code == 500
                // 실패 처리
                Log.d(TAG2, "인터넷 네트워크 문제")
                Log.d(TAG2, t.toString())
            }
        })
    }

    private fun postImage(accessToken: String, body: MultipartBody.Part) {
        RetrofitBuild.api.postImage(accessToken, body).enqueue(object : Callback<ResultImageUrl> {
            override fun onResponse(
                call: Call<ResultImageUrl>,
                response: Response<ResultImageUrl>
            ) {
                if (response.isSuccessful) { // <--> response.code == 200
                    Log.d(TAG2, "연결성공")
                    val responseApi = response.body()
                    Log.d(TAG2, responseApi.toString())
                    userProfile = responseApi?.url
                    if (editUserPassword == "") {
                        val myInfo = EditMyInfo(userProfile, editUserName, null, null)
                        textName.text = editUserName
                        putMyInfo(bearerAccessToken, myInfo)
                    } else {
                        val myInfo =
                            EditMyInfo(userProfile, editUserName, editOldPassword, editUserPassword)
                        textName.text = editUserName
                        putMyInfo(bearerAccessToken, myInfo)
                    }
                } else { // code == 400
                    val errorResponse: ErrorResult? =
                        gson.fromJson(response.errorBody()!!.charStream(), type)
                    Log.d(TAG2, "연결실패")
                    when (errorResponse!!.code) {
                        // access토큰 만료
                        40300 -> {
                            postRefresh(refreshToken)
                            postImage(bearerAccessToken, body)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<ResultImageUrl>, t: Throwable) { // code == 500
                // 실패 처리
                Log.d(TAG2, "인터넷 네트워크 문제")
                Log.d(TAG2, t.toString())
            }
        })
    }

    // 고정지출 내역 받아오기
    private fun getSubscriptions(accessToken: String) {
        val page = mapOf("page" to 1, "limit" to 1000)
        RetrofitBuild.api.getSubscriptions(accessToken, accountId, page)
            .enqueue(object : Callback<ResultTransactions> {
                @SuppressLint("NotifyDataSetChanged")
                override fun onResponse(
                    call: Call<ResultTransactions>,
                    response: Response<ResultTransactions>
                ) {
                    if (response.isSuccessful) { // <--> response.code == 200
                        Log.d(TAG2, "연결성공")
                        val responseApi = response.body()
                        Log.d(TAG2, responseApi.toString())

                        responseApi!!.rows.forEach {
                            SubsProfileDataList.datas.add(
                                it
                            )
                        }
                        SubsProfileDataList.datas.sortBy { it.date.split("-")[2] }
                        SubsProfileDataList.datas.apply {
                            subsProfileAdapter.datas = SubsProfileDataList.datas
                            subsProfileAdapter.notifyDataSetChanged()
                        }
                    } else { // code == 400
                        val errorResponse: ErrorResult? =
                            gson.fromJson(response.errorBody()!!.charStream(), type)
                        Log.d(TAG2, "연결실패")
                        when (errorResponse!!.code) {
                            // access토큰 만료
                            40300 -> {
                                postRefresh(refreshToken)
                                getSubscriptions(bearerAccessToken)
                            }
                        }
                    }
                }

                override fun onFailure(
                    call: Call<ResultTransactions>,
                    t: Throwable
                ) { // code == 500
                    // 실패 처리
                    Log.d(TAG2, "인터넷 네트워크 문제")
                    Log.d(TAG2, t.toString())
                }
            })
    }

    // 고정지출 추가
    private fun postSubscriptions(
        accessToken: String,
        transaction: Transaction
    ) {
        RetrofitBuild.api.postSubscriptions(accessToken, transaction, accountId)
            .enqueue(object : Callback<ResultId> {
                @SuppressLint("NotifyDataSetChanged")
                override fun onResponse(call: Call<ResultId>, response: Response<ResultId>) {
                    if (response.isSuccessful) { // <--> response.code == 200
                        Log.d(TAG2, "연결성공")
                        val responseApi = response.body()
                        Log.d(TAG2, responseApi.toString())
                        val transactionModel = TransactionModel(
                            responseApi!!.id,
                            transaction.is_consumption,
                            transaction.price,
                            transaction.detail,
                            transaction.date,
                            transaction.category,
                            accountId
                        )
                        SubsProfileDataList.datas.apply {
                            add(transactionModel)
                            subsProfileAdapter.datas = SubsProfileDataList.datas
                            subsProfileAdapter.datas.sortBy { it.date.split("-")[2] }
                            subsProfileAdapter.notifyDataSetChanged()
                        }
                        Log.d("!!!!!!!!!!!!!!!!!", SubsProfileDataList.datas.toString())
                    } else { // code == 400
                        val errorResponse: ErrorResult? =
                            gson.fromJson(response.errorBody()!!.charStream(), type)
                        Log.d(TAG2, "연결실패")
                        when (errorResponse!!.code) {
                            // access토큰 만료
                            40300 -> {
                                postRefresh(refreshToken)
                                postSubscriptions(bearerAccessToken, transaction)
                            }
                        }
                    }
                }
                override fun onFailure(call: Call<ResultId>, t: Throwable) { // code == 500
                    // 실패 처리
                    Log.d(TAG2, "인터넷 네트워크 문제")
                    Log.d(TAG2, t.toString())
                }
            })
    }

    private fun postRefresh(refreshToken: RefreshToken) {
        RetrofitBuild.api.postRefresh(refreshToken).enqueue(object : Callback<ResultAndToken> {
            override fun onResponse(
                call: Call<ResultAndToken>,
                response: Response<ResultAndToken>
            ) {
                if (response.isSuccessful) { // <--> response.code == 200
                    Log.d(TAG2, "연결성공")
                    val responseApi = response.body()
                    Log.d(TAG2, responseApi.toString())
                    tokens.refresh_token = responseApi!!.refresh_token!!
                    tokens.access_token = responseApi.access_token!!
                    bearerAccessToken = "Bearer ${tokens.access_token}"
                } else { // code == 400
                    Log.d(TAG2, "연결실패")
                    //refresh토큰 만료시 로그아웃
                    AppPref.prefs.clearUser(this@WalletActivity)
                    googleSignOut()
                    val loginIntent = Intent(this@WalletActivity, LoginActivity::class.java)
                    startActivity(loginIntent)
                    finish()
                }
            }

            override fun onFailure(call: Call<ResultAndToken>, t: Throwable) { // code == 500
                // 실패 처리
                Log.d(TAG2, "인터넷 네트워크 문제")
                Log.d(TAG2, t.toString())
            }
        })
    }


    //-------------------------------------------------------------------------------------
    //                                     Permission
    //-------------------------------------------------------------------------------------

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQ_PERMISSION_GALLERY) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                Toast.makeText(this, "권한이 없어 해당 기능을 실행할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun galleryPermissionGranted(dlg: Dialog): Boolean {
        val preference = getPreferences(Context.MODE_PRIVATE)
        val isFirstCheck = preference.getBoolean("isFirstPermissionCheckGallery", true)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) ||
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                dlg.dismiss()
                // 거부할 경우 왜 필요한지 설명
                val snackBar = Snackbar.make(layoutDrawer, "권한이 필요합니다", Snackbar.LENGTH_INDEFINITE)
                snackBar.setAction("권한승인") {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ), REQ_PERMISSION_GALLERY
                    )
                }
                snackBar.show()
            } else {
                if (isFirstCheck) {
                    // 처음 물었는지 여부를 저장
                    preference.edit().putBoolean("isFirstPermissionCheckGallery", false).apply()
                    // 권한요청
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ), REQ_PERMISSION_GALLERY
                    )
                } else {
                    // 사용자가 권한을 거부하면서 다시 묻지않음 옵션을 선택한 경우
                    // requestPermission을 요청해도 창이 나타나지 않기 때문에 설정창으로 이동한
                    val snackBar = Snackbar.make(
                        layoutDrawer,
                        "권한이 필요합니다 확인을 누르시면 이동합니다",
                        Snackbar.LENGTH_INDEFINITE
                    )
                    snackBar.setAction("확인") {
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }
                    snackBar.show()
                }
            }
            return false
        } else {
            return true
        }
    }
}