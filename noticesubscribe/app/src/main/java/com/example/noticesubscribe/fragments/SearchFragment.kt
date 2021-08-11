package com.example.noticesubscribe.fragments

import android.content.Intent
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.system.Os.remove
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.noticesubscribe.*
import com.example.noticesubscribe.databinding.FragmentSearchBinding
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*


class SearchFragment : Fragment() {
    //공지검색
    //mbinding을 통해 네비게이션 바를 이용해서 이동할 수 있는 fragment를 만듦
    private var mBinding: FragmentSearchBinding? = null
    val db = FirebaseFirestore.getInstance()
    var mDocuments: List<DocumentSnapshot>? = null

    val historylist = arrayListOf<Searchhistory>()
    val historyadapter = HistoryAdapter(historylist)


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding = FragmentSearchBinding.inflate(inflater, container, false)

        mBinding = binding

        return mBinding?.root
    }

    override fun onDestroyView() {
        mBinding = null
        super.onDestroyView()
    }

    //검색 내용에 해당하는 공지들이 표시될 recyclerlist 넣기
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_searchnotice)
        val notice_list = arrayListOf<Notice>()
        val recyclerView1 = view.findViewById<RecyclerView>(R.id.rv_searchhistory)
        var searchOption = "title"
        val searchOption2 = "history"




        //검색버튼 클릭시
        mBinding?.button?.setOnClickListener {
            val input = mBinding?.searchWord
            val input_tostring = input?.text.toString()
            val input_ToHistory = Searchhistory(input_tostring)
            when {
                (input?.length()!! <= 1 || input.length() >= 11) -> {
                    Toast.makeText(getContext(), "2글자 이상 10글자 이하로 검색어를 입력해주세요", Toast.LENGTH_SHORT).show()
                }

                else -> {
                    if (input_ToHistory in historylist){
//                        Toast.makeText(getContext(), "이미 등록된 검색어입니다", Toast.LENGTH_SHORT).show()
                        //그냥 검색만
                        (mBinding?.rvSearchnotice?.adapter as NoticeAdapter).search2(mBinding?.searchWord?.text.toString(), searchOption)
                        mBinding?.searchWord?.text = null
                        Toast.makeText(getContext(), "검색 완료", Toast.LENGTH_SHORT).show()
                    } else{
                        //searchword에서 문자열을 가져와 hashMap으로 만듦
                            //검색하고
                        val data = hashMapOf("history" to input?.text?.toString(), "timestamp" to FieldValue.serverTimestamp())//검색 옵션에 따라 검색
                        (mBinding?.rvSearchnotice?.adapter as NoticeAdapter).search2(mBinding?.searchWord?.text.toString(), searchOption)
                        //검색어 기록
                        (mBinding?.rvSearchhistory?.adapter as HistoryAdapter).search3(data, searchOption2)
                        mBinding?.searchWord?.text = null
                        Toast.makeText(getContext(), "검색 완료", Toast.LENGTH_SHORT).show()
                    }

                }
            }
        }

        mBinding?.searhDelete?.setOnClickListener{
            val input = mBinding?.searchWord
            input?.getText()?.clear()
        }
        //검색기록의 검색어 클릭시 해당 공지사항 나열.
        mBinding?.rvSearchhistory?.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
//      recyclerView1.setHasFixedSize(true)
        mBinding?.rvSearchhistory?.adapter =historyadapter
        recyclerView.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = NoticeAdapter(view.context, notice_list)
        historyadapter.itemClick = object : HistoryAdapter.ItemClick {
            override fun onClick(view: View, pos: Int) {
                val history: TextView = view.findViewById(R.id.historyword)
                val searchOption = "title"
                (mBinding?.rvSearchnotice?.adapter as NoticeAdapter).move(history.text.toString(), searchOption)
                mBinding?.searchWord?.setText(history.text.toString())
            }
        }

        (mBinding?.rvSearchhistory?.adapter as HistoryAdapter).getDataFromFirestore()

        //검색어 ,클릭삭제 관련부분
        historyadapter.deleteClick = object:HistoryAdapter.ItemClick {
            override fun onClick(v: View, pos: Int) {
                when(v.id){
                        R.id.btn_delete -> itemDelete(mDocuments!!.get(pos))
                }
            }
       }
        (mBinding?.rvSearchhistory?.adapter as HistoryAdapter).getDataFromFirestore()
    }
    //검색기록 추가
    fun HistoryAdapter.search3(word: HashMap<String, Any?>, option: String) {
        db.collection("History")
            .add(word)
            .addOnSuccessListener {
                // 성공할 경우
                db.collection("History")//작업할 컬렉션
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get() // 문서 가져오기
                    .addOnSuccessListener { result ->
                        historyList.clear()
                        //성공 경우
                        for (document in result) {
                            val item2 = Searchhistory(document[option] as String)
                            historyList.add(item2)
                        }
                        notifyDataSetChanged()//리사이클러뷰 갱신
                    }
                    .addOnFailureListener { exception ->
                        //실패 경우
                        Log.w("MainActivity", "Error getting documents: $exception")
                    }
            }
            .addOnFailureListener { exception ->
                // 실패할 경우
                Log.w("MainActivity", "Error getting documents: $exception")
            }
    }

    //파이어베이스에서 모든검색 결과 가져오기
    fun NoticeAdapter.search2(searchWord: String, option: String) {
        db.collection("total")   // 작업할 컬렉션
            .orderBy("date",Query.Direction.DESCENDING)
            .get()      // 문서 가져오기
            .addOnSuccessListener { result ->
                // 성공할 경우
                noticeList.clear()
                for (document in result) {  // 가져온 문서들은 result에 들어감
                    if (document.getString(option)!!.contains(searchWord)) {
                        val item = Notice(document["title"] as String, document["date"] as String, document["visited"] as String, document["link"] as String,
                        document["text"] as String)
                        noticeList.add(item)
                    }
                }
                notifyDataSetChanged()  // 리사이클러 뷰 갱신
            }
            .addOnFailureListener { exception ->
                // 실패할 경우
                Log.w("MainActivity", "Error getting documents: $exception")
            }
    }


    //검색기록의 검색어 클릭통해 공지사항보기 함수
    fun NoticeAdapter.move(word: String, option: String) {
        db.collection("total")   // 작업할 컬렉션2
            .orderBy("date",Query.Direction.DESCENDING)
            .get()      // 문서 가져오기
            .addOnSuccessListener { result ->
                // 성공할 경우
                noticeList.clear()
                for (document in result) {  // 가져온 문서들은 result에 들어감
                    if (document.getString(option)!!.contains(word)) {
                        val item = Notice(document["title"] as String, document["date"] as String, document["visited"] as String,
                            document["link"] as String, document["text"] as String)
                        noticeList.add(item)
                    }
                }
                notifyDataSetChanged()  // 리사이클러 뷰 갱신
            }
            .addOnFailureListener { exception ->
                // 실패할 경우
                Log.w("MainActivity", "Error getting documents: $exception")
            }
    }
//이전 검색어 삭제 관련부분:https://stackoverflow.com/questions/64370610/android-kotlin-how-can-i-delete-the-data-from-firebase
    fun HistoryAdapter.getDataFromFirestore() {
        db.collection("History")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener{ snapshot, exception ->
                if (exception != null) {
                }
                else {
                    if (snapshot != null) {
                        if (!snapshot.isEmpty) {
                            historyList.clear()
                            mDocuments = snapshot.documents
                            val documents = snapshot.documents
                            for (document in documents) {
                                val item = Searchhistory(document["history"] as String)
                                historyList.add(item)
                            }
                                notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
    fun itemDelete(doc:DocumentSnapshot){
        db.collection("History").document(doc.id)
            .delete()
    }

    }




















