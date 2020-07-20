package com.example.gppayment

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.content.Intent
import android.net.ConnectivityManager
import kotlinx.android.synthetic.main.activity_main.*
import java.util.ArrayList


class MainActivity : AppCompatActivity() {

    var TAG = "main"
    val UPI_PAYMENT = 0
    var context : Context = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val send = findViewById<Button>(R.id.send)
        val amount = findViewById<EditText>(R.id.amount)
        val note = "Upi Pay".toString()
        val name = "".toString()
        val upivirtualid =  "dubeympf@okaxis" //upi_id.getText().toString()


        send.setOnClickListener {

            if(amount.getText().toString().length == 0 )
            {
                amount.setError("fill amount")
            }else if(upi_id.getText().toString().length == 0)
            {
                upi_id.setError("fill upi id ")
            }else{

                UpiPay(name, upivirtualid ,note, amount.getText().toString())
            }

        }



    }

    private fun UpiPay(name: String, upiId: String, note: String, amount: String) {
        val uri = Uri.parse("upi://pay").buildUpon()
            .appendQueryParameter("pa", upiId)
            .appendQueryParameter("pn", name)
            //.appendQueryParameter("mc", "")
            //.appendQueryParameter("tid", "02125412")
            //.appendQueryParameter("tr", "25584584")
            .appendQueryParameter("tn", note)
            .appendQueryParameter("am", amount)
            .appendQueryParameter("cu", "INR")
            //.appendQueryParameter("refUrl", "blueapp")
            .build()


        val UpiIntent = Intent(Intent.ACTION_VIEW)
        UpiIntent.data = uri
        // choose UPI application
        val chooser = Intent.createChooser(UpiIntent, "Pay with")
        if (null != chooser.resolveActivity(packageManager)) {
            startActivityForResult(chooser, UPI_PAYMENT)
        } else {
            Toast.makeText(this@MainActivity, "No UPI app found, please install one to continue", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onActivityResult(UpiCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(UpiCode, resultCode, data)
        Log.e("main ", "response $resultCode")
        /*
       E/main: response -1
       E/UPI: onActivityResult: txnId=AXI4a3428ee58654a938811812c72c0df45&responseCode=00&Status=SUCCESS&txnRef=922118921612
       E/UPIPAY: upiPaymentDataOperation: txnId=AXI4a3428ee58654a938811812c72c0df45&responseCode=00&Status=SUCCESS&txnRef=922118921612
       E/UPI: payment successfull: 922118921612
         */
        when (UpiCode) {
            UPI_PAYMENT -> if (Activity.RESULT_OK == resultCode || resultCode == 11) {
                if (data != null) {
                    val txtresposnce = data.getStringExtra("response")
                    Log.e("UPI", "onActivityResult: " + txtresposnce!!)
                    val dataList = ArrayList<String>()
                    dataList.add(txtresposnce)
                    upiPaymentDataOperation(dataList)
                } else {
                    Log.e("UPI", "onActivityResult: " + "Return data is null")
                    val dataList = ArrayList<String>()
                    dataList.add("nothing")
                    upiPaymentDataOperation(dataList)
                }
            } else {
                //when user simply back without payment
                Log.e("UPI", "onActivityResult: " + "Return data is null")
                val dataList = ArrayList<String>()
                dataList.add("nothing")
                upiPaymentDataOperation(dataList)
            }
        }
    }


    private fun upiPaymentDataOperation(data: ArrayList<String>) {
        if (CheckInternetConnection(this@MainActivity)) {
            var str = data[0]
            Log.e("UPI_PAY", "upiPaymentDataOperation: $str")
            var paymentCancel = ""
            if (str == null) str = "discard"
            var status = ""
            var approvalRefNo = ""
            val response = str.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (i in response.indices) {
                val equalStr =
                    response[i].split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (equalStr.size >= 2) {
                    if (equalStr[0].toLowerCase() == "Status".toLowerCase()) {
                        status = equalStr[1].toLowerCase()
                    } else if (equalStr[0].toLowerCase() == "ApprovalRefNo".toLowerCase() || equalStr[0].toLowerCase() == "txnRef".toLowerCase()) {
                        approvalRefNo = equalStr[1]
                    }
                } else {
                    paymentCancel = "Payment cancelled by user."
                }
            }
            if (status == "success") {
                //successful transaction

                val builder = AlertDialog.Builder(context)
                builder.setTitle("Transection Successful")
                builder.setNegativeButton("OK")
                { dialog, which ->

                    dialog.cancel()

                }
                val dialog = builder.create()
                dialog.show()

                Log.e("UPI", "payment successfull: $approvalRefNo")
            } else if ("Payment cancelled by user." == paymentCancel) {
                Toast.makeText(this@MainActivity, "Payment cancelled by user.", Toast.LENGTH_SHORT).show()
                Log.e("UPI", "Cancelled by user: $approvalRefNo")
            } else {
                Toast.makeText(this@MainActivity, "Transaction failed. Please try again", Toast.LENGTH_SHORT).show()
                Log.e("UPI", "failed payment: $approvalRefNo")
            }
        } else {
            Log.e("UPI", "Internet Problem: ")

            //custom dialog for internet issue
            val builder = AlertDialog.Builder(context)
            builder.setMessage("Looks like your internet is slow")
            builder.setNegativeButton("try again")
            { dialog, which ->

                dialog.cancel()

            }
            val dialog = builder.create()
            dialog.show()

        }
    }

    fun CheckInternetConnection(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val netInfo = connectivityManager.activeNetworkInfo
            if (netInfo != null && netInfo.isConnected
                && netInfo.isConnectedOrConnecting
                && netInfo.isAvailable
            ) {
                return true
            }
        }
        return false
    }

}
