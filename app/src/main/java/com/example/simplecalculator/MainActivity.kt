package com.example.simplecalculator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.room.Room
import com.example.simplecalculator.model.History
import java.lang.NumberFormatException
import kotlin.math.exp

class MainActivity : AppCompatActivity() {

    private val expressionTextView: TextView by lazy {
        findViewById(R.id.expressionTextView)
    }

    private val resultTextView: TextView by lazy {
        findViewById(R.id.resultTextView)
    }

    private val historyLayout: View by lazy {
        findViewById(R.id.historyLayout)
    }

    private val historyLinearLayout: LinearLayout by lazy {
        findViewById(R.id.historyLinearLayout)
    }

    private lateinit var db: AppDatabase


    private var isOperator = false
    private var hasOperator = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "historyDB"
        ).build()
    }

    fun buttonClicked(view: View) {
        when (view.id) {
            R.id.button0 -> numberButtonClicked("0")
            R.id.button1 -> numberButtonClicked("1")
            R.id.button2 -> numberButtonClicked("2")
            R.id.button3 -> numberButtonClicked("3")
            R.id.button4 -> numberButtonClicked("4")
            R.id.button5 -> numberButtonClicked("5")
            R.id.button6 -> numberButtonClicked("6")
            R.id.button7 -> numberButtonClicked("7")
            R.id.button8 -> numberButtonClicked("8")
            R.id.button9 -> numberButtonClicked("9")
            R.id.buttonPlus -> operatorButtonClicked("+")
            R.id.buttonMinus -> operatorButtonClicked("-")
            R.id.buttonMultiply -> operatorButtonClicked("x")
            R.id.buttonDivider -> operatorButtonClicked("/")
            R.id.buttonModular -> operatorButtonClicked("%")
        }
    }

    private fun numberButtonClicked(number: String) {

        if (isOperator) {
            expressionTextView.append(" ")
        }

        isOperator = false

        var expressionText = expressionTextView.text.split(" ")
        if (expressionText.isNotEmpty() && expressionText.last().length >= 15) {
            Toast.makeText(this, "15자리까지만 입력할 수 있습니다", Toast.LENGTH_SHORT).show()
            return
        } else if (number == "0" && expressionText.last().isEmpty()) {
            Toast.makeText(this, "0은 제일 앞에 올 수 없습니다", Toast.LENGTH_SHORT).show()
            return
        }

        expressionTextView.append(number)

        //TODO resultView 실시간 계산 결과
        resultTextView.text = calculateExpression()
    }

    private fun operatorButtonClicked(operator: String) {
        if (expressionTextView.text.isEmpty()) {
            return
        }

        when {
            isOperator -> {
                val text = expressionTextView.text.toString()
                expressionTextView.text = text.dropLast(1) + operator
            }
            hasOperator -> {
                Toast.makeText(this, "연산자는 한번만 사용할 수 있습니다", Toast.LENGTH_SHORT).show()
                return
            }
            else -> {
                expressionTextView.append(" ${operator}")
            }
        }

        val ssb = SpannableStringBuilder(expressionTextView.text)
        ssb.setSpan(
            ForegroundColorSpan(getColor(R.color.green)),
            expressionTextView.text.length - 1,
            expressionTextView.text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        expressionTextView.text = ssb

        isOperator = true
        hasOperator = true
    }

    private fun calculateExpression(): String {
        val expressionText = expressionTextView.text.split(" ")

        if (hasOperator.not() || expressionText.size != 3) {
            return ""
        } else if (expressionText[0].isNumber().not() || expressionText[2].isNumber().not()) {
            return ""
        }

        val exp1 = expressionText[0].toBigInteger()
        val exp2 = expressionText[2].toBigInteger()
        val op = expressionText[1]

        return when (op) {
            "+" -> (exp1 + exp2).toString()
            "-" -> (exp1 - exp2).toString()
            "x" -> (exp1 * exp2).toString()
            "/" -> (exp1 / exp2).toString()
            "%" -> (exp1 % exp2).toString()
            else -> ""
        }
    }

    fun clearButtonClicked(view: View) {
        expressionTextView.text = ""
        resultTextView.text = ""
        isOperator = false
        hasOperator = false
    }

    fun resultButtonClicked(view: View) {
        val expressionText = expressionTextView.text.split(" ")

        if (expressionTextView.text.isEmpty() || expressionText.size == 1){
            return
        }

        if (expressionText.size != 3 && hasOperator){
            Toast.makeText(this, "아직 완성되지 않은 수식입니다", Toast.LENGTH_SHORT).show()
            return
        }

        if (expressionText[0].isNumber().not() || expressionText[2].isNumber().not()) {
            Toast.makeText(this, "오류가 발생했습니다", Toast.LENGTH_SHORT).show()
            return
        }

        val expText = expressionTextView.text.toString()
        val resultText = calculateExpression()

        //TODO DB 삽입
        Thread(Runnable {
            db.historyDao().insertHistory(History(null, expText, resultText))
        }).start()

        resultTextView.text = ""
        expressionTextView.text = resultText

        isOperator = false
        hasOperator = false
    }

    fun historyButtonClicked(view: View) {
        historyLayout.isVisible = true
        historyLinearLayout.removeAllViews()

        // TODO 디비에서 모든 기록 가져오기
        // TODO 뷰에서 모든 기록 할당하기

        Thread(Runnable {

            db.historyDao().getAll().reversed().forEach {
                runOnUiThread {
                    val historyView = LayoutInflater.from(this).inflate(R.layout.history_row, null, false)
                    historyView.findViewById<TextView>(R.id.expressionTextView).text = it.expression
                    historyView.findViewById<TextView>(R.id.resultTextView).text = "= ${it.result}"
                    historyLinearLayout.addView(historyView)
                }
            }

        }).start()
    }

    fun closeHistoryButtonClicked(view: View) {
        historyLayout.isVisible = false
    }

    fun clearHistoryButtonClicked(view: View) {
        // TODO 디비에서 모든 기록 삭제
        // TODO 뷰에서 모든 기록 삭제
        historyLinearLayout.removeAllViews()
        Thread(Runnable {
            db.historyDao().deleteAll()
        }).start()
    }
}

fun String.isNumber(): Boolean {
    return try {
        this.toBigInteger()
        true
    } catch (e: NumberFormatException) {
        false
    }
}