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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.messaging.FirebaseMessaging
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
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var mBackWait:Long = 0
    private val gson = Gson()
    private val type = object : TypeToken<ErrorResult>() {}.type
    lateinit var context_main: Context

    private lateinit var profileAdapter: ProfileAdapter
    private lateinit var layoutDrawer: DrawerLayout
    private lateinit var navHeader: View
    private lateinit var rv_profile: RecyclerView
    private lateinit var btn_navi: ImageView
    private lateinit var img_profile: CircleImageView
    private lateinit var naviView: NavigationView
    private lateinit var text_email: TextView
    private lateinit var text_name: TextView

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
    private lateinit var body : MultipartBody.Part
    private lateinit var viewpager: ViewPager2
    private lateinit var tabs: TabLayout

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var bearerAccessToken: String
    private lateinit var choiceImage: Bitmap
    private lateinit var allTransaction: ArrayList<TransactionModel>

    private var editOldPassword = ""
    private var editUserPassword = ""
    private var editUserName = ""
    private var userName = ""
    private var userEmail = ""
    private var userProfile: String? = null
    private var name_count = 0
    private var pw_count = 1
    private var profile_count = 0

    private var refreshToken = RefreshToken(refresh_token = "", push_token = "")
    private val TAG = "MainActivity"
    private val TAG2 = "MainActivity_API"
    private var accountId = -1
    private val REQUEST_OPEN_GALLERY: Int = 1
    private val REQ_PERMISSION_GALLERY = 1001

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ?????? ?????????
        setVariable()
        // drawer layout ????????? ??????
        setLayoutSize()
        // ?????? ??????
        googleBuildIn()
        // access token, refresh token, ????????? ???????????? LoginActivity?????? ?????????
        initSetting()
        // ??? ?????? ????????????
        getMyInfo(bearerAccessToken)
        // ?????? ????????????
        getAccount(bearerAccessToken)
        // ??????????????? dlg setting
        setEditUserInfoDlg()
        //viewpager2, tablayout ?????? ??? ??????

        btn_navi.setOnClickListener {
            layoutDrawer.openDrawer(GravityCompat.START)
            profileAdapter.datas = ProfileDataList.datas
            profileAdapter.notifyDataSetChanged()
        }
    }

    override fun onStart() {
        super.onStart()
        // ?????? ?????? ???????????? ?????? ?????????
        getAllTransAction(bearerAccessToken)
    }

    // recycler?????? ??????
    @SuppressLint("NotifyDataSetChanged")
    private fun initRecycler(accountList: ArrayList<AccountModel>) {
        //????????? ??????
        profileAdapter = ProfileAdapter(this,accountId,layoutDrawer)
        rv_profile.adapter = profileAdapter

        ProfileDataList.datas.clear()
        // ????????? ?????? ??????
        ProfileDataList.datas.apply {
            accountList.forEach {
                add(ProfileData(title = it.title, description = it.description, id = it.id))
            }
            Log.d(TAG,"Profile Data list" + ProfileDataList.datas.toString())
            profileAdapter.datas = ProfileDataList.datas
            profileAdapter.notifyDataSetChanged()
        }
    }

    private fun renderViewPager() {
        viewpager.adapter = object : FragmentStateAdapter(this) {

            override fun createFragment(position: Int): Fragment {
                val bundle = Bundle()
                bundle.putParcelableArrayList("transaction", allTransaction)
                ResourceStore.pagerFragments[position].arguments = bundle
                return ResourceStore.pagerFragments[position]
            }

            override fun getItemCount(): Int {
                return ResourceStore.tabList.size
            }
        }
    }

    private fun renderTabLayer() {
        TabLayoutMediator(tabs, viewpager) { tab, position ->
            tab.text = ResourceStore.tabList[position]
        }.attach()
    }

    // ?????? ?????????
    @SuppressLint("CutPasteId")
    private fun setVariable() {
        layoutDrawer = findViewById(R.id.layout_drawer)
        btn_navi = findViewById(R.id.btn_navi)
        naviView = findViewById(R.id.naviView)
        navHeader = naviView.getHeaderView(0)
        rv_profile = navHeader.findViewById(R.id.rv_profile)
        img_profile = navHeader.findViewById(R.id.img_profile)
        text_email = navHeader.findViewById(R.id.text_email)
        text_name = navHeader.findViewById(R.id.text_name)
        viewpager = findViewById(R.id.viewpager)
        tabs = findViewById(R.id.tabs)
    }

    // drawer layout ????????? ??????
    private fun setLayoutSize() {
        val display = windowManager.defaultDisplay // in case of Activity
        val size = Point()
        display.getRealSize(size) // or getSize(size)
        // drawer ????????????
        val width = size.x * (0.8)
        // drawer ????????????
        val height = size.y * (0.67)
        // ?????? ??????
        navHeader.layoutParams.height = height.toInt()
        naviView.layoutParams.width= width.toInt()
    }

    // access token, refresh token, ????????? ???????????? LoginActivity?????? ?????????
    private fun initSetting() {
        // ??????????????? ?????? ???????????? ?????? ?????? ??????
        naviView.setNavigationItemSelectedListener(this)
        // ???????????????????????? ????????? ?????????
        context_main = this
        // refresfh?????? ?????????
        refreshToken.refresh_token = tokens.refresh_token
        // push?????? ?????????
        refreshToken.push_token = tokens.push_token
        // access?????? ?????????
        bearerAccessToken = "Bearer ${tokens.access_token}"

    }

    // ??????????????? ?????? ????????? ?????? ??? ??????
    @SuppressLint("NotifyDataSetChanged")
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // ?????????????????? ?????? - ???????????? dlg
           R.id.add -> {
                val dlg = Dialog(this@MainActivity)
                dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)   //???????????? ??????
                dlg.setContentView(R.layout.dialog_wallet_create)     //?????????????????? ????????? xml ????????? ?????????
                dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dlg.show()

                val et_wname = dlg.findViewById<EditText>(R.id.et_wname)
                val et_description = dlg.findViewById<EditText>(R.id.et_description)
                val btn_create = dlg.findViewById<Button>(R.id.btn_create)
                val btn_cancle = dlg.findViewById<Button>(R.id.btn_cancel)

               // ?????? ??????
                btn_create.setOnClickListener {
                    val account = Account(title = et_wname?.text.toString(), description = et_description?.text.toString())
                    postAccount(bearerAccessToken, account)
                    dlg.dismiss()
                }
                btn_cancle.setOnClickListener {
                    dlg.dismiss()
                }
            }
            // ????????? ?????? - ????????????
            R.id.home -> {
                layoutDrawer.closeDrawer(GravityCompat.START)
            }
            // ????????????????????? ?????? - ???????????? dlg
            R.id.edit -> {
                editUserInfo()
            }
            // ?????????????????? ?????? - ????????????
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

    // ???????????? ???????????? ??? ??? ??????
    override fun onBackPressed() {
        // drawer??? ??????????????? ??????
        if (layoutDrawer.isDrawerOpen(GravityCompat.START)) {
            layoutDrawer.closeDrawer(GravityCompat.START)
        }
        // ?????? ??? ??????????????? ??? ??????
        else {
            if (System.currentTimeMillis() - mBackWait >= 2000) {
                mBackWait = System.currentTimeMillis()
                Toast.makeText(this, " ??? ??? ??? ????????? ???????????????.", Toast.LENGTH_LONG).show()
            } else {
                finishAffinity()
                System.runFinalization()
                System.exit(0)
            }
        }
    }

    //-----------------------------------------------------------------------
    //                             Edit My Info
    //-----------------------------------------------------------------------

    // ??????????????? ??????
    private fun setEditUserInfoDlg(){
        edit_user_dlg = Dialog(this@MainActivity)
        edit_user_dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)   //???????????? ??????
        edit_user_dlg.setContentView(R.layout.dialog_userinfo_edit)     //?????????????????? ????????? xml ????????? ?????????
        edit_user_dlg.window!!.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT)
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

    // ??????????????? dlg
    private fun editUserInfo() {
        edit_user_dlg.show()
        profile_count = 0
        getMyInfo(bearerAccessToken)
        if(userProfile == null) {
            img_profile_dlg.setImageResource(R.drawable.profile)
        }
        else{
            val url =
                "https://checkmoneyproject.azurewebsites.net/api$userProfile"
            Glide.with(this@MainActivity).load(url).into(img_profile_dlg)
        }

        pwCheck()
        nameCheck()

        btn_edit.setOnClickListener {
            userEdit()
        }

        btn_cancle.setOnClickListener {
            text_EditCheck.text = ""
            edit_user_dlg.dismiss()
        }
        btn_getImage.setOnClickListener {
            val dlg = Dialog(this@MainActivity)
            dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)   //???????????? ??????
            dlg.setContentView(R.layout.dialog_choice_profile)     //?????????????????? ????????? xml ????????? ?????????
            dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dlg.show()

            val btn_basic: Button = dlg.findViewById(R.id.btn_basic)
            val btn_gallery: Button = dlg.findViewById(R.id.btn_gallery)
            val btn_cancle: Button = dlg.findViewById(R.id.btn_cancel)

            btn_basic.setOnClickListener {
                img_profile.setImageResource(R.drawable.profile)
                img_profile_dlg.setImageResource(R.drawable.profile)
                userProfile = null
                dlg.dismiss()
            }

            btn_gallery.setOnClickListener {
                try {
                    if(galleryPermissionGranted(edit_user_dlg)) {
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

    // ????????? ??????
    private fun openGallery(){
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_OPEN_GALLERY)
    }

    // ???????????? ?????? ??????
    private fun pwCheck() {
        var regular_count = 0
        et_oldPw.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                editOldPassword = et_oldPw.text.toString()
                if(editOldPassword == "" && editUserPassword == ""){
                    pw_count = 1
                }else if(editOldPassword != "" && editUserPassword == ""){
                    pw_count = 0
                }
            }
        })
        et_pw.addTextChangedListener(object: TextWatcher {
            var pw_first = et_pw.text.toString()
            var pw_check = et_pwConfirm.text.toString()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                pw_first = et_pw.text.toString()
                pw_check = et_pwConfirm.text.toString()
                editUserPassword = pw_first
                if(pw_first != ""){
                    val regex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[$@$!%*#?&])[A-Za-z\\d$@$!%*#?&]{8,}$".toRegex()
                    if (!regex.containsMatchIn(pw_first)){
                        text_pwRegular.text = "??????+??????+??????????????? ???????????? 8?????? ????????? ????????? ?????????."
                        regular_count = 0
                    }
                    else{
                        text_pwRegular.text = ""
                        regular_count = 1
                    }
                }
                else{
                    text_pwRegular.text = ""
                }
                if(pw_check != "") {
                    if (pw_first == pw_check) {
                        text_pwConfirmCheck.setTextColor(
                            ContextCompat.getColor(
                                this@MainActivity,
                                R.color.logoBlue
                            )
                        )
                        text_pwConfirmCheck.text = "??????????????? ???????????????."
                        pw_count = if(regular_count == 1) {
                            1
                        }else 0
                    } else {
                        text_pwConfirmCheck.setTextColor(ContextCompat.getColor(this@MainActivity,
                            R.color.red
                        ))
                        text_pwConfirmCheck.text = "??????????????? ???????????? ????????????."
                        pw_count = 0
                    }
                }
                else {
                    text_pwConfirmCheck.text = ""
                    pw_count = if(regular_count == 1) {
                        1
                    }else 0
                }
            }
            override fun afterTextChanged(s: Editable?) {
                if(editOldPassword == "" && editUserPassword == ""){
                    pw_count = 1
                }
            }
        })

        et_pwConfirm.addTextChangedListener(object: TextWatcher {
            var pw_first = et_pw.text.toString()
            var pw_check = et_pwConfirm.text.toString()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                pw_first = et_pw.text.toString()
                pw_check = et_pwConfirm.text.toString()
                if(pw_check != "") {
                    if (pw_first == pw_check) {
                        text_pwConfirmCheck.setTextColor(
                            ContextCompat.getColor(
                                this@MainActivity,
                                R.color.logoBlue
                            )
                        )
                        text_pwConfirmCheck.text = "??????????????? ???????????????."
                        pw_count = if(regular_count == 1) {
                            1
                        }else 0
                    } else {
                        text_pwConfirmCheck.setTextColor(ContextCompat.getColor(this@MainActivity,
                            R.color.red
                        ))
                        text_pwConfirmCheck.text = "??????????????? ???????????? ????????????."
                        pw_count = 0
                    }
                }
                else {
                    text_pwConfirmCheck.text = ""
                    pw_count = 0
                }

            }
            override fun afterTextChanged(s: Editable?) {
                if(editOldPassword == "" && editUserPassword == ""){
                    pw_count = 1
                }
            }
        })
    }

    // ?????? ????????? ??????
    private fun nameCheck(){
        et_name.addTextChangedListener(object:TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
            override fun afterTextChanged(s: Editable?) {
                if(et_name.text.toString() != ""){
                    name_count = 1
                    editUserName = et_name.text.toString()
                }
                else{
                    name_count = 0
                }
            }
        })
    }

    // ?????? ??????
    private fun userEdit() {
        if(pw_count == 1 && name_count == 1){
            if(profile_count == 1) {
                postImage(bearerAccessToken, body)
                img_profile.setImageBitmap(choiceImage)
            }
            else {
                if (editUserPassword == "") {
                    val myInfo = EditMyInfo(null, editUserName, null, null)
                    text_name.text = editUserName
                    putMyInfo(bearerAccessToken, myInfo)
                } else {
                    val myInfo =
                        EditMyInfo(null, editUserName, editOldPassword, editUserPassword)
                    text_name.text = editUserName
                    putMyInfo(bearerAccessToken, myInfo)
                }
            }
        }
        else{
            text_EditCheck.text="?????? ?????? ???????????? ????????????."
        }
    }

    // ????????? ??????
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_OPEN_GALLERY) {
            if(resultCode == RESULT_OK) {
                val currentImageUri = data?.data

                try{
                    currentImageUri?.let {
                        choiceImage = MediaStore.Images.Media.getBitmap(
                            this.contentResolver,
                            currentImageUri
                        )
                    }
                    val absolutePath = getFullPathFromUri(this,currentImageUri)
                    val file = File(absolutePath!!)
                    val requestFile = RequestBody.create(MediaType.parse("MultipartBody.Part"), file)
                    body = MultipartBody.Part.createFormData("img", file.path,requestFile)
                    profile_count = 1
                    img_profile_dlg.setImageBitmap(choiceImage)
                }catch(e: Exception) {
                    //e.printStackTrace()
                }
            }
        }
    }

    // ???????????? ?????????
    private fun getFullPathFromUri(ctx: Context, fileUri: Uri?): String? {
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
    // ?????? ??????
    private fun googleBuildIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("500159069581-m2dqev5jhbpumksnoodl7bmi90v5kjtl.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }
    //??????????????? ????????????
    private fun googleSignOut() { // ????????????
        // Google sign out
        googleSignInClient.signOut().addOnCompleteListener(this) {
            //updateUI(null)
            Log.d(TAG, "Logout success")
            /* ????????????
            googleSignInClient.revokeAccess().addOnCompleteListener(this){
                Log.d(TAG, "revokeAccess success")
            }
            */
        }
    }

    //-----------------------------------------------------------------------
    //                            Rest Api function
    //-----------------------------------------------------------------------

    private fun getAccount(accessToken: String) {
        val page = mapOf("page" to 1, "limit" to 1000)
        RetrofitBuild.api.getAccount(accessToken, page).enqueue(object : Callback<ResultAccountList> {
            override fun onResponse(call: Call<ResultAccountList>, response: Response<ResultAccountList>) {
                if(response.isSuccessful) { // <--> response.code == 200
                    Log.d(TAG2, "????????????")
                    val responseApi = response.body()
                    Log.d(TAG2,responseApi.toString())
                    // ????????? ?????? ??????
                    initRecycler(response.body()!!.rows)
                } else { // code == 400
                    val errorResponse: ErrorResult? = gson.fromJson(response.errorBody()!!.charStream(), type)
                    Log.d(TAG2, "????????????")
                    when(errorResponse!!.code){
                        // access?????? ??????
                        40300 -> {
                            postRefresh(refreshToken)
                            getAccount(bearerAccessToken)
                        }
                    }
                }
            }
            override fun onFailure(call: Call<ResultAccountList>, t: Throwable) { // code == 500
                // ?????? ??????
                Log.d(TAG2, "????????? ???????????? ??????")
                Log.d(TAG2, t.toString())
            }
        })
    }

    private fun postAccount(accessToken: String, account: Account) {
        RetrofitBuild.api.postAccount(accessToken, account).enqueue(object : Callback<ResultId> {
            override fun onResponse(call: Call<ResultId>, response: Response<ResultId>) {
                if(response.isSuccessful) { // <--> response.code == 200
                    Log.d(TAG2, "????????????")
                    val responseApi = response.body()
                    Log.d(TAG2,responseApi.toString())
                    // ?????? ??????
                    ProfileDataList.datas.apply {
                        add(ProfileData(title = account.title, description = account.description ,id = responseApi!!.id))
                        profileAdapter.datas = ProfileDataList.datas
                        profileAdapter.notifyDataSetChanged()
                    }
                } else { // code == 400
                    val errorResponse: ErrorResult? = gson.fromJson(response.errorBody()!!.charStream(), type)
                    Log.d(TAG2, "????????????")
                    when(errorResponse!!.code){
                        // access?????? ??????
                        40300 -> {
                            postRefresh(refreshToken)
                            postAccount(bearerAccessToken, account)
                        }
                    }
                }
            }
            override fun onFailure(call: Call<ResultId>, t: Throwable) { // code == 500
                // ?????? ??????
                Log.d(TAG2, "????????? ???????????? ??????")
                Log.d(TAG2, t.toString())
            }
        })
    }

    private fun getMyInfo(accessToken: String){
        RetrofitBuild.api.getMyInfo(accessToken).enqueue(object : Callback<ResultMyInfo> {
            override fun onResponse(call: Call<ResultMyInfo>, response: Response<ResultMyInfo>) {
                if(response.isSuccessful) { // <--> response.code == 200
                    Log.d(TAG2, "????????????")
                    val responseApi = response.body()
                    Log.d(TAG2,responseApi.toString())

                    userName = responseApi!!.name
                    userEmail = responseApi.email
                    userProfile = responseApi.img_url
                    // ????????? ?????? ??????(?????????, ??????) ?????????
                    text_userEmail.text = userEmail
                    text_email.text = userEmail
                    text_name.text = userName
                    et_name.setText(userName)
                    if(userProfile == null){
                        img_profile.setImageResource(R.drawable.profile)
                    }
                    else{
                        val url = "https://checkmoneyproject.azurewebsites.net/api" + userProfile
                        Glide.with(this@MainActivity).load(url).into(img_profile)
                    }

                } else { // code == 400
                    val errorResponse: ErrorResult? = gson.fromJson(response.errorBody()!!.charStream(), type)
                    Log.d(TAG2, "????????????")
                    when(errorResponse!!.code){
                        // access?????? ??????
                        40300 -> {
                            postRefresh(refreshToken)
                            getMyInfo(bearerAccessToken)
                        }
                    }
                }
            }
            override fun onFailure(call: Call<ResultMyInfo>, t: Throwable) { // code == 500
                // ?????? ??????
                Log.d(TAG2, "????????? ???????????? ??????")
                Log.d(TAG2, t.toString())
            }
        })
    }

    private fun putMyInfo(accessToken: String, editMyInfo: EditMyInfo){
        RetrofitBuild.api.putMyInfo(accessToken,editMyInfo).enqueue(object : Callback<Result> {
            override fun onResponse(call: Call<Result>, response: Response<Result>) {
                if(response.isSuccessful) { // <--> response.code == 200
                    Log.d(TAG2, "????????????")
                    val responseApi = response.body()
                    Log.d(TAG2,responseApi.toString())
                    edit_user_dlg.dismiss()
                } else { // code == 400
                    val errorResponse: ErrorResult? = gson.fromJson(response.errorBody()!!.charStream(), type)
                    Log.d(TAG2, "????????????")
                    when(errorResponse!!.code){
                        40007 -> {
                            postRefresh(refreshToken)
                            putMyInfo(bearerAccessToken, editMyInfo)
                        }
                        40008 -> text_EditCheck.text = "?????? ??????????????? ???????????? ????????????."
                        // access?????? ??????
                        40300 -> {
                            postRefresh(refreshToken)
                            putMyInfo(bearerAccessToken, editMyInfo)
                        }
                    }
                }
            }
            override fun onFailure(call: Call<Result>, t: Throwable) { // code == 500
                // ?????? ??????
                Log.d(TAG2, "????????? ???????????? ??????")
                Log.d(TAG2, t.toString())
            }
        })
    }

    private fun postImage(accessToken: String, body: MultipartBody.Part){
        RetrofitBuild.api.postImage(accessToken,body).enqueue(object : Callback<ResultImageUrl> {
            override fun onResponse(call: Call<ResultImageUrl>, response: Response<ResultImageUrl>) {
                if(response.isSuccessful) { // <--> response.code == 200
                    Log.d(TAG2, "????????????")
                    val responseApi = response.body()
                    Log.d(TAG2,responseApi.toString())
                    userProfile = responseApi?.url
                    if (editUserPassword == "") {
                        val myInfo = EditMyInfo(userProfile, editUserName, null, null)
                        text_name.text = editUserName
                        putMyInfo(bearerAccessToken, myInfo)
                    } else {
                        val myInfo =
                            EditMyInfo(userProfile, editUserName, editOldPassword, editUserPassword)
                        text_name.text = editUserName
                        putMyInfo(bearerAccessToken, myInfo)
                    }
                } else { // code == 400
                    val errorResponse: ErrorResult? = gson.fromJson(response.errorBody()!!.charStream(), type)
                    Log.d(TAG2, "????????????")
                    when(errorResponse!!.code){
                        // access?????? ??????
                        40300 -> {
                            postRefresh(refreshToken)
                            postImage(bearerAccessToken, body)
                        }
                    }
                }
            }
            override fun onFailure(call: Call<ResultImageUrl>, t: Throwable) { // code == 500
                // ?????? ??????
                Log.d(TAG2, "????????? ???????????? ??????")
                Log.d(TAG2, t.toString())
            }
        })
    }

    private fun postRefresh(refreshToken: RefreshToken){
        RetrofitBuild.api.postRefresh(refreshToken).enqueue(object : Callback<ResultAndToken> {
            override fun onResponse(call: Call<ResultAndToken>, response: Response<ResultAndToken>) {
                if(response.isSuccessful) { // <--> response.code == 200
                    Log.d(TAG2, "????????????")
                    val responseApi = response.body()
                    Log.d(TAG2,responseApi.toString())
                    tokens.refresh_token = responseApi!!.refresh_token!!
                    tokens.access_token = responseApi.access_token!!
                    this@MainActivity.refreshToken = RefreshToken(tokens.refresh_token,tokens.push_token)
                    bearerAccessToken = "Bearer ${tokens.access_token}"
                } else { // code == 400
                    Log.d(TAG2, "????????????")
                    //refresh?????? ????????? ????????????
                    AppPref.prefs.clearUser(this@MainActivity)
                    googleSignOut()
                    val loginIntent = Intent(this@MainActivity, LoginActivity::class.java)
                    startActivity(loginIntent)
                    finish()
                }
            }
            override fun onFailure(call: Call<ResultAndToken>, t: Throwable) { // code == 500
                // ?????? ??????
                Log.d(TAG2, "????????? ???????????? ??????")
                Log.d(TAG2, t.toString())
            }
        })
    }

    private fun getAllTransAction(accessToken: String){
        val page = mapOf("page" to 1, "limit" to 1000)
        RetrofitBuild.api.getAllTransaction(accessToken, page).enqueue(object : Callback<ResultTransactions> {
            override fun onResponse(call: Call<ResultTransactions>, response: Response<ResultTransactions>) {
                if(response.isSuccessful) { // <--> response.code == 200
                    Log.d(TAG2, "????????????")
                    val responseApi = response.body()
                    Log.d(TAG2,responseApi.toString())
                    allTransaction = responseApi!!.rows
                    // viewpager ??????
                    renderViewPager()
                    renderTabLayer()
                } else { // code == 400
                    val errorResponse: ErrorResult? = gson.fromJson(response.errorBody()!!.charStream(), type)
                    Log.d(TAG2, "????????????")
                    when(errorResponse!!.code){
                        // access?????? ??????
                        40300 -> {
                            postRefresh(refreshToken)
                            postImage(bearerAccessToken, body)
                        }
                    }
                }
            }
            override fun onFailure(call: Call<ResultTransactions>, t: Throwable) { // code == 500
                // ?????? ??????
                Log.d(TAG2, "????????? ???????????? ??????")
                Log.d(TAG2, t.toString())
            }
        })
    }

    //-------------------------------------------------------------------------------------
    //                                      Permission
    //-------------------------------------------------------------------------------------

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQ_PERMISSION_GALLERY){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                openGallery()
            } else{
                Toast.makeText(this,"????????? ?????? ?????? ????????? ????????? ??? ????????????.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun galleryPermissionGranted(dlg: Dialog): Boolean {
        val preference = getPreferences(Context.MODE_PRIVATE)
        val isFirstCheck = preference.getBoolean("isFirstPermissionCheckGallery", true)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    dlg.dismiss()
                // ????????? ?????? ??? ???????????? ??????
                val snackBar = Snackbar.make(layoutDrawer, "????????? ???????????????", Snackbar.LENGTH_INDEFINITE)
                snackBar.setAction("????????????") {
                    ActivityCompat.requestPermissions(this,
                        arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ), REQ_PERMISSION_GALLERY
                    )
                }
                snackBar.show()
            } else {
                if (isFirstCheck) {
                    // ?????? ???????????? ????????? ??????
                    preference.edit().putBoolean("isFirstPermissionCheckGallery", false).apply()
                    // ????????????
                    ActivityCompat.requestPermissions(this,
                        arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ), REQ_PERMISSION_GALLERY
                    )
                } else {
                    // ???????????? ????????? ??????????????? ?????? ???????????? ????????? ????????? ??????
                    // requestPermission??? ???????????? ?????? ???????????? ?????? ????????? ??????????????? ?????????
                    val snackBar = Snackbar.make(layoutDrawer, "????????? ??????????????? ????????? ???????????? ???????????????", Snackbar.LENGTH_INDEFINITE)
                    snackBar.setAction("??????") {
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